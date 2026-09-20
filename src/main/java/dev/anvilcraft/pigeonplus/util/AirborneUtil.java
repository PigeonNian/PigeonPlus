package dev.anvilcraft.pigeonplus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 判断玩家是否处于「悬空」状态。
 *
 * <p>用于指向性裂地重拳的解锁条件：脚底两个方块内没有可站立的方块。
 *
 * <p>为什么不用 {@code Player#onGround()} 单独判断：那个标志在离开地面边缘的
 * 一瞬间仍是 true，也会因为台阶/半砖产生误判。这里直接看脚下的方块，
 * 判定更贴近「脚下是空的」这个直觉描述。
 */
public final class AirborneUtil {
    /** 向下检查的方块层数。 */
    private static final int CHECK_DEPTH = 2;

    private AirborneUtil() {
    }

    /**
     * 玩家脚下 {@value #CHECK_DEPTH} 格内是否没有可站立的方块。
     */
    public static boolean isAirborne(Player player) {
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        for (int depth = 1; depth <= CHECK_DEPTH; depth++) {
            BlockPos pos = feet.below(depth);
            // 未加载的区块按“有方块”处理：宁可不让用，也不要让玩家掉进未加载区
            if (!level.isLoaded(pos)) return false;
            BlockState state = level.getBlockState(pos);
            if (canStandOn(level, pos, state)) return false;
        }
        return true;
    }

    /**
     * 该方块能否支撑玩家站立。
     *
     * <p>用 {@link Block#canSupportRigidBlock} 而不是 {@code isSolid()}：
     * 后者会把草丛、告示牌这类「有碰撞箱但站不住」的方块也算上，
     * 导致玩家站在草上就无法使用指向性裂地。
     */
    private static boolean canStandOn(Level level, BlockPos pos, BlockState state) {
        return Block.canSupportRigidBlock(level, pos) || !state.getCollisionShape(level, pos).isEmpty();
    }
}
