package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.block.entity.AnvilPumpBlockEntity;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidNetworkScanner.class)
public class FluidNetworkScannerMixin {
    @Inject(method = "pumpHalfLift", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$useAnvilPumpHeadlift(Level level, BlockPos pumpPos, CallbackInfoReturnable<Integer> cir) {
        if (level.getBlockEntity(pumpPos) instanceof AnvilPumpBlockEntity pump) {
            cir.setReturnValue(pump.getCurrentHeadlift());
        }
    }
}
