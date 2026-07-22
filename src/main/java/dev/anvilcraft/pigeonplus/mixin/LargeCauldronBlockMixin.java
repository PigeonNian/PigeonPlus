package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LargeCauldronBlock.class)
public class LargeCauldronBlockMixin {
    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$exposeTopSurfaceForNozzlePlacement(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context,
        CallbackInfoReturnable<VoxelShape> cir
    ) {
        Cube3x3PartHalf part = state.getValue(LargeCauldronBlock.HALF);
        if (part.getOffsetY() == 2 && context.isHoldingItem(AddonBlocks.NOZZLE.asItem())) {
            cir.setReturnValue(Shapes.block());
        }
    }
}
