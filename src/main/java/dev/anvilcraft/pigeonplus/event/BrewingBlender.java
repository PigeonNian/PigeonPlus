package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BrewingBlender {
    private static final int MAX_STRUCTURE_HEIGHT = 16;

    private BrewingBlender() {
    }

    public static boolean hasBrewingBlenderBelow(Level level, BlockPos vesselPos) {
        return isRunningBlazeBlender(level.getBlockState(vesselPos.below()));
    }

    public static boolean hasBulkBrewingBlenderArrayBelow(
        Level level,
        BlockPos mainPos,
        BlockState structureBlock
    ) {
        BlockPos bottom = mainPos;
        for (int guard = 0; guard < MAX_STRUCTURE_HEIGHT; guard++) {
            BlockState below = level.getBlockState(bottom.below());
            if (!below.is(structureBlock.getBlock())) {
                break;
            }
            bottom = bottom.below();
        }
        BlockPos center = bottom.below();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            if (isRunningBlazeElectricBlender(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRunningBlazeBlender(BlockState state) {
        boolean blender = state.is(AddonBlocks.BLENDER.get()) || state.is(AddonBlocks.ELECTRIC_BLENDER.get());
        return blender && isBlazeHead(state) && isWorking(state);
    }

    private static boolean isRunningBlazeElectricBlender(BlockState state) {
        return state.is(AddonBlocks.ELECTRIC_BLENDER.get()) && isBlazeHead(state) && isWorking(state);
    }

    private static boolean isBlazeHead(BlockState state) {
        return state.hasProperty(BlenderBlock.HEAD)
            && state.getValue(BlenderBlock.HEAD) == BlenderBlock.BlenderHead.BLAZE;
    }

    private static boolean isWorking(BlockState state) {
        return state.hasProperty(BlenderBlock.WORKING) && state.getValue(BlenderBlock.WORKING);
    }
}
