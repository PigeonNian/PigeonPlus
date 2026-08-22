package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.block.NozzleBlock;
import dev.anvilcraft.pigeonplus.util.NozzleExhaustUtil;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
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
    private static final VoxelShape PIGEONPLUS_LID_CENTER = Block.box(0.0, 12.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape PIGEONPLUS_LID_W = Block.box(4.0, 12.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape PIGEONPLUS_LID_E = Block.box(0.0, 12.0, 0.0, 12.0, 16.0, 16.0);
    private static final VoxelShape PIGEONPLUS_LID_N = Block.box(0.0, 12.0, 4.0, 16.0, 16.0, 16.0);
    private static final VoxelShape PIGEONPLUS_LID_S = Block.box(0.0, 12.0, 0.0, 16.0, 16.0, 12.0);
    private static final VoxelShape PIGEONPLUS_LID_WN = Block.box(4.0, 12.0, 4.0, 16.0, 16.0, 16.0);
    private static final VoxelShape PIGEONPLUS_LID_WS = Block.box(4.0, 12.0, 0.0, 16.0, 16.0, 12.0);
    private static final VoxelShape PIGEONPLUS_LID_EN = Block.box(0.0, 12.0, 4.0, 12.0, 16.0, 16.0);
    private static final VoxelShape PIGEONPLUS_LID_ES = Block.box(0.0, 12.0, 0.0, 12.0, 16.0, 12.0);
    private static final VoxelShape PIGEONPLUS_BOTTOM_CENTER = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);
    private static final VoxelShape PIGEONPLUS_BOTTOM_W = Block.box(11.0, 0.0, 0.0, 16.0, 4.0, 16.0);
    private static final VoxelShape PIGEONPLUS_BOTTOM_E = Block.box(0.0, 0.0, 0.0, 5.0, 4.0, 16.0);
    private static final VoxelShape PIGEONPLUS_BOTTOM_N = Block.box(0.0, 0.0, 11.0, 16.0, 4.0, 16.0);
    private static final VoxelShape PIGEONPLUS_BOTTOM_S = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 5.0);
    private static final VoxelShape PIGEONPLUS_BOTTOM_WN = Block.box(11.0, 0.0, 11.0, 16.0, 4.0, 16.0);
    private static final VoxelShape PIGEONPLUS_BOTTOM_WS = Block.box(11.0, 0.0, 0.0, 16.0, 4.0, 5.0);
    private static final VoxelShape PIGEONPLUS_BOTTOM_EN = Block.box(0.0, 0.0, 11.0, 5.0, 4.0, 16.0);
    private static final VoxelShape PIGEONPLUS_BOTTOM_ES = Block.box(0.0, 0.0, 0.0, 5.0, 4.0, 5.0);

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

    @Inject(method = "getShape", at = @At("RETURN"), cancellable = true)
    private void pigeonplus$addNozzleCauldronAttachmentCollision(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context,
        CallbackInfoReturnable<VoxelShape> cir
    ) {
        Cube3x3PartHalf part = state.getValue(LargeCauldronBlock.HALF);
        if (!(level instanceof Level realLevel)) {
            return;
        }
        BlockPos mainPos = ((LargeCauldronBlock) (Object) this).getMainPartPos(pos, state);
        VoxelShape attachmentShape = Shapes.empty();
        boolean hasBottomNozzle = pigeonplus$hasBottomNozzle(realLevel, mainPos);
        boolean hasHorizontalNozzle = pigeonplus$hasHorizontalNozzle(realLevel, mainPos);
        if (part.getOffsetY() == 2 && (hasHorizontalNozzle || hasBottomNozzle)) {
            attachmentShape = pigeonplus$getLidShape(part);
        } else if (part.getOffsetY() == 0 && hasBottomNozzle) {
            attachmentShape = pigeonplus$getBottomShape(part);
        }
        if (attachmentShape.isEmpty()) {
            return;
        }
        cir.setReturnValue(Shapes.or(cir.getReturnValue(), attachmentShape));
    }

    private static VoxelShape pigeonplus$getLidShape(Cube3x3PartHalf part) {
        return switch (part) {
            case TOP_CENTER -> PIGEONPLUS_LID_CENTER;
            case TOP_W -> PIGEONPLUS_LID_W;
            case TOP_E -> PIGEONPLUS_LID_E;
            case TOP_N -> PIGEONPLUS_LID_N;
            case TOP_S -> PIGEONPLUS_LID_S;
            case TOP_WN -> PIGEONPLUS_LID_WN;
            case TOP_WS -> PIGEONPLUS_LID_WS;
            case TOP_EN -> PIGEONPLUS_LID_EN;
            case TOP_ES -> PIGEONPLUS_LID_ES;
            default -> Shapes.empty();
        };
    }

    private static VoxelShape pigeonplus$getBottomShape(Cube3x3PartHalf part) {
        return switch (part) {
            case BOTTOM_CENTER -> PIGEONPLUS_BOTTOM_CENTER;
            case BOTTOM_W -> PIGEONPLUS_BOTTOM_W;
            case BOTTOM_E -> PIGEONPLUS_BOTTOM_E;
            case BOTTOM_N -> PIGEONPLUS_BOTTOM_N;
            case BOTTOM_S -> PIGEONPLUS_BOTTOM_S;
            case BOTTOM_WN -> PIGEONPLUS_BOTTOM_WN;
            case BOTTOM_WS -> PIGEONPLUS_BOTTOM_WS;
            case BOTTOM_EN -> PIGEONPLUS_BOTTOM_EN;
            case BOTTOM_ES -> PIGEONPLUS_BOTTOM_ES;
            default -> Shapes.empty();
        };
    }

    private static boolean pigeonplus$hasBottomNozzle(Level level, BlockPos cauldronPos) {
        BlockPos nozzlePos = cauldronPos.below(NozzleExhaustUtil.NOZZLE_MAIN_OFFSET_Y);
        BlockState state = level.getBlockState(nozzlePos);
        return state.getBlock() instanceof NozzleBlock nozzle
            && nozzle.isMainPart(state)
            && state.getValue(NozzleBlock.FACING) == Direction.DOWN;
    }

    private static boolean pigeonplus$hasHorizontalNozzle(Level level, BlockPos cauldronPos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos nozzlePos = cauldronPos.relative(direction, NozzleExhaustUtil.NOZZLE_MAIN_OFFSET_Y);
            BlockState state = level.getBlockState(nozzlePos);
            if (state.getBlock() instanceof NozzleBlock nozzle
                && nozzle.isMainPart(state)
                && state.getValue(NozzleBlock.FACING) == direction) {
                return true;
            }
        }
        return false;
    }
}
