package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.util.NozzlePlasmaJetUtil;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.ModParticles;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Set;

@Mixin(PlasmaJetsBlockEntity.class)
public abstract class PlasmaJetsBlockEntityMixin {
    @Shadow private @Nullable BlockPos cauldronPos;
    @Shadow private int duration;
    @Shadow @Final private Set<PlasmaJetsBlockEntity.TubeWallLayer> tubeWalls;

    @Shadow protected abstract void hurtEntities(Level level);

    @Shadow protected abstract void playJetSound(ServerLevel level);

    @Shadow protected abstract void provideCharge(Level level);

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$serverTickNozzleJet(ServerLevel level, CallbackInfo ci) {
        LargeCauldronBlockEntity cauldron = NozzlePlasmaJetUtil.getStructuralCauldron(level, this.pigeonplus$blockPos());
        if (cauldron == null) {
            return;
        }
        ci.cancel();
        this.cauldronPos = cauldron.getBlockPos();
        NozzlePlasmaJetUtil.seedTubeWalls(this.tubeWalls, this.pigeonplus$blockPos());
        if (!NozzlePlasmaJetUtil.canSustainJet(level, cauldron)) {
            this.pigeonplus$removeJet(level);
            return;
        }

        this.duration--;
        if (this.duration + NozzlePlasmaJetUtil.PLASMA_HALF_DURATION < NozzlePlasmaJetUtil.PLASMA_MAX_DURATION
            && NozzlePlasmaJetUtil.consumeTopOilOnce(cauldron)) {
            this.duration += NozzlePlasmaJetUtil.PLASMA_HALF_DURATION;
        }
        if (this.duration < 0) {
            this.pigeonplus$removeJet(level);
            return;
        }

        HeaterManager.addProducer(this.pigeonplus$blockPos(), level, ModHeaterInfos.NO_MAGNET_PLASMA_JETS);
        HeaterManager.addProducer(this.pigeonplus$blockPos(), level, ModHeaterInfos.MAGNET_PLASMA_JETS);
        this.pigeonplus$hurtNozzleJetEntities(level);
        this.provideCharge(level);
        this.playJetSound(level);
    }

