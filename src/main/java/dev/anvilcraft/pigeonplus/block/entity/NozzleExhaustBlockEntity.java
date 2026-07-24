package dev.anvilcraft.pigeonplus.block.entity;

import com.mojang.datafixers.util.Pair;
import dev.anvilcraft.pigeonplus.init.AddonHeaterInfos;
import dev.anvilcraft.pigeonplus.init.AddonParticles;
import dev.anvilcraft.pigeonplus.init.AddonVaporizationSources;
import dev.anvilcraft.pigeonplus.util.NozzleExhaustUtil;
import dev.anvilcraft.pigeonplus.util.StasisTimeFreezeManager;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.init.ModParticles;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Set;

public class NozzleExhaustBlockEntity extends BlockEntity {
    private static final double KEROSENE_ACCELERATION_PER_TICK = 320.0 / StasisTimeFreezeManager.MAX_FREEZE_TICKS;
    private static final double METHANE_ACCELERATION_PER_TICK = 192.0 / StasisTimeFreezeManager.MAX_FREEZE_TICKS;

    private int duration;

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
            this.duration = 0;
            return;
        }
        LargeCauldronBlockEntity cauldron = NozzleExhaustUtil.getStructuralCauldron(level, this.worldPosition);
        if (cauldron == null) {
            this.duration = 0;
            return;
        }
        Direction facing = NozzleExhaustUtil.getStructuralFacing(level, this.worldPosition);
        BlockPos outletPos = NozzleExhaustUtil.getStructuralOutletPos(level, this.worldPosition);
        if (facing == null || outletPos == null) {
            this.duration = 0;
            return;
        }
        AddonVaporizationSources.JetPropellant propellant = NozzleExhaustUtil.getJetPropellant(level, cauldron);
        if (propellant == null || !NozzleExhaustUtil.canSustainJet(level, cauldron)) {
            this.duration = 0;
            return;
        }

        if (level.getGameTime() % NozzleExhaustUtil.PLASMA_CONSUME_INTERVAL == 0
            && !NozzleExhaustUtil.consumeTopFuelOnce(cauldron, propellant)) {
            this.duration = 0;
            return;
        }

        HeaterManager.addProducer(this.worldPosition, level, AddonHeaterInfos.NO_MAGNET_NOZZLE_EXHAUST);
        HeaterManager.addProducer(this.worldPosition, level, AddonHeaterInfos.MAGNET_NOZZLE_EXHAUST);
        this.accelerateEntities(level, outletPos, facing, propellant);
        this.hurtEntities(level, outletPos, facing);
        this.provideCharge(level);
        this.duration++;
    }

    private void clientTick(Level level) {
        if (!NozzleExhaustUtil.isNozzleActive(level, this.worldPosition)) {
            return;
        }
        this.spawnParticles(level);
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
            if (entity.hurt(ModDamageTypes.plasmaJets(level), 16.0f)) {
                entity.playSound(SoundEvents.GENERIC_BURN, 0.4f, 2.0f + RandomSource.create().nextFloat() * 0.4f);
            }
        }
    }

    private void accelerateEntities(
        ServerLevel level,
        BlockPos startPos,
        Direction facing,
        AddonVaporizationSources.JetPropellant propellant
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

    private static double accelerationPerTick(AddonVaporizationSources.JetPropellant propellant) {
        return propellant == AddonVaporizationSources.JetPropellant.METHANE
            ? METHANE_ACCELERATION_PER_TICK
            : KEROSENE_ACCELERATION_PER_TICK;
    }

    private void spawnParticles(Level level) {
        RandomSource random = level.getRandom();
        Direction facing = NozzleExhaustUtil.getStructuralFacing(level, this.worldPosition);
        BlockPos outletPos = NozzleExhaustUtil.getStructuralOutletPos(level, this.worldPosition);
        if (facing == null || outletPos == null) {
            return;
        }
        LargeCauldronBlockEntity cauldron = NozzleExhaustUtil.getStructuralCauldron(level, this.worldPosition);
        AddonVaporizationSources.JetPropellant propellant = cauldron == null
            ? AddonVaporizationSources.JetPropellant.KEROSENE
            : NozzleExhaustUtil.getJetPropellant(level, cauldron);
        boolean methane = propellant == AddonVaporizationSources.JetPropellant.METHANE;
        var rollingParticle = methane ? AddonParticles.ROLLING_METHANE_PLASMA.get() : AddonParticles.ROLLING_PLASMA.get();
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
}
