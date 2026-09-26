package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.util.NozzleCauldronShapes;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 大炼药锅的喷口相关形状处理。
 *
 * <p>注意这里<strong>只处理准星选中轮廓</strong>（{@code getShape}）。
 * 实体碰撞走的是父类的 {@code getCollisionShape}，那条路径不经过 {@code getShape}，
 * 因此由 {@link AbstractMultiPartBlockMixin} 单独注入——
 * 只改这里的话，模型会出现但撞不上去。
 */
@Mixin(LargeCauldronBlock.class)
public class LargeCauldronBlockMixin {
    /**
     * 手持喷口时把顶层当作完整方块，方便把喷口贴在锅盖上。
     */
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

    /**
     * 给额外模型补上<strong>选中轮廓</strong>，否则准星框还是原来那么大。
     *
     * <p>实体碰撞不在这里（见类注释），那个由父类 mixin 负责。
     */
    @Inject(method = "getShape", at = @At("RETURN"), cancellable = true)
    private void pigeonplus$addNozzleAttachmentOutline(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context,
        CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (!(level instanceof Level realLevel)) {
            return;
        }
        LargeCauldronBlock self = (LargeCauldronBlock) (Object) this;
        Cube3x3PartHalf part = state.getValue(LargeCauldronBlock.HALF);
        VoxelShape attachment = NozzleCauldronShapes.attachmentShape(
            realLevel,
            pos,
            self.getMainPartPos(pos, state),
            part.getOffsetY(),
            part.name()
        );
        if (attachment.isEmpty()) {
            return;
        }
        cir.setReturnValue(Shapes.or(cir.getReturnValue(), attachment));
    }
}
