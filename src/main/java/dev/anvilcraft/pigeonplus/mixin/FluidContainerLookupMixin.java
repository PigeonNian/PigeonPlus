package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.util.CompressedAirDrainFluidHandler;
import dev.dubhe.anvilcraft.api.fluid.network.FluidContainerLookup;
import dev.dubhe.anvilcraft.block.entity.fluid.DrainBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidContainerLookup.class)
public class FluidContainerLookupMixin {
    @Inject(method = "find", at = @At("RETURN"), cancellable = true)
    private static void pigeonplus$wrapExposedDrainAsCompressedAirSource(
        Level level,
        BlockPos pos,
        Direction side,
        CallbackInfoReturnable<FluidContainerLookup.Result> cir
    ) {
        FluidContainerLookup.Result result = cir.getReturnValue();
        if (result == null
            || result.handler() instanceof CompressedAirDrainFluidHandler
            || !(level.getBlockEntity(pos) instanceof DrainBlockEntity)
            || !CompressedAirDrainFluidHandler.isDrainAirExposed(level, pos)) {
            return;
        }
        cir.setReturnValue(new FluidContainerLookup.Result(
            new CompressedAirDrainFluidHandler(level, pos, result.handler()),
            result.cauldron(),
            result.entity()
        ));
    }
}
