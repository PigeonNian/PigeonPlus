package dev.anvilcraft.pigeonplus.client.sound;

import dev.anvilcraft.pigeonplus.client.particle.NozzleStartupParticleUtil;
import dev.anvilcraft.pigeonplus.client.support.NozzleScreenShakeManager;
import dev.anvilcraft.pigeonplus.block.entity.NozzleExhaustBlockEntity;
import dev.anvilcraft.pigeonplus.util.NozzleExhaustUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class NozzleSoundController {
    private static final Map<BlockPos, SoundState> SOUND_STATES = new HashMap<>();
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final long LEVEL_STARTUP_WARM_START_NANOS = 5_000_000_000L;
    private static final float STARTUP_SHAKE_RADIUS = 24.0F;
    private static final float CONTINUOUS_SHAKE_RADIUS = 12.0F;
    public static final int FLAME_DELAY_TICKS = 6;
    public static final int FLAME_GROWTH_TICKS = 12;
    private static ClientLevel observedLevel;
    private static long levelWarmStartUntilNanos;

    private record SoundState(
        List<NozzleSoundInstance> playingSounds,
        long startNanos,
        long nextFireNanos,
        int lastStartupRingAge
    ) {
        private SoundState withNextFireNanos(long nextFireNanos) {
            return new SoundState(this.playingSounds, this.startNanos, nextFireNanos, this.lastStartupRingAge);
        }

        private SoundState withLastStartupRingAge(int lastStartupRingAge) {
            return new SoundState(this.playingSounds, this.startNanos, this.nextFireNanos, lastStartupRingAge);
        }
    }

    private NozzleSoundController() {
    }

    public static void tick(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused() || minecraft.level == null) {
            return;
        }
        long now = System.nanoTime();
        refreshObservedLevel((ClientLevel) minecraft.level, now);
        if (!NozzleExhaustUtil.isNozzleActive(minecraft.level, pos)) {
            cleanup();
            return;
        }
        Direction facing = NozzleExhaustUtil.getStructuralFacing(minecraft.level, pos);
        if (facing == null) {
            return;
        }
        BlockPos outletPos = NozzleExhaustUtil.getStructuralOutletPos(minecraft.level, pos);
        if (outletPos == null) {
            return;
        }
        NozzleScreenShakeManager.getInstance().sustain(Vec3.atCenterOf(outletPos), CONTINUOUS_SHAKE_RADIUS);

        SoundState state = SOUND_STATES.get(pos);
        if (state == null) {
            if (now <= levelWarmStartUntilNanos) {
                SOUND_STATES.put(pos, createWarmState(minecraft, pos, now));
                return;
            }
            NozzleSoundInstance created = new NozzleSoundInstance(pos, true);
            minecraft.getSoundManager().play(created);
            NozzleScreenShakeManager.getInstance().trigger(Vec3.atCenterOf(outletPos), STARTUP_SHAKE_RADIUS);
            NozzleStartupParticleUtil.spawnStartupRing((ClientLevel) minecraft.level, outletPos, facing, 0);
            List<NozzleSoundInstance> sounds = new ArrayList<>();
            sounds.add(created);
            SOUND_STATES.put(pos, new SoundState(
                sounds,
                now,
                now + millisToNanos(NozzleSoundInstance.ENGINE_ON_MILLIS - NozzleSoundInstance.ENGINE_ON_TO_FIRE_LEAD_MILLIS),
                0
            ));
            return;
        }

        state.playingSounds().removeIf(NozzleSoundInstance::isStopped);

        int startupRingAge = elapsedTicks(now, state.startNanos());
        int lastStartupRingAge = state.lastStartupRingAge();
        int targetStartupRingAge = Math.min(startupRingAge, NozzleStartupParticleUtil.STARTUP_RING_TICKS - 1);
        for (int age = lastStartupRingAge + 1; age <= targetStartupRingAge; age++) {
            NozzleStartupParticleUtil.spawnStartupRing((ClientLevel) minecraft.level, outletPos, facing, age);
        }
        state = state.withLastStartupRingAge(Math.max(lastStartupRingAge, targetStartupRingAge));

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

    private static SoundState createWarmState(Minecraft minecraft, BlockPos pos, long now) {
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
            NozzleStartupParticleUtil.STARTUP_RING_TICKS - 1
        );
    }

    private static void refreshObservedLevel(ClientLevel level, long now) {
        if (observedLevel == level) {
            return;
        }
        observedLevel = level;
        levelWarmStartUntilNanos = now + LEVEL_STARTUP_WARM_START_NANOS;
        SOUND_STATES.clear();
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
            observedLevel = null;
            levelWarmStartUntilNanos = 0L;
            return;
        }

        Iterator<Map.Entry<BlockPos, SoundState>> iterator = SOUND_STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, SoundState> entry = iterator.next();
            BlockPos pos = entry.getKey();
            SoundState state = entry.getValue();
            state.playingSounds().removeIf(NozzleSoundInstance::isStopped);

            boolean active = level.getBlockEntity(pos) instanceof NozzleExhaustBlockEntity
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
    }

    public static float getFlameStartupProgress(BlockPos pos) {
        SoundState state = SOUND_STATES.get(pos);
        if (state == null) {
            return 1.0F;
        }
        int age = elapsedTicks(System.nanoTime(), state.startNanos());
        if (age <= FLAME_DELAY_TICKS) {
            return 0.0F;
        }
        float progress = (age - FLAME_DELAY_TICKS) / (float) FLAME_GROWTH_TICKS;
        return Math.min(1.0F, Math.max(0.0F, progress));
    }

    private static int elapsedTicks(long nowNanos, long startNanos) {
        return (int) Math.max(0L, (nowNanos - startNanos) / millisToNanos(NozzleSoundInstance.TICK_MILLIS));
    }

    private static long millisToNanos(long millis) {
        return millis * NANOS_PER_MILLI;
    }
}
