package dev.anvilcraft.pigeonplus.mixin;

import com.mojang.datafixers.util.Pair;
import dev.anvilcraft.pigeonplus.init.AddonHeaterInfos;
import dev.anvilcraft.pigeonplus.init.AddonParticles;
import dev.anvilcraft.pigeonplus.init.AddonVaporizationSources;
import dev.anvilcraft.pigeonplus.util.NozzlePlasmaJetUtil;
import dev.anvilcraft.pigeonplus.util.StasisTimeFreezeManager;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import dev.dubhe.anvilcraft.init.ModParticles;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Set;

@Mixin(PlasmaJetsBlockEntity.class)
public abstract class PlasmaJetsBlockEntityMixin {
    private static final double KEROSENE_ACCELERATION_PER_TICK = 320.0 / StasisTimeFreezeManager.MAX_FREEZE_TICKS;
    private static final double METHANE_ACCELERATION_PER_TICK = 192.0 / StasisTimeFreezeManager.MAX_FREEZE_TICKS;

    @Shadow private @Nullable BlockPos cauldronPos;
    @Shadow private int duration;
    @Shadow @Final private Set<PlasmaJetsBlockEntity.TubeWallLayer> tubeWalls;

    @Shadow protected abstract void hurtEntities(Level level);

    @Shadow protected abstract void provideCharge(Level level);

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$serverTickNozzleJet(ServerLevel level, CallbackInfo ci) {
        LargeCauldronBlockEntity cauldron = NozzlePlasmaJetUtil.getStructuralCauldron(level, this.pigeonplus$blockPos());
        if (cauldron == null) {
            return;
        }
        ci.cancel();
        this.cauldronPos = cauldron.getBlockPos();
        Direction facing = NozzlePlasmaJetUtil.getStructuralFacing(level, this.pigeonplus$blockPos());
        if (facing == null) {
            this.pigeonplus$removeJet(level);
            return;
        }
        BlockPos outletPos = NozzlePlasmaJetUtil.getStructuralOutletPos(level, this.pigeonplus$blockPos());
        if (outletPos == null) {
            this.pigeonplus$removeJet(level);
            return;
        }
        NozzlePlasmaJetUtil.seedTubeWalls(this.tubeWalls, outletPos, facing);
        AddonVaporizationSources.JetPropellant propellant = NozzlePlasmaJetUtil.getJetPropellant(level, cauldron);
        if (propellant == null || !NozzlePlasmaJetUtil.canSustainJet(level, cauldron)) {
            this.pigeonplus$removeJet(level);
            return;
        }

        if (level.getGameTime() % NozzlePlasmaJetUtil.PLASMA_CONSUME_INTERVAL == 0
            && !NozzlePlasmaJetUtil.consumeTopFuelOnce(cauldron, propellant)) {
            this.pigeonplus$removeJet(level);
            return;
        }

        HeaterManager.addProducer(this.pigeonplus$blockPos(), level, AddonHeaterInfos.NO_MAGNET_NOZZLE_PLASMA_JETS);
        HeaterManager.addProducer(this.pigeonplus$blockPos(), level, AddonHeaterInfos.MAGNET_NOZZLE_PLASMA_JETS);
        this.pigeonplus$accelerateNozzleJetEntities(level, outletPos, facing, propellant);
        this.pigeonplus$hurtNozzleJetEntities(level, outletPos, facing);
        this.provideCharge(level);
        this.duration++;
    }

