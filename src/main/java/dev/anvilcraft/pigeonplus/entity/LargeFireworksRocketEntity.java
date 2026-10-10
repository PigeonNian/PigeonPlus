package dev.anvilcraft.pigeonplus.entity;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.client.render.LargeFireworksRocketBurst;
import dev.anvilcraft.pigeonplus.init.AddonDamageTypes;
import dev.anvilcraft.pigeonplus.init.AddonEntities;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LargeFireworksRocketEntity extends Entity {

    private static final double LAUNCH_SPEED = 0.10;

    private static final double THRUST_BASE = 0.002;

    private static final double THRUST_GAIN = 0.0007;

    private static final double DRAG = 0.985;

    private static final double MAX_SPEED = 3.5;

    public static final int DEFAULT_FUSE = 93;

    private static final int MOON_FUSE = 300;

    private static final double MOON_SUCCESS_CHANCE = 0.5;

    private static final float BLAST_RADIUS = 12.0F;

    private static final double BODY_RADIUS = 1.2;

    private static final double CORE_LENGTH = 1.6;

    private static final double BODY_HEIGHT = 9.0;

    public static final double PEARL_RENDER_HEIGHT = BODY_HEIGHT;

    public static final float PEARL_RENDER_SCALE = 2.0F;

    public static final int MAX_PEARLS = 4;

    private static final ResourceKey<Level> MUN_LEVEL =
        ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("anvilcraft", "mun"));

    private static final double ESCAPE_HEIGHT = 512.0;

    private static final long MIDNIGHT = 18000L;
    private static final long MIDNIGHT_WINDOW = 1000L;

    private static final int ARRIVAL_MIN_DISTANCE = 8;
    private static final int ARRIVAL_MAX_DISTANCE = 16;

    private int life;

    public int getLife() {
        return this.life;
    }

    private final InterpolationHandler interpolation = new InterpolationHandler(this);

    @Nullable
    private Boolean moonTravelSucceeds;

    private static final EntityDataAccessor<Integer> DATA_FUSE =
        SynchedEntityData.defineId(LargeFireworksRocketEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<ItemStack> DATA_FIREWORKS_ITEM =
        SynchedEntityData.defineId(LargeFireworksRocketEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final int BURST_INTERVAL = 2;

    private static final int BURST_LINGER = 20;

    private static final EntityDataAccessor<Boolean> DATA_DETONATING =
        SynchedEntityData.defineId(LargeFireworksRocketEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_PEARLS =
        SynchedEntityData.defineId(LargeFireworksRocketEntity.class, EntityDataSerializers.INT);

    private List<UUID> pearlOwners = List.of();

    public List<UUID> getPearlOwners() {
        return this.pearlOwners;
    }

    private static final double SPREAD_BASE = 5.0;

    private static final double SPREAD_PER_STAR = 1.5;

    private static final double SPREAD_MAX = 27.0;

    private static final EntityDataAccessor<Direction> DATA_FACING =
        SynchedEntityData.defineId(LargeFireworksRocketEntity.class, EntityDataSerializers.DIRECTION);

    @Nullable
    private Entity owner;

    private int burstTicks;

    private int clientBurstIndex;

    private int clientBurstTicks = -1;

    private boolean trailStarted;

    public LargeFireworksRocketEntity(EntityType<? extends LargeFireworksRocketEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;
    }

    public LargeFireworksRocketEntity(Level level, double x, double y, double z, int fuse) {
        this(AddonEntities.LARGE_FIREWORKS_ROCKET.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setFuse(fuse);
    }

    public void setOwner(@Nullable Entity owner) {
        this.owner = owner;
    }

    public void setExplosions(List<FireworkExplosion> explosions) {
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        if (!explosions.isEmpty()) {
            stack.set(DataComponents.FIREWORKS, new Fireworks(0, List.copyOf(explosions)));
        }
        this.entityData.set(DATA_FIREWORKS_ITEM, stack);
    }

    public List<FireworkExplosion> getExplosions() {
        Fireworks fireworks = this.entityData.get(DATA_FIREWORKS_ITEM).get(DataComponents.FIREWORKS);
        return fireworks != null ? fireworks.explosions() : List.of();
    }

    public static LargeFireworksRocketEntity launch(
        ServerLevel level,
        BlockPos mainPos,
        Direction facing,
        @Nullable Entity owner,
        List<FireworkExplosion> explosions,
        List<UUID> pearlOwners
    ) {
        LargeFireworksRocketEntity firework = new LargeFireworksRocketEntity(
            level,
            mainPos.getX() + 0.5,
            mainPos.getY(),
            mainPos.getZ() + 0.5,
            DEFAULT_FUSE
        );
        firework.setOwner(owner);
        firework.setFacing(facing);
        firework.setExplosions(explosions);
        firework.setPearlOwners(pearlOwners);
        firework.setDeltaMovement(0.0, LAUNCH_SPEED, 0.0);
        level.addFreshEntity(firework);
        level.playSound(
            null, mainPos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL,
            3.0F, 1.0F
        );
        return firework;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FUSE, DEFAULT_FUSE);
        builder.define(DATA_FIREWORKS_ITEM, new ItemStack(Items.FIREWORK_ROCKET));
        builder.define(DATA_DETONATING, false);
        builder.define(DATA_FACING, Direction.NORTH);
        builder.define(DATA_PEARLS, 0);
    }

    public int getPearls() {
        return this.entityData.get(DATA_PEARLS);
    }

    public void setPearlOwners(List<UUID> owners) {
        this.pearlOwners = List.copyOf(owners.subList(0, Math.min(owners.size(), MAX_PEARLS)));
        this.entityData.set(DATA_PEARLS, this.pearlOwners.size());
    }

    public Direction getFacing() {
        return this.entityData.get(DATA_FACING);
    }

    public void setFacing(Direction facing) {
        this.entityData.set(DATA_FACING, facing.getAxis().isHorizontal() ? facing : Direction.NORTH);
    }

    public boolean isDetonating() {
        return this.entityData.get(DATA_DETONATING);
    }

    private void setDetonating(boolean detonating) {
        this.entityData.set(DATA_DETONATING, detonating);
    }

    private void setLife(int life) {
        this.life = life;
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE);
    }

    private void setFuse(int fuse) {
        this.entityData.set(DATA_FUSE, fuse);
    }

    private void lerpClientStep() {
        if (this.getInterpolation() != null) {
            this.getInterpolation().interpolate();
        }
    }

    @Override
    public @Nullable InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide() && this.isDetonating()) {
            this.tickClientBurst();
            return;
        }

        if (this.level().isClientSide()) {
            this.lerpClientStep();

            this.setLife(this.getLife() + 1);
            this.spawnClientTrail();
            return;
        }

        if (this.isDetonating()) {
            this.tickBurst();
            return;
        }

        int life = this.getLife() + 1;
        this.setLife(life);

        Vec3 motion = this.getDeltaMovement();
        double thrust = THRUST_BASE + THRUST_GAIN * life;
        motion = motion.add(0.0, thrust, 0.0).scale(DRAG);
        if (motion.y > MAX_SPEED) {
            motion = new Vec3(motion.x, MAX_SPEED, motion.z);
        }
        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, motion);

        if (this.tryMoonTravel(life)) {
            return;
        }

        if (this.horizontalCollision || this.verticalCollision) {
            this.detonate();
            return;
        }

        boolean needsAltitude =
            !this.getPassengers().isEmpty() || !this.pearlOwners.isEmpty();
        int fuse = needsAltitude ? MOON_FUSE : this.getFuse();
        if (life >= fuse) {
            this.detonate();
        }
    }

    private boolean tryMoonTravel(int life) {

        if (life % 5 != 0) {
            return false;
        }
        if (this.getY() <= ESCAPE_HEIGHT) {
            return false;
        }
        if (!(this.level() instanceof ServerLevel overworld)
            || !overworld.dimension().equals(Level.OVERWORLD)) {
            return false;
        }
        long time = Math.abs(Math.floorMod(overworld.getOverworldClockTime(), 24000L) - MIDNIGHT);
        if (time > MIDNIGHT_WINDOW) {
            return false;
        }
        MinecraftServer server = overworld.getServer();
        ServerLevel mun = server.getLevel(MUN_LEVEL);
        if (mun == null) {
            return false;
        }

        if (!this.pearlOwners.isEmpty()) {
            List<ServerPlayer> already = new ArrayList<>();
            for (UUID owner : this.pearlOwners) {
                ServerPlayer player = server.getPlayerList().getPlayer(owner);
                if (player != null) {
                    this.sendToMoon(player, mun);
                    already.add(player);
                }
            }

            for (Entity rider : List.copyOf(this.getPassengers())) {
                if (rider instanceof ServerPlayer player && !already.contains(player)) {
                    this.sendToMoon(player, mun);
                }
            }

            this.discard();
            return true;
        }

        if (this.getPassengers().isEmpty()
            || !(this.getPassengers().getFirst() instanceof ServerPlayer passenger)) {
            return false;
        }

        if (this.moonTravelSucceeds == null) {
            this.moonTravelSucceeds = this.random.nextDouble() < MOON_SUCCESS_CHANCE;
        }
        if (!this.moonTravelSucceeds) {

            return false;
        }
        this.sendToMoon(passenger, mun);
        this.discard();
        return true;
    }

    private void sendToMoon(ServerPlayer player, ServerLevel mun) {
        player.stopRiding();
        RandomSource random = mun.getRandom();
        double angle = random.nextDouble() * (Math.PI * 2.0);
        double distance = ARRIVAL_MIN_DISTANCE
            + random.nextDouble() * (ARRIVAL_MAX_DISTANCE - ARRIVAL_MIN_DISTANCE);
        BlockPos spawn = mun.getRespawnData().pos();
        BlockPos landing = CelestialTravelManager.findSafeSurfaceLandingPos(
            mun,
            new BlockPos(
                spawn.getX() + Mth.floor(Math.cos(angle) * distance),
                0,
                spawn.getZ() + Mth.floor(Math.sin(angle) * distance)
            )
        );
        if (landing == null) {
            return;
        }
        player.teleport(new TeleportTransition(
            mun,
            Vec3.atBottomCenterOf(landing),
            Vec3.ZERO,
            player.getYRot(),
            player.getXRot(),
            TeleportTransition.DO_NOTHING
        ));
    }

    private void detonate() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.setDetonating(true);
        this.setDeltaMovement(Vec3.ZERO);

        double x = this.getX();
        double y = this.blastY();
        double z = this.getZ();

        this.gameEvent(GameEvent.EXPLODE, this.owner);

        this.killRiders(serverLevel);

        AABB area = new AABB(x, y, z, x, y, z).inflate(BLAST_RADIUS);
        DamageSource source = this.damageSources().explosion(this, this.owner);
        for (Entity target : serverLevel.getEntities(this, area)) {
            if (isRider(target)) {

                continue;
            }
            double distance = Math.sqrt(target.distanceToSqr(x, y, z));
            if (distance > BLAST_RADIUS) {
                continue;
            }
            float damage = (float) (BLAST_RADIUS - distance) * 1.5F;
            if (damage <= 0.0F) {
                continue;
            }
            target.hurt(source, damage);
            double push = (BLAST_RADIUS - distance) / BLAST_RADIUS * 2.0;
            Vec3 pushDir = target.position().subtract(x, y, z);
            if (pushDir.lengthSqr() > 1.0E-4) {
                pushDir = pushDir.normalize().scale(push);
                target.push(pushDir.x, pushDir.y * 0.5 + 0.3, pushDir.z);
            }
        }

        serverLevel.playSound(
            null, x, y, z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.NEUTRAL,
            6.0F, 1.0F
        );
        this.burstTicks = 0;
    }

    private void tickBurst() {
        if (++this.burstTicks > burstDuration(this.getExplosions().size())) {
            this.discard();
        }
    }

    private boolean isRider(Entity entity) {
        return entity.getRootVehicle() == this;
    }

    private void killRiders(ServerLevel level) {

        List<Entity> riders = new ArrayList<>();
        this.getIndirectPassengers().forEach(riders::add);
        if (riders.isEmpty()) {
            return;
        }
        DamageSource source = AddonDamageTypes.escapeTowerFailure(level, this);
        for (Entity rider : riders) {
            if (rider instanceof LivingEntity) {
                rider.hurt(source, Float.MAX_VALUE);
            } else {
                rider.kill(level);
            }
        }
    }

    private static int burstDuration(int starCount) {
        return Math.max(1, starCount) * BURST_INTERVAL + BURST_LINGER;
    }

    private void tickClientBurst() {

        this.trailStarted = false;
        if (this.clientBurstTicks < 0) {
            this.clientBurstTicks = 0;
            this.clientBurstIndex = 0;
        }
        int total = Math.max(1, this.getExplosions().size());
        if (this.clientBurstIndex >= total) {
            return;
        }
        if (this.clientBurstTicks % BURST_INTERVAL == 0) {
            this.spawnBurst(this.clientBurstIndex);
            this.clientBurstIndex++;
        }
        this.clientBurstTicks++;
    }

    private void spawnClientTrail() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        Vec3 motion = this.getDeltaMovement();
        double speed = motion.length();

        double throttle = Math.min(1.0, speed / MAX_SPEED);

        if (!this.trailStarted) {
            this.trailStarted = true;
            this.spawnIgnitionPuff(x, y, z);
        }

        int coreCount = 5 + (int) Math.round(throttle * 11.0);
        for (int i = 0; i < coreCount; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double along = this.random.nextDouble() * this.random.nextDouble();
            double radius = BODY_RADIUS * (0.18 + 0.62 * along) * Math.sqrt(this.random.nextDouble());
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;
            this.level().addParticle(
                ParticleTypes.FLAME,
                x + dx, y - along * CORE_LENGTH, z + dz,
                dx * 0.2,
                -motion.y * 0.5 - 0.02,
                dz * 0.2
            );
        }

        int sparkCount = 2 + (int) Math.round(throttle * 2.0);
        for (int i = 0; i < sparkCount; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            double outward = 0.04 + this.random.nextDouble() * 0.04;
            this.level().addParticle(
                ParticleTypes.FIREWORK,
                x + dx * BODY_RADIUS, y + 0.2, z + dz * BODY_RADIUS,
                dx * outward,
                -motion.y * 0.35 - this.random.nextDouble() * 0.05,
                dz * outward
            );
        }

        if (this.getLife() % 2 == 0) {
            int puffs = 1 + (int) Math.round(throttle);
            for (int i = 0; i < puffs; i++) {
                double back = (double) i / puffs;
                this.level().addParticle(
                    this.random.nextBoolean()
                        ? ParticleTypes.LARGE_SMOKE
                        : ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                    x - motion.x * back, y - motion.y * back + 0.4, z - motion.z * back,
                    this.random.nextGaussian() * 0.03,
                    -0.02 - this.random.nextDouble() * 0.03,
                    this.random.nextGaussian() * 0.03
                );
            }
        }

        if (this.getLife() % 3 == 0) {
            this.level().addParticle(
                ParticleTypes.ASH,
                x + this.random.nextGaussian() * BODY_RADIUS,
                y + 0.3,
                z + this.random.nextGaussian() * BODY_RADIUS,
                this.random.nextGaussian() * 0.02,
                -0.05 - this.random.nextDouble() * 0.05,
                this.random.nextGaussian() * 0.02
            );
        }

        if (throttle > 0.5) {
            int streaks = (int) Math.round(throttle * 2.0);
            for (int i = 0; i < streaks; i++) {
                double back = this.random.nextDouble();
                this.level().addParticle(
                    ParticleTypes.END_ROD,
                    x - motion.x * back, y - motion.y * back + 0.2, z - motion.z * back,
                    0.0, -motion.y * 0.15, 0.0
                );
            }
        }
    }

    private void spawnIgnitionPuff(double x, double y, double z) {
        for (int i = 0; i < 16; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double radius = BODY_RADIUS * (0.6 + this.random.nextDouble());
            this.level().addParticle(
                ParticleTypes.LARGE_SMOKE,
                x + Math.cos(angle) * radius, y + 0.2, z + Math.sin(angle) * radius,
                Math.cos(angle) * 0.12, 0.02, Math.sin(angle) * 0.12
            );
        }
        for (int i = 0; i < 18; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double radius = BODY_RADIUS * this.random.nextDouble();
            this.level().addParticle(
                ParticleTypes.FLAME,
                x + Math.cos(angle) * radius, y + 0.15, z + Math.sin(angle) * radius,
                Math.cos(angle) * 0.05, 0.01, Math.sin(angle) * 0.05
            );
        }
    }

    private void spawnBurst(int index) {
        List<FireworkExplosion> explosions = this.getExplosions();
        if (explosions.isEmpty()) {
            this.level().createFireworks(
                this.getX(), this.blastY(), this.getZ(), 0.0, 0.0, 0.0, explosions
            );
            return;
        }
        if (index < 0 || index >= explosions.size()) {
            return;
        }
        FireworkExplosion explosion = explosions.get(index);
        double radius = spreadRadius(explosions.size());

        RandomSource random = RandomSource.create(this.getId() * 31L + index);
        for (int ball = 0; ball < LargeFireworksRocketBurst.BALLS_PER_STAR; ball++) {
            Vec3 offset = randomOffset(random, radius);
            LargeFireworksRocketBurst.spawn(
                this.level(),
                random,
                this.getX() + offset.x,
                this.blastY() + offset.y,
                this.getZ() + offset.z,
                explosion,
                LargeFireworksRocketBurst.ballScale(random, LargeFireworksRocketBurst.randomScale(random))
            );
        }
    }

    private static Vec3 randomOffset(RandomSource random, double radius) {
        if (radius <= 0.0) {
            return Vec3.ZERO;
        }

        double z = random.nextDouble() * 2.0 - 1.0;
        double ring = Math.sqrt(Math.max(0.0, 1.0 - z * z));
        double theta = random.nextDouble() * Math.PI * 2.0;

        double r = radius * Math.cbrt(random.nextDouble());
        return new Vec3(Math.cos(theta) * ring * r, z * r, Math.sin(theta) * ring * r);
    }

    private static double spreadRadius(int total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.min(SPREAD_MAX, SPREAD_BASE + SPREAD_PER_STAR * (total - 1));
    }

    private double blastY() {
        return this.getY() + BODY_HEIGHT / 2.0;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Life", this.getLife());
        output.putInt("Fuse", this.getFuse());
        output.store("FireworksItem", ItemStack.CODEC, this.entityData.get(DATA_FIREWORKS_ITEM));
        output.putBoolean("Detonating", this.isDetonating());
        output.putInt("BurstTicks", this.burstTicks);
        output.store("Facing", Direction.CODEC, this.getFacing());
        output.putInt("Pearls", this.getPearls());
        if (!this.pearlOwners.isEmpty()) {
            ValueOutput.ValueOutputList owners = output.childrenList("PearlOwners");
            for (UUID owner : this.pearlOwners) {
                owners.addChild().putString("Id", owner.toString());
            }
        }
        if (this.moonTravelSucceeds != null) {
            output.putBoolean("MoonTravelSucceeds", this.moonTravelSucceeds);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setLife(input.getIntOr("Life", 0));
        this.setFuse(input.getIntOr("Fuse", DEFAULT_FUSE));
        input.read("FireworksItem", ItemStack.CODEC).ifPresent(stack ->
            this.entityData.set(DATA_FIREWORKS_ITEM, stack));
        this.setDetonating(input.getBooleanOr("Detonating", false));
        this.burstTicks = input.getIntOr("BurstTicks", 0);
        List<UUID> owners = new ArrayList<>();
        for (ValueInput owner : input.childrenListOrEmpty("PearlOwners")) {
            owner.getString("Id").ifPresent(raw -> {
                try {
                    owners.add(UUID.fromString(raw));
                } catch (IllegalArgumentException error) {
                    AnvilCraftPigeonPlus.LOGGER.warn(
                        "Large firework: unparsable pearl owner UUID, skipped: {}", raw);
                }
            });
        }
        this.setPearlOwners(owners);
        this.moonTravelSucceeds = input.read("MoonTravelSucceeds", Codec.BOOL).orElse(null);
        input.read("Facing", Direction.CODEC).ifPresent(this::setFacing);
    }

    @Override
    protected @NonNull Vec3 getPassengerAttachmentPoint(@NonNull Entity passenger, @NonNull EntityDimensions dimensions, float partialTick) {
        return new Vec3(0.0, BODY_HEIGHT, 0.0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        this.markHurt();
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return false;
    }

    @Override
    public boolean ignoreExplosion(@NonNull Explosion explosion) {
        return true;
    }
}
