package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.init.AddonVaporizationSources;
import dev.anvilcraft.pigeonplus.util.GasEscapeUtil;
import dev.anvilcraft.pigeonplus.util.NozzlePlasmaJetUtil;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import net.minecraft.core.BlockPos;
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
        if (entity.isMainPart()) {
            GasEscapeUtil.escapeLargeCauldronGas(level, pos, entity.getFluids());
            NozzlePlasmaJetUtil.trySpawn(level, entity);
        }
    }

    @Inject(method = "canIgniteTopFluid", at = @At("RETURN"), cancellable = true)
    private void pigeonplus$allowMixedPropellantIgnition(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()
            && AddonVaporizationSources.hasMixedPropellant((LargeCauldronBlockEntity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
