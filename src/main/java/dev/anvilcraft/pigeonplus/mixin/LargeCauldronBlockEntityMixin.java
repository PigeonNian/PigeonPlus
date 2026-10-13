package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.util.HeaterCompat;
import dev.anvilcraft.pigeonplus.util.NozzleExhaustUtil;
import dev.anvilcraft.pigeonplus.util.GasEscapeUtil;
import dev.anvilcraft.pigeonplus.util.PotionParticleUtil;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LargeCauldronBlockEntity.class)
public class LargeCauldronBlockEntityMixin {
    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void pigeonplus$escapeGas(
        Level level,
        BlockPos pos,
        BlockState state,
        LargeCauldronBlockEntity entity,
        CallbackInfo ci
    ) {
        if (level instanceof ServerLevel serverLevel && entity.isMainPart()) {
            GasEscapeUtil.escapeLargeCauldronGas(level, pos, entity.getFluids());
            if (entity.isIgnited()) {
                NozzleExhaustUtil.JetPropellant propellant = NozzleExhaustUtil.getAvailableJetPropellant(entity);
                if (propellant != null) {
                    NozzleExhaustUtil.spawnPropellantParticles(serverLevel, pos, propellant);
                }
            }
        }
    }

    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void pigeonplus$emitPotionParticles(
        Level level,
        BlockPos pos,
        BlockState state,
        LargeCauldronBlockEntity entity,
        CallbackInfo ci
    ) {
        if (entity.isMainPart()) {
            PotionParticleUtil.emitLargeCauldronParticles(level, pos, entity.getFluids());
        }
    }

    @Inject(method = "canIgniteTopFluid", at = @At("RETURN"), cancellable = true)
    private void pigeonplus$allowMixedPropellantIgnition(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()
            && NozzleExhaustUtil.hasAnyPropellant((LargeCauldronBlockEntity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isActiveRecipeHelper", at = @At("RETURN"), cancellable = true)
    private static void pigeonplus$acceptHeatingBlenderAsHelper(
        BlockState state,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ() && HeaterCompat.isHeatingElectricBlender(state)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isActiveHeatingHelper", at = @At("RETURN"), cancellable = true)
    private static void pigeonplus$acceptHeatingBlenderAsHeatingHelper(
        BlockState state,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ() && HeaterCompat.isHeatingElectricBlender(state)) {
            cir.setReturnValue(true);
        }
    }
}
