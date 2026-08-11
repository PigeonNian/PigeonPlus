package dev.anvilcraft.pigeonplus.client.sound;

import dev.anvilcraft.pigeonplus.client.AnvilCraftPigeonPlusClient;
import dev.anvilcraft.pigeonplus.client.particle.NozzleStartupParticleUtil;
import dev.anvilcraft.pigeonplus.client.support.NozzleScreenShakeManager;
import dev.anvilcraft.pigeonplus.block.entity.NozzleExhaustBlockEntity;
import dev.anvilcraft.pigeonplus.util.NozzleExhaustUtil;
import dev.dubhe.anvilcraft.init.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NozzleSoundController {
    private static final Map<BlockPos, SoundState> SOUND_STATES = new HashMap<>();
    private static final Map<BlockPos, ShutdownState> SHUTDOWN_STATES = new HashMap<>();
    private static final Set<BlockPos> ACTIVE_HINTS = new HashSet<>();
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final int DISCOVERY_INTERVAL_TICKS = 10;
    private static final int DISCOVERY_RADIUS = 96;

    public static final int FLAME_DELAY_TICKS = 10;
    public static final int FLAME_GROWTH_TICKS = 8;
    public static final int FLAME_SHUTDOWN_TICKS = 12;
    private static final int FIRST_FLAME_SHUTDOWN_BURST_TICKS = 4;
    private static final int SECOND_FLAME_SHUTDOWN_BURST_TICKS = 8;
    private static final int THIRD_FLAME_SHUTDOWN_BURST_TICKS = 12;
    private static final int STARTUP_FLAME_BURST_TICKS = 10;
    private static final int LAST_STARTUP_GAS_TICK = NozzleStartupParticleUtil.STARTUP_RING_TICKS - 1;
    private static final int STARTUP_FLAME_BURST_PARTICLES = 44;
    private static final int SHUTDOWN_BURST_PARTICLES = 28;
    private static final int LARGE_SHUTDOWN_BURST_PARTICLES = 56;
    private static ClientLevel observedLevel;

    private record SoundState(
        List<NozzleSoundInstance> playingSounds,
        long startNanos,
        long nextFireNanos,
        int lastStartupRingAge,
        boolean startupFlameBurstSpawned
    ) {
        private SoundState withNextFireNanos(long nextFireNanos) {
            return new SoundState(
                this.playingSounds,
                this.startNanos,
                nextFireNanos,
                this.lastStartupRingAge,
                this.startupFlameBurstSpawned
            );
        }

        private SoundState withLastStartupRingAge(int lastStartupRingAge) {
            return new SoundState(
                this.playingSounds,
                this.startNanos,
                this.nextFireNanos,
                lastStartupRingAge,
                this.startupFlameBurstSpawned
            );
        }

        private SoundState withStartupFlameBurstSpawned() {
            return new SoundState(
                this.playingSounds,
                this.startNanos,
                this.nextFireNanos,
                this.lastStartupRingAge,
                true
            );
        }
    }

    private record ShutdownState(
        long startNanos,
        float startProgress,
        BlockPos outletPos,
        Direction facing,
        boolean firstBurstSpawned,
        boolean secondBurstSpawned,
        boolean thirdBurstSpawned
    ) {
        private ShutdownState withFirstBurstSpawned() {
            return new ShutdownState(
                this.startNanos,
                this.startProgress,
                this.outletPos,
                this.facing,
                true,
                this.secondBurstSpawned,
                this.thirdBurstSpawned
            );
        }

        private ShutdownState withSecondBurstSpawned() {
            return new ShutdownState(
                this.startNanos,
                this.startProgress,
                this.outletPos,
                this.facing,
                this.firstBurstSpawned,
                true,
                this.thirdBurstSpawned
            );
        }

        private ShutdownState withThirdBurstSpawned() {
            return new ShutdownState(
                this.startNanos,
                this.startProgress,
                this.outletPos,
                this.facing,
                this.firstBurstSpawned,
                this.secondBurstSpawned,
                true
            );
        }
    }

    private NozzleSoundController() {
    }

    private static float shakeRange() {
        return AnvilCraftPigeonPlusClient.CLIENT_CONFIG.nozzleShakeRange;
    }

    public static void registerActive(BlockPos pos) {
        ACTIVE_HINTS.add(pos.immutable());
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            cleanup();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        refreshObservedLevel(level);

        Set<BlockPos> activePositions = new HashSet<>(ACTIVE_HINTS);
        ACTIVE_HINTS.clear();
        if (minecraft.player != null && level.getGameTime() % DISCOVERY_INTERVAL_TICKS == 0) {
            discoverLoadedExhausts(level, minecraft.player.blockPosition(), activePositions);
        }

        for (BlockPos pos : activePositions) {
            tick(pos);
        }
        for (BlockPos pos : new ArrayList<>(SOUND_STATES.keySet())) {
            if (!activePositions.contains(pos)) {
                tick(pos);
            }
        }
        cleanup();
    }

    public static void tick(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused() || minecraft.level == null) {
            return;
        }
        long now = System.nanoTime();
        refreshObservedLevel((ClientLevel) minecraft.level);
        if (!(minecraft.level.getBlockEntity(pos) instanceof NozzleExhaustBlockEntity blockEntity)
            || !NozzleExhaustUtil.isNozzleActive(minecraft.level, pos)
            || blockEntity.getExhaustPhase() == NozzleExhaustBlockEntity.ExhaustPhase.IDLE) {
            beginShutdown(pos);
            return;
        }
        SHUTDOWN_STATES.remove(pos);
        Direction facing = NozzleExhaustUtil.getStructuralFacing(minecraft.level, pos);
        if (facing == null) {
            return;
        }
        BlockPos outletPos = NozzleExhaustUtil.getStructuralOutletPos(minecraft.level, pos);
        if (outletPos == null) {
            return;
        }
        NozzleScreenShakeManager.getInstance().sustain(Vec3.atCenterOf(outletPos), shakeRange());

        SoundState state = SOUND_STATES.get(pos);
        if (state == null) {
            if (blockEntity.isExhaustFiring()) {
                SOUND_STATES.put(pos, createFiringState(minecraft, pos, now));
                return;
            }
            NozzleSoundInstance created = new NozzleSoundInstance(pos, true);
            minecraft.getSoundManager().play(created);
            spawnStartupGasPulses((ClientLevel) minecraft.level, outletPos, facing, 0);
            List<NozzleSoundInstance> sounds = new ArrayList<>();
            sounds.add(created);
            SOUND_STATES.put(pos, new SoundState(
                sounds,
                now,
                now + millisToNanos(NozzleSoundInstance.ENGINE_ON_MILLIS - NozzleSoundInstance.ENGINE_ON_TO_FIRE_LEAD_MILLIS),
                0,
                false
            ));
            return;
        }

        state.playingSounds().removeIf(NozzleSoundInstance::isStopped);

        int startupRingAge = elapsedTicks(now, state.startNanos());
        int lastStartupRingAge = state.lastStartupRingAge();
        int targetStartupRingAge = Math.min(startupRingAge, LAST_STARTUP_GAS_TICK);
        for (int age = lastStartupRingAge + 1; age <= targetStartupRingAge; age++) {
            spawnStartupGasPulses((ClientLevel) minecraft.level, outletPos, facing, age);
        }
        state = state.withLastStartupRingAge(Math.max(lastStartupRingAge, targetStartupRingAge));
        state = tickStartupFlameBurst(state, outletPos, facing, now);

        if (now >= state.nextFireNanos()) {
            NozzleSoundInstance fireSound = new NozzleSoundInstance(pos, false);
            minecraft.getSoundManager().play(fireSound);
            state.playingSounds().add(fireSound);
            long nextFireNanos = state.nextFireNanos();
            long fireIntervalNanos = millisToNanos(
                NozzleSoundInstance.ENGINE_FIRE_MILLIS - NozzleSoundInstance.ENGINE_FIRE_TO_FIRE_LEAD_MILLIS
            );
            do {
                nextFireNanos += fireIntervalNanos;
            } while (nextFireNanos <= now);
            state = state.withNextFireNanos(nextFireNanos);
        }

        SOUND_STATES.put(pos, state);
    }

    private static void discoverLoadedExhausts(ClientLevel level, BlockPos center, Set<BlockPos> activePositions) {
        int chunkRadius = (DISCOVERY_RADIUS + 15) >> 4;
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        double maxDistanceSqr = DISCOVERY_RADIUS * DISCOVERY_RADIUS;
        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                if (!(level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) instanceof LevelChunk chunk)) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof NozzleExhaustBlockEntity nozzleExhaust)) {
                        continue;
                    }
                    BlockPos pos = nozzleExhaust.getBlockPos();
                    if (pos.distSqr(center) > maxDistanceSqr
                        || nozzleExhaust.getExhaustPhase() == NozzleExhaustBlockEntity.ExhaustPhase.IDLE
                        || !NozzleExhaustUtil.isNozzleActive(level, pos)) {
                        continue;
                    }
                    activePositions.add(pos.immutable());
                }
            }
        }
    }

    private static SoundState createFiringState(Minecraft minecraft, BlockPos pos, long now) {
        NozzleSoundInstance fireSound = new NozzleSoundInstance(pos, false);
        minecraft.getSoundManager().play(fireSound);
        List<NozzleSoundInstance> sounds = new ArrayList<>();
        sounds.add(fireSound);
        long fireIntervalNanos = millisToNanos(
            NozzleSoundInstance.ENGINE_FIRE_MILLIS - NozzleSoundInstance.ENGINE_FIRE_TO_FIRE_LEAD_MILLIS
        );
        return new SoundState(
            sounds,
            now - millisToNanos((FLAME_DELAY_TICKS + FLAME_GROWTH_TICKS) * NozzleSoundInstance.TICK_MILLIS),
            now + fireIntervalNanos,
            LAST_STARTUP_GAS_TICK,
            true
        );
    }

    private static void refreshObservedLevel(ClientLevel level) {
        if (observedLevel == level) {
            return;
        }
        observedLevel = level;
        SOUND_STATES.clear();
        SHUTDOWN_STATES.clear();
    }

    public static void cleanup() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            for (SoundState state : SOUND_STATES.values()) {
                for (NozzleSoundInstance sound : state.playingSounds()) {
                    if (!sound.isStopped()) {
                        sound.forceStop();
                    }
                }
            }
            SOUND_STATES.clear();
            SHUTDOWN_STATES.clear();
            observedLevel = null;
            return;
        }

        Iterator<Map.Entry<BlockPos, SoundState>> iterator = SOUND_STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, SoundState> entry = iterator.next();
            BlockPos pos = entry.getKey();
            SoundState state = entry.getValue();
            state.playingSounds().removeIf(NozzleSoundInstance::isStopped);

            boolean active = level.isLoaded(pos)
                && level.getBlockEntity(pos) instanceof NozzleExhaustBlockEntity
                && NozzleExhaustUtil.isNozzleActive(level, pos);
            if (active) {
                continue;
            }

            for (NozzleSoundInstance sound : state.playingSounds()) {
                if (!sound.isStopped()) {
                    sound.forceStop();
                }
            }
            iterator.remove();
        }
        Iterator<Map.Entry<BlockPos, ShutdownState>> shutdownIterator = SHUTDOWN_STATES.entrySet().iterator();
        while (shutdownIterator.hasNext()) {
            Map.Entry<BlockPos, ShutdownState> entry = shutdownIterator.next();
            ShutdownState shutdownState = tickShutdownBurst(entry.getValue(), System.nanoTime());
            if (getShutdownProgress(shutdownState, System.nanoTime()) <= 0.0F) {
                shutdownIterator.remove();
            } else if (shutdownState != entry.getValue()) {
                entry.setValue(shutdownState);
            }
        }
    }

    public static void stop(BlockPos pos) {
        SoundState state = SOUND_STATES.remove(pos);
        if (state == null) {
            SHUTDOWN_STATES.remove(pos);
            return;
        }
        for (NozzleSoundInstance sound : state.playingSounds()) {
            if (!sound.isStopped()) {
                sound.forceStop();
            }
        }
        SHUTDOWN_STATES.remove(pos);
    }

    public static void beginShutdown(BlockPos pos) {
        if (SHUTDOWN_STATES.containsKey(pos)) {
            return;
        }
        SoundState state = SOUND_STATES.remove(pos);
        if (state == null) {
            return;
        }
        for (NozzleSoundInstance sound : state.playingSounds()) {
            if (!sound.isStopped()) {
                sound.forceStop();
            }
        }
        float progress = calculateStartupProgress(System.nanoTime(), state.startNanos());
        if (progress > 0.0F) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }
            Direction facing = NozzleExhaustUtil.getStructuralFacing(minecraft.level, pos);
            BlockPos outletPos = NozzleExhaustUtil.getStructuralOutletPos(minecraft.level, pos);
            if (facing != null && outletPos != null) {
                SHUTDOWN_STATES.put(pos, new ShutdownState(
                    System.nanoTime(),
                    progress,
                    outletPos,
                    facing,
                    false,
                    false,
                    false
                ));
            }
        }
    }

    public static float getFlameProgress(BlockPos pos) {
        long now = System.nanoTime();
        SoundState state = SOUND_STATES.get(pos);
        if (state != null) {
            return calculateStartupProgress(now, state.startNanos());
        }
        ShutdownState shutdownState = SHUTDOWN_STATES.get(pos);
        if (shutdownState == null) {
            return 0.0F;
        }
        shutdownState = tickShutdownBurst(shutdownState, now);
        SHUTDOWN_STATES.put(pos, shutdownState);
        float progress = getShutdownProgress(shutdownState, now);
        if (progress <= 0.0F) {
            SHUTDOWN_STATES.remove(pos);
            return 0.0F;
        }
        return progress;
    }

    private static float calculateStartupProgress(long nowNanos, long startNanos) {
        int age = elapsedTicks(nowNanos, startNanos);
        if (age <= FLAME_DELAY_TICKS) {
            return 0.0F;
        }
        float progress = (age - FLAME_DELAY_TICKS) / (float) FLAME_GROWTH_TICKS;
        return Math.min(1.0F, Math.max(0.0F, progress));
    }

    private static float getShutdownProgress(ShutdownState state, long nowNanos) {
        int age = elapsedTicks(nowNanos, state.startNanos());
        float progress = 1.0F - age / (float) FLAME_SHUTDOWN_TICKS;
        return state.startProgress() * Math.min(1.0F, Math.max(0.0F, progress));
    }

    private static ShutdownState tickShutdownBurst(ShutdownState state, long nowNanos) {
        int age = elapsedTicks(nowNanos, state.startNanos());
        ShutdownState current = state;
        if (!current.firstBurstSpawned() && age >= FIRST_FLAME_SHUTDOWN_BURST_TICKS) {
            spawnNozzleBurst(current.outletPos(), current.facing(), false);
            current = current.withFirstBurstSpawned();
        }
        if (!current.secondBurstSpawned() && age >= SECOND_FLAME_SHUTDOWN_BURST_TICKS) {
            spawnNozzleBurst(current.outletPos(), current.facing(), false);
            current = current.withSecondBurstSpawned();
        }
        if (!current.thirdBurstSpawned() && age >= THIRD_FLAME_SHUTDOWN_BURST_TICKS) {
            spawnNozzleBurst(current.outletPos(), current.facing(), true);
            current = current.withThirdBurstSpawned();
        }
        return current;
    }

    private static SoundState tickStartupFlameBurst(SoundState state, BlockPos outletPos, Direction facing, long nowNanos) {
        if (state.startupFlameBurstSpawned()) {
            return state;
        }
        if (elapsedTicks(nowNanos, state.startNanos()) < STARTUP_FLAME_BURST_TICKS) {
            return state;
        }
        spawnNozzleBurst(outletPos, facing, STARTUP_FLAME_BURST_PARTICLES, 1.45, 1.45, 1.15);
        return state.withStartupFlameBurstSpawned();
    }

    private static void spawnNozzleBurst(BlockPos outletPos, Direction facing, boolean large) {
        spawnNozzleBurst(
            outletPos,
            facing,
            large ? LARGE_SHUTDOWN_BURST_PARTICLES : SHUTDOWN_BURST_PARTICLES,
            large ? 1.0 : 1.0,
            large ? 1.0 : 1.0,
            large ? 1.0 : 1.0
        );
    }

    private static void spawnNozzleBurst(
        BlockPos outletPos,
        Direction facing,
        int count,
        double radiusScale,
        double spreadScale,
        double speedScale
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.level instanceof ClientLevel level)) {
            return;
        }
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            boolean large = count >= LARGE_SHUTDOWN_BURST_PARTICLES;
            double radius = random.nextDouble() * (large ? 1.05 : 0.55) * radiusScale;
            double sideX = 0.5 + Math.cos(angle) * radius;
            double sideZ = 0.5 + Math.sin(angle) * radius;
            double axis = -0.75 + random.nextDouble() * (large ? 0.50 : 0.30);
            double axialSpeed = ((large ? 1.30 : 1.10) + random.nextDouble() * (large ? 0.75 : 0.55)) * speedScale;
            double spread = ((large ? 0.16 : 0.08) + random.nextDouble() * (large ? 0.22 : 0.10)) * spreadScale;
            Vec3 pos = point(outletPos, facing, sideX, axis, sideZ);
            Vec3 velocity = vector(
                facing,
                Math.cos(angle) * spread + (random.nextDouble() - 0.5) * 0.035,
                axialSpeed,
                Math.sin(angle) * spread + (random.nextDouble() - 0.5) * 0.035
            );
            level.addParticle(
                ModParticles.PLASMA_JETS.get(),
                true,
                pos.x,
                pos.y,
                pos.z,
                velocity.x,
                velocity.y,
                velocity.z
            );
        }
    }

    private static void spawnStartupGasPulses(ClientLevel level, BlockPos outletPos, Direction facing, int age) {
        if (age >= 0 && age < NozzleStartupParticleUtil.STARTUP_RING_TICKS) {
            NozzleStartupParticleUtil.spawnStartupRing(level, outletPos, facing, age, true);
        }
    }

    private static Vec3 point(BlockPos jetPos, Direction facing, double sideX, double axis, double sideZ) {
        Vec3 local = vector(facing, sideX, axis, sideZ);
        if (facing == Direction.DOWN || facing == Direction.WEST || facing == Direction.NORTH) {
            local = local.add(
                facing == Direction.WEST ? 1.0 : 0.0,
                facing == Direction.DOWN ? 1.0 : 0.0,
                facing == Direction.NORTH ? 1.0 : 0.0
            );
        }
        return new Vec3(jetPos.getX() + local.x, jetPos.getY() + local.y, jetPos.getZ() + local.z);
    }

    private static Vec3 vector(Direction facing, double sideX, double axis, double sideZ) {
        return switch (facing) {
            case DOWN -> new Vec3(sideX, -axis, sideZ);
            case EAST -> new Vec3(axis, sideX, sideZ);
            case WEST -> new Vec3(-axis, sideX, sideZ);
            case SOUTH -> new Vec3(sideX, sideZ, axis);
            case NORTH -> new Vec3(sideX, sideZ, -axis);
            default -> new Vec3(sideX, axis, sideZ);
        };
    }

    private static int elapsedTicks(long nowNanos, long startNanos) {
        return (int) Math.max(0L, (nowNanos - startNanos) / millisToNanos(NozzleSoundInstance.TICK_MILLIS));
    }

    private static long millisToNanos(long millis) {
        return millis * NANOS_PER_MILLI;
    }
}
