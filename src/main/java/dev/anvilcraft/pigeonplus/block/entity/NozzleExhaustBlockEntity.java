package dev.anvilcraft.pigeonplus.block.entity;

import com.mojang.datafixers.util.Pair;
import dev.anvilcraft.pigeonplus.block.NozzleBlock;
import dev.anvilcraft.pigeonplus.init.AddonDamageTypes;
import dev.anvilcraft.pigeonplus.init.AddonHeaterInfos;
import dev.anvilcraft.pigeonplus.init.AddonParticles;
import dev.anvilcraft.pigeonplus.init.ModCriterionTriggers;
import dev.anvilcraft.pigeonplus.util.NozzleExhaustUtil;
import dev.anvilcraft.pigeonplus.util.StasisTimeFreezeManager;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.init.ModParticles;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class NozzleExhaustBlockEntity extends BlockEntity {
    public static final int STARTUP_TICKS = 28;
    public static final int VISUAL_PARTICLE_DELAY_TICKS = 10;
    public static final double NOZZLE_ACTIVATION_RADIUS = 8.0;
    private static final double KEROSENE_ACCELERATION_PER_TICK = 320.0 / StasisTimeFreezeManager.MAX_FREEZE_TICKS;
    private static final double METHANE_ACCELERATION_PER_TICK = 192.0 / StasisTimeFreezeManager.MAX_FREEZE_TICKS;
    private static final double HYDROGEN_ACCELERATION_PER_TICK = 160.0 / StasisTimeFreezeManager.MAX_FREEZE_TICKS;
    private static final int CENTER_HEAT_INTERVAL_TICKS = 10;
    private static final int CENTER_HEAT_DURATION_TICKS = 20 * 20;
    public static final int BLOCKED_EXPLODE_TICKS = 200;
    private static final float NOZZLE_EXPLOSION_RADIUS = 2.5F;
    private static final String EXHAUST_TICKS_TAG = "ExhaustTicks";
    private static final String BLOCKED_TICKS_TAG = "BlockedTicks";
    private static final String ACTIVE_PROPELLANT_TAG = "ActivePropellant";

    private int duration;
    private int blockedTicks;
    private NozzleExhaustUtil.JetPropellant activePropellant =
        NozzleExhaustUtil.JetPropellant.KEROSENE;

    public NozzleExhaustBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NOZZLE_EXHAUST.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, NozzleExhaustBlockEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            entity.serverTick(serverLevel);
            return;
        }
        entity.clientTick(level);
    }

    private void serverTick(ServerLevel level) {
        if (!NozzleExhaustUtil.isNozzleActive(level, this.worldPosition)) {
            this.stopExhaust();
            return;
        }
        LargeCauldronBlockEntity cauldron = NozzleExhaustUtil.getStructuralCauldron(level, this.worldPosition);
        if (cauldron == null) {
            this.stopExhaust();
            return;
        }
        Direction facing = NozzleExhaustUtil.getStructuralFacing(level, this.worldPosition);
        BlockPos outletPos = NozzleExhaustUtil.getStructuralOutletPos(level, this.worldPosition);
        if (facing == null || outletPos == null) {
            this.stopExhaust();
            return;
        }
        NozzleExhaustUtil.JetPropellant propellant = NozzleExhaustUtil.getJetPropellant(level, cauldron);
        if (propellant == null || !NozzleExhaustUtil.canSustainJet(level, cauldron)) {
            this.stopExhaust();
            return;
        }
        this.activePropellant = propellant;

        if (level.getGameTime() % NozzleExhaustUtil.PLASMA_CONSUME_INTERVAL == 0
            && !NozzleExhaustUtil.consumeTopFuelOnce(cauldron, propellant)) {
            this.stopExhaust();
            return;
        }

        if (NozzleExhaustUtil.isOutletAreaFullyBlocked(level, outletPos, facing)) {
            this.blockedTicks++;
            if (this.blockedTicks >= BLOCKED_EXPLODE_TICKS) {
                this.explodeNozzle(level, outletPos);
                return;
            }
        } else if (this.blockedTicks != 0) {
            this.blockedTicks = 0;
            this.setChanged();
        }

        HeaterManager.addProducer(this.worldPosition, level, AddonHeaterInfos.NO_MAGNET_NOZZLE_EXHAUST);
        HeaterManager.addProducer(this.worldPosition, level, AddonHeaterInfos.MAGNET_NOZZLE_EXHAUST);
        this.accelerateEntities(level, outletPos, facing, propellant);
        this.hurtEntities(level, outletPos, facing);
        this.igniteObstructingBlock(level, outletPos, facing);
        this.provideCharge(level);
        boolean activating = this.duration == 0;
        this.tickExhaust();
        if (activating) {
            this.grantNozzleActivation(level);
        }
    }

    private void clientTick(Level level) {
        if (!NozzleExhaustUtil.isNozzleActive(level, this.worldPosition)
            || this.getExhaustPhase() == ExhaustPhase.IDLE) {
            return;
        }
        if (this.duration < VISUAL_PARTICLE_DELAY_TICKS) {
            this.duration++;
            return;
        }
        this.spawnParticles(level);
        this.duration++;
    }

    public ExhaustPhase getExhaustPhase() {
        if (this.duration <= 0) {
            return ExhaustPhase.IDLE;
        }
        return this.duration <= STARTUP_TICKS ? ExhaustPhase.STARTING : ExhaustPhase.FIRING;
    }

    public boolean isExhaustStarting() {
        return this.getExhaustPhase() == ExhaustPhase.STARTING;
    }

    public boolean isExhaustFiring() {
        return this.getExhaustPhase() == ExhaustPhase.FIRING;
    }

    public NozzleExhaustUtil.JetPropellant getActivePropellant() {
        return this.activePropellant;
    }

    private void tickExhaust() {
        ExhaustPhase previous = this.getExhaustPhase();
        this.duration++;
        if (previous != this.getExhaustPhase()) {
            this.syncToClient();
        } else {
            this.setChanged();
        }
    }

    private void stopExhaust() {
        if (this.duration == 0 && this.blockedTicks == 0) {
            return;
        }
        this.duration = 0;
        this.blockedTicks = 0;
        this.syncToClient();
    }

    private void explodeNozzle(ServerLevel level, BlockPos outletPos) {
        this.grantNozzleExplosion(level);
        BlockState state = this.getBlockState();
        if (state.getBlock() instanceof NozzleBlock nozzle && nozzle.isMainPart(state)) {
            nozzle.forEachPart(level, this.worldPosition, partPos -> {
                BlockState oldState = level.getBlockState(partPos);
                BlockState newState = oldState.getFluidState().createLegacyBlock();
                level.setBlockAndUpdate(partPos, newState);
            });
        }
        Vec3 center = Vec3.atCenterOf(outletPos);
        level.explode(null, center.x, center.y, center.z, NOZZLE_EXPLOSION_RADIUS, Level.ExplosionInteraction.BLOCK);
    }

    private void grantNozzleExplosion(ServerLevel level) {
        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        double radiusSqr = NOZZLE_ACTIVATION_RADIUS * NOZZLE_ACTIVATION_RADIUS;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.distanceToSqr(center) > radiusSqr) {
                continue;
            }
            ModCriterionTriggers.NOZZLE_EXPLOSION.get().trigger(player);
        }
    }

    private void grantNozzleActivation(ServerLevel level) {
        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        double radiusSqr = NOZZLE_ACTIVATION_RADIUS * NOZZLE_ACTIVATION_RADIUS;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.distanceToSqr(center) > radiusSqr) {
                continue;
            }
            ModCriterionTriggers.NOZZLE_GAS_ACTIVATED.get().trigger(player);
        }
    }

    private void syncToClient() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public Pair<Set<BlockPos>, Set<BlockPos>> getHeatingPoses() {
        if (this.level == null) {
            return Pair.of(Set.of(), Set.of());
        }
        return NozzleExhaustUtil.collectRingTargets(this.level, this.worldPosition).toHeatingPoses();
    }

    private void hurtEntities(Level level, BlockPos startPos, Direction facing) {
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        int effectiveHeight = NozzleExhaustUtil.getEffectiveJetLength(
            level,
            startPos,
            facing,
            NozzleExhaustUtil.JET_DAMAGE_HEIGHT
        );
        if (effectiveHeight <= 0) {
            return;
        }
        Collection<Entity> entities = level.getEntitiesOfClass(
            Entity.class,
            NozzleExhaustUtil.getJetEffectBounds(startPos, facing, effectiveHeight),
            entity -> !entity.fireImmune()
        );
        for (Entity entity : entities) {
            entity.igniteForSeconds(15.0f);
            if (entity.hurt(AddonDamageTypes.nozzleExhaust(level), 16.0f)) {
                entity.playSound(SoundEvents.GENERIC_BURN, 0.4f, 2.0f + RandomSource.create().nextFloat() * 0.4f);
            }
        }
    }

    private void igniteObstructingBlock(ServerLevel level, BlockPos startPos, Direction facing) {
        if (level.getGameTime() % CENTER_HEAT_INTERVAL_TICKS != 0) {
            return;
        }
        BlockPos obstructionPos = NozzleExhaustUtil.getJetRenderObstructionPos(
            level,
            startPos,
            facing,
            NozzleExhaustUtil.JET_VISUAL_HEIGHT
        );
        if (obstructionPos == null) {
            return;
        }
        BlockState obstructionState = level.getBlockState(obstructionPos);
        if (this.heatObstructingBlockToIncandescent(level, obstructionPos, obstructionState)) {
            return;
        }
        if (!obstructionState.isFlammable(level, obstructionPos, facing.getOpposite())) {
            return;
        }
        BlockPos firePos = obstructionPos.relative(facing.getOpposite());
        if (!BaseFireBlock.canBePlacedAt(level, firePos, facing)) {
            return;
        }
        level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
    }

    private boolean heatObstructingBlockToIncandescent(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.is(ModBlockTags.HEATABLE_BLOCKS)) {
            return false;
        }
        Optional<HeatTier> currentTier = HeatRecorder.getTier(level, pos, state);
        if (currentTier.isPresent() && currentTier.get().compareTo(HeatTier.INCANDESCENT) >= 0) {
            if (level.getBlockEntity(pos) instanceof HeatableBlockEntity heatable) {
                heatable.addDurationInTick(CENTER_HEAT_DURATION_TICKS);
            }
            return true;
        }
        Optional<Block> incandescentBlock = HeatRecorder.getId(level, pos, state)
            .flatMap(id -> HeatRecorder.getHeatableBlock(id, HeatTier.INCANDESCENT));
        if (incandescentBlock.isEmpty()) {
            return false;
        }
        Block block = incandescentBlock.get();
        BlockState incandescentState = block.defaultBlockState();
        level.setBlock(pos, incandescentState, Block.UPDATE_CLIENTS);
        if (block instanceof EntityBlock entityBlock) {
            BlockEntity blockEntity = entityBlock.newBlockEntity(pos, incandescentState);
            if (blockEntity instanceof HeatableBlockEntity heatable) {
                level.setBlockEntity(heatable);
                heatable.addDurationInTick(CENTER_HEAT_DURATION_TICKS);
            }
        }
        return true;
    }

    private void accelerateEntities(
        ServerLevel level,
        BlockPos startPos,
        Direction facing,
        NozzleExhaustUtil.JetPropellant propellant
    ) {
        int effectiveHeight = NozzleExhaustUtil.getEffectiveJetLength(
            level,
            startPos,
            facing,
            NozzleExhaustUtil.JET_RANGE_HEIGHT
        );
        if (effectiveHeight <= 0) {
            return;
        }
        Vec3 acceleration = Vec3.atLowerCornerOf(facing.getNormal()).scale(accelerationPerTick(propellant));
        Collection<Entity> entities = level.getEntitiesOfClass(
            Entity.class,
            NozzleExhaustUtil.getJetEffectBounds(startPos, facing, effectiveHeight),
            entity -> !entity.isSpectator() && NozzleExhaustUtil.isInJetCenterLine(entity, startPos, facing)
        );
        for (Entity entity : entities) {
            if (!StasisTimeFreezeManager.captureMomentum(entity, acceleration)) {
                entity.setDeltaMovement(entity.getDeltaMovement().add(acceleration));
                entity.hurtMarked = true;
            }
        }
    }

    private void provideCharge(Level level) {
        if (level.getGameTime() % (ChargeCollectorBlockEntity.INPUT_COOLDOWN * 20) != 0) {
            return;
        }
        ChargeCollectorManager instance = ChargeCollectorManager.getInstance(level);
        for (BlockPos magnetPos : NozzleExhaustUtil.collectRingTargets(level, this.worldPosition).magnetPoses()) {
            instance.charge(512, magnetPos);
        }
    }

    private static double accelerationPerTick(NozzleExhaustUtil.JetPropellant propellant) {
        if (propellant == null) {
            return KEROSENE_ACCELERATION_PER_TICK;
        }
        return switch (propellant) {
            case METHANE -> METHANE_ACCELERATION_PER_TICK;
            case HYDROGEN -> HYDROGEN_ACCELERATION_PER_TICK;
            default -> KEROSENE_ACCELERATION_PER_TICK;
        };
    }

    private void spawnParticles(Level level) {
        RandomSource random = level.getRandom();
        Direction facing = NozzleExhaustUtil.getStructuralFacing(level, this.worldPosition);
        BlockPos outletPos = NozzleExhaustUtil.getStructuralOutletPos(level, this.worldPosition);
        if (facing == null || outletPos == null) {
            return;
        }
        NozzleExhaustUtil.JetPropellant propellant = this.activePropellant;
        boolean methane = propellant == NozzleExhaustUtil.JetPropellant.METHANE;
        var rollingParticle = switch (propellant) {
            case METHANE -> AddonParticles.ROLLING_METHANE_PLASMA.get();
            case HYDROGEN -> AddonParticles.ROLLING_HYDROGEN_PLASMA.get();
            default -> AddonParticles.ROLLING_PLASMA.get();
        };
        double baseAxis = -0.92;

        for (int i = 0; i < 6; i++) {
            Vec3 pos = point(
                outletPos,
                facing,
                -0.55 + random.nextDouble() * 2.1,
                baseAxis + random.nextDouble() * 0.28,
                -0.55 + random.nextDouble() * 2.1
            );
            Vec3 velocity = vector(
                facing,
                (random.nextDouble() - 0.5) * 0.08,
                0.9 + random.nextDouble() * 0.9,
                (random.nextDouble() - 0.5) * 0.08
            );
            level.addParticle(ModParticles.PLASMA_JETS.get(), true, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
        }

        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 2.0 + random.nextDouble() * 0.4;
            double dirX = Math.cos(angle);
            double dirZ = Math.sin(angle);
            double rimX = 0.5 + dirX * radius;
            double rimZ = 0.5 + dirZ * radius;
            double rimAxis = baseAxis + 0.5 + random.nextDouble() * 0.22;
            double targetScale = 0.5 / Math.max(Math.abs(dirX), Math.abs(dirZ));
            double targetX = 0.5 + dirX * targetScale;
            double targetAxis = rimAxis + 2.0;
            double targetZ = 0.5 + dirZ * targetScale;
            double speed = 0.11 + random.nextDouble() * 0.09;
            double inwardX = targetX - rimX;
            double inwardY = targetAxis - rimAxis;
            double inwardZ = targetZ - rimZ;
            double inwardLength = Math.sqrt(inwardX * inwardX + inwardY * inwardY + inwardZ * inwardZ);
            if (inwardLength > 1.0E-6) {
                inwardX = inwardX / inwardLength * speed;
                inwardY = inwardY / inwardLength * speed;
                inwardZ = inwardZ / inwardLength * speed;
            }
            Vec3 pos = point(outletPos, facing, rimX, rimAxis, rimZ);
            Vec3 fastVelocity = vector(
                facing,
                inwardX * 0.72 + (random.nextDouble() - 0.5) * 0.012,
                inwardY * 0.72 + (random.nextDouble() - 0.5) * 0.012,
                inwardZ * 0.72 + (random.nextDouble() - 0.5) * 0.012
            );
            Vec3 slowVelocity = vector(
                facing,
                inwardX * 0.56 + (random.nextDouble() - 0.5) * 0.010,
                inwardY * 0.56 + (random.nextDouble() - 0.5) * 0.010,
                inwardZ * 0.56 + (random.nextDouble() - 0.5) * 0.010
            );
            level.addParticle(rollingParticle, pos.x, pos.y, pos.z, fastVelocity.x, fastVelocity.y, fastVelocity.z);
            level.addParticle(rollingParticle, pos.x, pos.y, pos.z, slowVelocity.x, slowVelocity.y, slowVelocity.z);
        }

        this.spawnImpactParticles(level, outletPos, facing);
    }

    private void spawnImpactParticles(Level level, BlockPos outletPos, Direction facing) {
        BlockPos obstructionPos = NozzleExhaustUtil.getJetRenderObstructionPos(
            level,
            outletPos,
            facing,
            NozzleExhaustUtil.JET_VISUAL_HEIGHT
        );
        if (obstructionPos == null) {
            return;
        }

        RandomSource random = level.getRandom();
        Direction[] plane = planeDirections(facing);
        Vec3 firstAxis = Vec3.atLowerCornerOf(plane[0].getNormal());
        Vec3 secondAxis = Vec3.atLowerCornerOf(plane[1].getNormal());
        Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 impactCenter = Vec3.atCenterOf(obstructionPos).subtract(normal.scale(0.51));
        BlockState obstructionState = level.getBlockState(obstructionPos);
        BlockParticleOption blockParticle = new BlockParticleOption(ParticleTypes.BLOCK, obstructionState);

        for (int i = 0; i < 14; i++) {
            double firstOffset = (random.nextDouble() - 0.5) * 1.55;
            double secondOffset = (random.nextDouble() - 0.5) * 1.55;
            Vec3 pos = impactCenter.add(firstAxis.scale(firstOffset)).add(secondAxis.scale(secondOffset));
            Vec3 tangent = firstAxis
                .scale(firstOffset + (random.nextDouble() - 0.5) * 0.35)
                .add(secondAxis.scale(secondOffset + (random.nextDouble() - 0.5) * 0.35));
            if (tangent.lengthSqr() < 1.0E-6) {
                tangent = firstAxis.scale(random.nextBoolean() ? 1.0 : -1.0);
            }
            tangent = tangent.normalize().scale(0.16 + random.nextDouble() * 0.26);
            Vec3 velocity = tangent
                .subtract(normal.scale(0.05 + random.nextDouble() * 0.08))
                .add(
                    (random.nextDouble() - 0.5) * 0.026,
                    (random.nextDouble() - 0.5) * 0.026,
                    (random.nextDouble() - 0.5) * 0.026
                );
            level.addParticle(ModParticles.PLASMA_JETS.get(), true, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
            if (i < 8) {
                Vec3 chipVelocity = tangent
                    .scale(0.68)
                    .subtract(normal.scale(0.035 + random.nextDouble() * 0.055))
                    .add(0.0, random.nextDouble() * 0.035, 0.0);
                level.addParticle(
                    blockParticle,
                    true,
                    pos.x,
                    pos.y,
                    pos.z,
                    chipVelocity.x,
                    chipVelocity.y,
                    chipVelocity.z
                );
            }
            if (i < 6) {
                Vec3 smokeVelocity = tangent
                    .scale(0.24)
                    .subtract(normal.scale(0.016 + random.nextDouble() * 0.030))
                    .add(0.0, 0.035 + random.nextDouble() * 0.055, 0.0);
                level.addParticle(ParticleTypes.CLOUD, true, pos.x, pos.y, pos.z, smokeVelocity.x, smokeVelocity.y, smokeVelocity.z);
            }
        }
    }

    private static Direction[] planeDirections(Direction facing) {
        return switch (facing.getAxis()) {
            case Y -> new Direction[] {Direction.NORTH, Direction.EAST};
            case X -> new Direction[] {Direction.UP, Direction.NORTH};
            case Z -> new Direction[] {Direction.UP, Direction.EAST};
        };
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(EXHAUST_TICKS_TAG, this.duration);
        tag.putInt(BLOCKED_TICKS_TAG, this.blockedTicks);
        tag.putString(ACTIVE_PROPELLANT_TAG, this.activePropellant.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(EXHAUST_TICKS_TAG)) {
            this.duration = Math.max(0, tag.getInt(EXHAUST_TICKS_TAG));
        }
        if (tag.contains(BLOCKED_TICKS_TAG)) {
            this.blockedTicks = Math.max(0, tag.getInt(BLOCKED_TICKS_TAG));
        }
        this.activePropellant = parsePropellant(tag.getString(ACTIVE_PROPELLANT_TAG));
    }

    private static NozzleExhaustUtil.JetPropellant parsePropellant(String name) {
        for (NozzleExhaustUtil.JetPropellant propellant : NozzleExhaustUtil.JetPropellant.values()) {
            if (propellant.name().equals(name)) {
                return propellant;
            }
        }
        return NozzleExhaustUtil.JetPropellant.KEROSENE;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    public enum ExhaustPhase {
        IDLE,
        STARTING,
        FIRING
    }
}
