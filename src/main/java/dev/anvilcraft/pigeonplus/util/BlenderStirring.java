package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BlenderStirring {
    private static final float ROTATION_DEGREES_PER_TICK = 45.0F;
    private static final int ARRAY_RADIUS = 1;
    private static final int ARRAY_OFFSET_Y = 2;
    private static final double VORTEX = 0.4D;

    private BlenderStirring() {
    }

    public static float angle(Level level, float partialTick) {
        return (level.getGameTime() + partialTick) * ROTATION_DEGREES_PER_TICK;
    }

    public static double angleAt(double baseDegrees, double radius) {
        return baseDegrees * (1.0D + VORTEX / (radius + VORTEX));
    }

    public static boolean isStirring(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof BlenderBlock
            && state.getOptionalValue(BlenderBlock.WORKING).orElse(false);
    }

    public static boolean isStirringBelowLargeCauldron(Level level, BlockPos mainPos) {
        BlockPos center = mainPos.below(ARRAY_OFFSET_Y);
        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-ARRAY_RADIUS, 0, -ARRAY_RADIUS),
            center.offset(ARRAY_RADIUS, 0, ARRAY_RADIUS)
        )) {
            if (isStirring(level, pos)) {
                return true;
            }
        }
        return false;
    }
}