    @Inject(method = "clientTick", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$clientTickNozzleJet(net.minecraft.client.multiplayer.ClientLevel level, CallbackInfo ci) {
        LargeCauldronBlockEntity cauldron = NozzlePlasmaJetUtil.getStructuralCauldron(level, this.pigeonplus$blockPos());
        if (cauldron == null) {
            return;
        }
        ci.cancel();
        this.cauldronPos = cauldron.getBlockPos();
        NozzlePlasmaJetUtil.seedTubeWalls(this.tubeWalls, this.pigeonplus$blockPos());
        this.pigeonplus$spawnNozzleJetParticles(level);
    }

    private BlockPos pigeonplus$blockPos() {
        return ((PlasmaJetsBlockEntity) (Object) this).getBlockPos();
    }

    private void pigeonplus$removeJet(Level level) {
        level.removeBlock(this.pigeonplus$blockPos(), false);
    }

    private void pigeonplus$hurtNozzleJetEntities(Level level) {
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        BlockPos startPos = this.pigeonplus$blockPos();
        Collection<Entity> entities = level.getEntitiesOfClass(
            Entity.class,
            new AABB(
                startPos.getX() - NozzlePlasmaJetUtil.JET_RANGE_RADIUS,
                startPos.getY(),
                startPos.getZ() - NozzlePlasmaJetUtil.JET_RANGE_RADIUS,
                startPos.getX() + NozzlePlasmaJetUtil.JET_RANGE_RADIUS + 2,
                startPos.getY() + NozzlePlasmaJetUtil.JET_RANGE_HEIGHT,
                startPos.getZ() + NozzlePlasmaJetUtil.JET_RANGE_RADIUS + 2
            ),
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
        double baseY = jetPos.getY() - 0.92;

        for (int i = 0; i < 6; i++) {
            double x = jetPos.getX() - 0.55 + random.nextDouble() * 2.1;
            double z = jetPos.getZ() - 0.55 + random.nextDouble() * 2.1;
            double y = baseY + random.nextDouble() * 0.28;
            level.addParticle(
                ModParticles.PLASMA_JETS.get(),
                true,
                x,
                y,
                z,
                (random.nextDouble() - 0.5) * 0.08,
                0.9 + random.nextDouble() * 0.9,
                (random.nextDouble() - 0.5) * 0.08
            );
        }

        for (int i = 0; i < 2; i++) {
            double edgeX = jetPos.getX() - 0.45 + random.nextDouble() * 1.9;
            double edgeZ = jetPos.getZ() - 0.45 + random.nextDouble() * 1.9;
            double edgeY = baseY + random.nextDouble() * 0.18;
            level.addParticle(
                ParticleTypes.FLAME,
                edgeX,
                edgeY,
                edgeZ,
                (random.nextDouble() - 0.5) * 0.03,
                0.08 + random.nextDouble() * 0.16,
                (random.nextDouble() - 0.5) * 0.03
            );
        }

        for (int i = 0; i < 7; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 2.0 + random.nextDouble() * 0.4;
            double dirX = Math.cos(angle);
            double dirZ = Math.sin(angle);
            double centerX = jetPos.getX() + 0.5;
            double centerZ = jetPos.getZ() + 0.5;
            double rimX = centerX + dirX * radius;
            double rimZ = centerZ + dirZ * radius;
            double rimY = baseY + 1.0 + random.nextDouble() * 0.22;
            double targetScale = 1.5 / Math.max(Math.abs(dirX), Math.abs(dirZ));
            double targetX = centerX + dirX * targetScale;
            double targetZ = centerZ + dirZ * targetScale;
            double speed = 0.08 + random.nextDouble() * 0.08;
            double inwardX = targetX - rimX;
            double inwardZ = targetZ - rimZ;
            double inwardLength = Math.sqrt(inwardX * inwardX + inwardZ * inwardZ);
            if (inwardLength > 1.0E-6) {
                inwardX = inwardX / inwardLength * speed;
                inwardZ = inwardZ / inwardLength * speed;
            }
            level.addParticle(
                ParticleTypes.CLOUD,
                rimX,
                rimY,
                rimZ,
                inwardX + (random.nextDouble() - 0.5) * 0.02,
                0.03 + random.nextDouble() * 0.05,
                inwardZ + (random.nextDouble() - 0.5) * 0.02
            );
            if (random.nextFloat() < 0.5F) {
                level.addParticle(
                    ParticleTypes.SMOKE,
                    rimX,
                    rimY,
                    rimZ,
                    inwardX * 0.7 + (random.nextDouble() - 0.5) * 0.02,
                    0.02 + random.nextDouble() * 0.04,
                    inwardZ * 0.7 + (random.nextDouble() - 0.5) * 0.02
                );
            }
        }

        if (random.nextFloat() < 0.45F) {
            double smokeX = jetPos.getX() - 0.45 + random.nextDouble() * 1.9;
            double smokeZ = jetPos.getZ() - 0.45 + random.nextDouble() * 1.9;
            double smokeY = baseY + random.nextDouble() * 0.12;
            level.addParticle(
                ParticleTypes.SMOKE,
                smokeX,
                smokeY,
                smokeZ,
                (random.nextDouble() - 0.5) * 0.02,
                0.05 + random.nextDouble() * 0.08,
                (random.nextDouble() - 0.5) * 0.02
            );
        }

        if (random.nextFloat() < 0.28F) {
            double sparkX = jetPos.getX() - 0.35 + random.nextDouble() * 1.7;
            double sparkZ = jetPos.getZ() - 0.35 + random.nextDouble() * 1.7;
            double sparkY = baseY + random.nextDouble() * 0.10;
            level.addParticle(
                ParticleTypes.LAVA,
                sparkX,
                sparkY,
                sparkZ,
                (random.nextDouble() - 0.5) * 0.05,
                0.10 + random.nextDouble() * 0.12,
                (random.nextDouble() - 0.5) * 0.05
            );
        }
    }
}
