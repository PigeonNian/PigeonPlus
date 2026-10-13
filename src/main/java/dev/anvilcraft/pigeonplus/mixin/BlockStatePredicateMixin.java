package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.pigeonplus.util.HeaterCompat;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockStatePredicate.class)
public class BlockStatePredicateMixin {
    @Inject(method = "cannotMatch", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$doNotPruneHeatingElectricBlender(
        BlockState state,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (HeaterCompat.isHeatingElectricBlender(state)
            && HeaterCompat.acceptsHeater((BlockStatePredicate) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "testWithoutEntity", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$acceptHeatingElectricBlender(
        BlockState state,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (HeaterCompat.isHeatingElectricBlender(state)
            && HeaterCompat.acceptsHeater((BlockStatePredicate) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