    @Inject(method = "clientTick", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$clientTickNozzleJet(net.minecraft.client.multiplayer.ClientLevel level, CallbackInfo ci) {
        LargeCauldronBlockEntity cauldron = NozzlePlasmaJetUtil.getStructuralCauldron(level, this.pigeonplus$blockPos());
        if (cauldron == null) {
            return;
        }
        ci.cancel();
        this.cauldronPos = cauldron.getBlockPos();
        Direction facing = NozzlePlasmaJetUtil.getStructuralFacing(level, this.pigeonplus$blockPos());
        if (facing == null) {
            return;
        }
        BlockPos outletPos = NozzlePlasmaJetUtil.getStructuralOutletPos(level, this.pigeonplus$blockPos());
        if (outletPos == null) {
            return;
        }
        NozzlePlasmaJetUtil.seedTubeWalls(this.tubeWalls, outletPos, facing);
        this.pigeonplus$spawnNozzleJetParticles(level);
    }

    @Inject(method = "getHeatingPoses()Lcom/mojang/datafixers/util/Pair;", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$getHeatingPoses(CallbackInfoReturnable<Pair<Set<BlockPos>, Set<BlockPos>>> cir) {
        Level level = ((PlasmaJetsBlockEntity) (Object) this).getLevel();
        if (level == null) {
            return;
        }
        if (NozzlePlasmaJetUtil.getStructuralCauldron(level, this.pigeonplus$blockPos()) == null) {
            return;
        }
        cir.setReturnValue(NozzlePlasmaJetUtil.collectRingTargets(level, this.pigeonplus$blockPos()).toHeatingPoses());
    }

    @Inject(
        method = "getHeatingPoses(Lnet/minecraft/world/level/Level;)Lcom/mojang/datafixers/util/Pair;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void pigeonplus$getHeatingPoses(
        Level level,
        CallbackInfoReturnable<Pair<Set<BlockPos>, Set<BlockPos>>> cir
    ) {
        if (NozzlePlasmaJetUtil.getStructuralCauldron(level, this.pigeonplus$blockPos()) == null) {
            return;
        }
        cir.setReturnValue(NozzlePlasmaJetUtil.collectRingTargets(level, this.pigeonplus$blockPos()).toHeatingPoses());
    }

    @Inject(method = "provideCharge", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$provideCharge(Level level, CallbackInfo ci) {
        if (NozzlePlasmaJetUtil.getStructuralCauldron(level, this.pigeonplus$blockPos()) == null) {
            return;
        }
        ci.cancel();
        if (level.getGameTime() % (ChargeCollectorBlockEntity.INPUT_COOLDOWN * 20) != 0) {
            return;
        }
        ChargeCollectorManager instance = ChargeCollectorManager.getInstance(level);
        for (BlockPos magnetPos : NozzlePlasmaJetUtil.collectRingTargets(level, this.pigeonplus$blockPos()).magnetPoses()) {
            instance.charge(512, magnetPos);
        }
    }

    private BlockPos pigeonplus$blockPos() {
        return ((PlasmaJetsBlockEntity) (Object) this).getBlockPos();
    }

    private void pigeonplus$removeJet(Level level) {
        this.duration = 0;
        level.removeBlock(this.pigeonplus$blockPos(), false);
    }

    private void pigeonplus$hurtNozzleJetEntities(Level level, BlockPos startPos, Direction facing) {
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        Collection<Entity> entities = level.getEntitiesOfClass(
            Entity.class,
            NozzlePlasmaJetUtil.getJetEffectBounds(startPos, facing, NozzlePlasmaJetUtil.JET_DAMAGE_HEIGHT),
            entity -> !entity.fireImmune()
        );
        for (Entity entity : entities) {
            entity.igniteForSeconds(15.0f);
            if (entity.hurt(ModDamageTypes.plasmaJets(level), 16.0f)) {
                entity.playSound(SoundEvents.GENERIC_BURN, 0.4f, 2.0f + RandomSource.create().nextFloat() * 0.4f);
            }
        }
    }

    private void pigeonplus$accelerateNozzleJetEntities(
        ServerLevel level,
        BlockPos startPos,
        Direction facing,
        AddonVaporizationSources.JetPropellant propellant
    ) {
        Vec3 acceleration = Vec3.atLowerCornerOf(facing.getNormal()).scale(pigeonplus$accelerationPerTick(propellant));
        Collection<Entity> entities = level.getEntitiesOfClass(
            Entity.class,
            NozzlePlasmaJetUtil.getJetEffectBounds(startPos, facing, NozzlePlasmaJetUtil.JET_RANGE_HEIGHT),
            entity -> !entity.isSpectator() && NozzlePlasmaJetUtil.isInJetCenterLine(entity, startPos, facing)
        );
        for (Entity entity : entities) {
            if (!StasisTimeFreezeManager.captureMomentum(entity, acceleration)) {
                entity.setDeltaMovement(entity.getDeltaMovement().add(acceleration));
                entity.hurtMarked = true;
            }
        }
    }

    private static double pigeonplus$accelerationPerTick(AddonVaporizationSources.JetPropellant propellant) {
        return propellant == AddonVaporizationSources.JetPropellant.METHANE
            ? METHANE_ACCELERATION_PER_TICK
            : KEROSENE_ACCELERATION_PER_TICK;
    }

    private void pigeonplus$spawnNozzleJetParticles(net.minecraft.client.multiplayer.ClientLevel level) {
        RandomSource random = level.getRandom();
        BlockPos jetPos = this.pigeonplus$blockPos();
        Direction facing = NozzlePlasmaJetUtil.getStructuralFacing(level, jetPos);
        if (facing == null) {
            return;
        }
        BlockPos outletPos = NozzlePlasmaJetUtil.getStructuralOutletPos(level, jetPos);
        if (outletPos == null) {
            return;
        }
        LargeCauldronBlockEntity cauldron = NozzlePlasmaJetUtil.getStructuralCauldron(level, jetPos);
        AddonVaporizationSources.JetPropellant propellant = cauldron == null
            ? AddonVaporizationSources.JetPropellant.KEROSENE
            : NozzlePlasmaJetUtil.getJetPropellant(level, cauldron);
        boolean methane = propellant == AddonVaporizationSources.JetPropellant.METHANE;
        var rollingParticle = methane ? AddonParticles.ROLLING_METHANE_PLASMA.get() : AddonParticles.ROLLING_PLASMA.get();
        double baseAxis = -0.92;

        for (int i = 0; i < 6; i++) {
            Vec3 pos = pigeonplus$point(
                outletPos,
                facing,
                -0.55 + random.nextDouble() * 2.1,
                baseAxis + random.nextDouble() * 0.28,
                -0.55 + random.nextDouble() * 2.1
            );
            Vec3 velocity = pigeonplus$vector(
                facing,
                (random.nextDouble() - 0.5) * 0.08,
                0.9 + random.nextDouble() * 0.9,
                (random.nextDouble() - 0.5) * 0.08
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
            Vec3 pos = pigeonplus$point(outletPos, facing, rimX, rimAxis, rimZ);
            Vec3 fastVelocity = pigeonplus$vector(
                facing,
                inwardX * 0.72 + (random.nextDouble() - 0.5) * 0.012,
                inwardY * 0.72 + (random.nextDouble() - 0.5) * 0.012,
                inwardZ * 0.72 + (random.nextDouble() - 0.5) * 0.012
            );
            Vec3 slowVelocity = pigeonplus$vector(
                facing,
                inwardX * 0.56 + (random.nextDouble() - 0.5) * 0.010,
                inwardY * 0.56 + (random.nextDouble() - 0.5) * 0.010,
                inwardZ * 0.56 + (random.nextDouble() - 0.5) * 0.010
            );
            level.addParticle(
                rollingParticle,
                pos.x,
                pos.y,
                pos.z,
                fastVelocity.x,
                fastVelocity.y,
                fastVelocity.z
            );
            level.addParticle(
                rollingParticle,
                pos.x,
                pos.y,
                pos.z,
                slowVelocity.x,
                slowVelocity.y,
                slowVelocity.z
            );
        }

        this.pigeonplus$spawnNozzleJetImpactParticles(level, outletPos, facing);
    }

    private void pigeonplus$spawnNozzleJetImpactParticles(
        net.minecraft.client.multiplayer.ClientLevel level,
        BlockPos jetPos,
        Direction facing
    ) {
        BlockPos obstructionPos = NozzlePlasmaJetUtil.getJetRenderObstructionPos(
            level,
            jetPos,
            facing,
            NozzlePlasmaJetUtil.JET_VISUAL_HEIGHT
        );
        if (obstructionPos == null) {
            return;
        }

        RandomSource random = level.getRandom();
        Direction[] plane = pigeonplus$planeDirections(facing);
        Vec3 firstAxis = Vec3.atLowerCornerOf(plane[0].getNormal());
        Vec3 secondAxis = Vec3.atLowerCornerOf(plane[1].getNormal());
        Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 impactCenter = Vec3.atCenterOf(obstructionPos).subtract(normal.scale(0.51));
        BlockState obstructionState = level.getBlockState(obstructionPos);
        BlockParticleOption blockParticle = new BlockParticleOption(ParticleTypes.BLOCK, obstructionState);

        for (int i = 0; i < 14; i++) {
            double firstOffset = (random.nextDouble() - 0.5) * 1.55;
            double secondOffset = (random.nextDouble() - 0.5) * 1.55;
            Vec3 pos = impactCenter
                .add(firstAxis.scale(firstOffset))
                .add(secondAxis.scale(secondOffset));
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
                level.addParticle(
                    ParticleTypes.CLOUD,
                    true,
                    pos.x,
                    pos.y,
                    pos.z,
                    smokeVelocity.x,
                    smokeVelocity.y,
                    smokeVelocity.z
                );
            }
        }
    }

    private static Direction[] pigeonplus$planeDirections(Direction facing) {
        return switch (facing.getAxis()) {
            case Y -> new Direction[] {Direction.NORTH, Direction.EAST};
            case X -> new Direction[] {Direction.UP, Direction.NORTH};
            case Z -> new Direction[] {Direction.UP, Direction.EAST};
        };
    }

    private static Vec3 pigeonplus$point(BlockPos jetPos, Direction facing, double sideX, double axis, double sideZ) {
        Vec3 local = pigeonplus$vector(facing, sideX, axis, sideZ);
        if (facing == Direction.DOWN || facing == Direction.WEST || facing == Direction.NORTH) {
            local = local.add(
                facing == Direction.WEST ? 1.0 : 0.0,
                facing == Direction.DOWN ? 1.0 : 0.0,
                facing == Direction.NORTH ? 1.0 : 0.0
            );
        }
        return new Vec3(jetPos.getX() + local.x, jetPos.getY() + local.y, jetPos.getZ() + local.z);
    }

    private static Vec3 pigeonplus$vector(Direction facing, double sideX, double axis, double sideZ) {
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
