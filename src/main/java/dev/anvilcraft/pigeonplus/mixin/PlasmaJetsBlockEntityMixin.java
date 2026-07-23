package dev.anvilcraft.pigeonplus.mixin;

import com.mojang.datafixers.util.Pair;
import dev.anvilcraft.pigeonplus.init.AddonHeaterInfos;
import dev.anvilcraft.pigeonplus.init.AddonParticles;
import dev.anvilcraft.pigeonplus.util.NozzlePlasmaJetUtil;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import dev.dubhe.anvilcraft.init.ModParticles;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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
        NozzlePlasmaJetUtil.seedTubeWalls(this.tubeWalls, this.pigeonplus$blockPos(), facing);
        if (!NozzlePlasmaJetUtil.canSustainJet(level, cauldron)) {
            this.pigeonplus$removeJet(level);
            return;
        }

        if (level.getGameTime() % NozzlePlasmaJetUtil.PLASMA_CONSUME_INTERVAL == 0
            && !NozzlePlasmaJetUtil.consumeTopOilOnce(cauldron)) {
            this.pigeonplus$removeJet(level);
            return;
        }

        HeaterManager.addProducer(this.pigeonplus$blockPos(), level, AddonHeaterInfos.NO_MAGNET_NOZZLE_PLASMA_JETS);
        HeaterManager.addProducer(this.pigeonplus$blockPos(), level, AddonHeaterInfos.MAGNET_NOZZLE_PLASMA_JETS);
        this.pigeonplus$hurtNozzleJetEntities(level);
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
        NozzlePlasmaJetUtil.seedTubeWalls(this.tubeWalls, this.pigeonplus$blockPos(), facing);
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

    private void pigeonplus$hurtNozzleJetEntities(Level level) {
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        BlockPos startPos = this.pigeonplus$blockPos();
        Direction facing = NozzlePlasmaJetUtil.getStructuralFacing(level, startPos);
        if (facing == null) {
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

    private void pigeonplus$spawnNozzleJetParticles(net.minecraft.client.multiplayer.ClientLevel level) {
        RandomSource random = level.getRandom();
        BlockPos jetPos = this.pigeonplus$blockPos();
        Direction facing = NozzlePlasmaJetUtil.getStructuralFacing(level, jetPos);
        if (facing == null) {
            return;
        }
        double baseAxis = -0.92;
        double visualHeight = NozzlePlasmaJetUtil.JET_VISUAL_HEIGHT;

        for (int i = 0; i < 6; i++) {
            Vec3 pos = pigeonplus$point(
                jetPos,
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

        for (int i = 0; i < 2; i++) {
            Vec3 pos = pigeonplus$point(
                jetPos,
                facing,
                -0.45 + random.nextDouble() * 1.9,
                baseAxis + random.nextDouble() * 0.18,
                -0.45 + random.nextDouble() * 1.9
            );
            Vec3 velocity = pigeonplus$vector(
                facing,
                (random.nextDouble() - 0.5) * 0.03,
                0.08 + random.nextDouble() * 0.16,
                (random.nextDouble() - 0.5) * 0.03
            );
            level.addParticle(
                ParticleTypes.FLAME,
                pos.x,
                pos.y,
                pos.z,
                velocity.x,
                velocity.y,
                velocity.z
            );
        }

        for (int i = 0; i < 4; i++) {
            double t = 0.12 + random.nextDouble() * 0.78;
            double radius = 0.20 + (1.0 - t) * 0.32 + random.nextDouble() * 0.10;
            double angle = random.nextDouble() * Math.PI * 2.0;
            Vec3 pos = pigeonplus$point(
                jetPos,
                facing,
                0.5 + Math.cos(angle) * radius,
                baseAxis + t * visualHeight,
                0.5 + Math.sin(angle) * radius
            );
            Vec3 velocity = pigeonplus$vector(
                facing,
                (random.nextDouble() - 0.5) * 0.02,
                0.04 + random.nextDouble() * 0.08,
                (random.nextDouble() - 0.5) * 0.02
            );
            level.addParticle(
                ParticleTypes.FLAME,
                pos.x,
                pos.y,
                pos.z,
                velocity.x,
                velocity.y,
                velocity.z
            );
            if (random.nextFloat() < 0.65F) {
                Vec3 plasmaVelocity = pigeonplus$vector(
                    facing,
                    (random.nextDouble() - 0.5) * 0.03,
                    0.08 + random.nextDouble() * 0.10,
                    (random.nextDouble() - 0.5) * 0.03
                );
                level.addParticle(
                    ModParticles.PLASMA_JETS.get(),
                    true,
                    pos.x,
                    pos.y,
                    pos.z,
                    plasmaVelocity.x,
                    plasmaVelocity.y,
                    plasmaVelocity.z
                );
            }
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
            Vec3 pos = pigeonplus$point(jetPos, facing, rimX, rimAxis, rimZ);
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
                AddonParticles.ROLLING_PLASMA.get(),
                pos.x,
                pos.y,
                pos.z,
                fastVelocity.x,
                fastVelocity.y,
                fastVelocity.z
            );
            level.addParticle(
                AddonParticles.ROLLING_PLASMA.get(),
                pos.x,
                pos.y,
                pos.z,
                slowVelocity.x,
                slowVelocity.y,
                slowVelocity.z
            );
        }

        if (random.nextFloat() < 0.45F) {
            Vec3 pos = pigeonplus$point(
                jetPos,
                facing,
                -0.45 + random.nextDouble() * 1.9,
                baseAxis + random.nextDouble() * 0.12,
                -0.45 + random.nextDouble() * 1.9
            );
            Vec3 velocity = pigeonplus$vector(
                facing,
                (random.nextDouble() - 0.5) * 0.02,
                0.05 + random.nextDouble() * 0.08,
                (random.nextDouble() - 0.5) * 0.02
            );
            level.addParticle(
                ParticleTypes.SMOKE,
                pos.x,
                pos.y,
                pos.z,
                velocity.x,
                velocity.y,
                velocity.z
            );
        }

        if (random.nextFloat() < 0.35F) {
            double t = 0.35 + random.nextDouble() * 0.55;
            Vec3 pos = pigeonplus$point(
                jetPos,
                facing,
                0.1 + random.nextDouble() * 0.8,
                baseAxis + t * visualHeight,
                0.1 + random.nextDouble() * 0.8
            );
            Vec3 velocity = pigeonplus$vector(
                facing,
                (random.nextDouble() - 0.5) * 0.02,
                0.03 + random.nextDouble() * 0.04,
                (random.nextDouble() - 0.5) * 0.02
            );
            level.addParticle(
                ParticleTypes.SMOKE,
                pos.x,
                pos.y,
                pos.z,
                velocity.x,
                velocity.y,
                velocity.z
            );
        }

        if (random.nextFloat() < 0.28F) {
            Vec3 pos = pigeonplus$point(
                jetPos,
                facing,
                -0.35 + random.nextDouble() * 1.7,
                baseAxis + random.nextDouble() * 0.10,
                -0.35 + random.nextDouble() * 1.7
            );
            Vec3 velocity = pigeonplus$vector(
                facing,
                (random.nextDouble() - 0.5) * 0.05,
                0.10 + random.nextDouble() * 0.12,
                (random.nextDouble() - 0.5) * 0.05
            );
            level.addParticle(
                ParticleTypes.LAVA,
                pos.x,
                pos.y,
                pos.z,
                velocity.x,
                velocity.y,
                velocity.z
            );
        }
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
