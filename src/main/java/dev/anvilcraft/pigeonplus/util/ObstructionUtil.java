package dev.anvilcraft.pigeonplus.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 测量玩家前方被方块挡住的比例，用于判断冲刺该不该改成斜向上翻越。
 *
 * <p>为什么不用原版的 {@code minorHorizontalCollision}：那个标志在 {@code Entity} 里
 * 硬编码返回 false，{@code LivingEntity} 与 {@code Player} 都没有覆写，
 * 对玩家永远是 false，拿不到「只蹭到一点」这个信息。
 *
 * <p>实现方式：把玩家碰撞箱的<strong>正面</strong>划分成
 * {@code SAMPLE_COLUMNS × SAMPLE_ROWS} 个采样点，每个点沿冲刺方向打一条射线，
 * 统计命中方块的比例。这就是「擦到的面积」——只看正面，不看体积。
 *
 * <p>为什么不用体积占比：一面完整的墙，沿冲刺方向的扫掠盒体积几乎全被占满，
 * 但占比算出来可能只有 0.3 左右（因为扫掠盒在前进方向上拉得很长），
 * 会把「正面撞墙」误判成「轻轻擦到」，导致玩家直接穿墙。射线命中率没有这个问题：
 * 完整墙面命中率接近 1.0，只蹭到边角才低。
 */
public final class ObstructionUtil {
    /** 正面横向采样列数。 */
    private static final int SAMPLE_COLUMNS = 5;
    /** 正面纵向采样行数。 */
    private static final int SAMPLE_ROWS = 5;

    /** 采样点相对碰撞箱边界的内缩量，避免贴边时射线刚好擦过方块面而误判。 */
    private static final double SAMPLE_INSET = 0.05;

    /**
     * 起点相对脚底抬高一点，跳过「可以自动跨上去」的矮障碍。
     *
     * <p>采样面最低一行原本正好落在脚底，于是台阶、半砖这类只有 0.5 格高的方块
     * 也会被算成遮挡，触发本不该有的抬升。而原版本来就能自动跨过它们
     * （玩家的 {@code maxUpStep} 为 0.6），根本不需要翻越。
     * 抬高 {@code maxUpStep} 再往上取一点，让这类矮坎不参与统计。
     */
    private static final double STEP_CLEARANCE = 0.02;

    private ObstructionUtil() {
    }

    /**
     * 计算玩家正前方被方块挡住的比例。
     *
     * @param player        玩家
     * @param direction     前进方向（会用其水平分量）
     * @param probeDistance 向前探测的距离（格）。应取一 tick 的实际位移，
     *                      这样能在真正撞上之前就发现障碍
     * @return 0.0 = 完全畅通，1.0 = 正面完全被挡
     */
    public static double obstructionRatio(Player player, Vec3 direction, double probeDistance) {
        Level level = player.level();

        // 只用水平分量：斜向冲刺时方向里带垂直分量，会让采样面倾斜
        Vec3 forward = new Vec3(direction.x, 0.0, direction.z);
        if (forward.lengthSqr() < 1.0E-9) return 0.0;
        forward = forward.normalize();

        // 采样面：碰撞箱的正面。横向宽度与纵向高度都内缩一点，
        // 避免贴边时射线刚好擦过方块面而被算成命中。
        // 底部从「可自动跨越的高度」之上开始，这样台阶/半砖不会被误算成障碍。
        double halfWidth = player.getBbWidth() / 2.0 - SAMPLE_INSET;
        double bottom = player.getY() + player.maxUpStep() + STEP_CLEARANCE;
        double top = player.getY() + player.getBbHeight() - SAMPLE_INSET;
        if (halfWidth <= 0.0 || top <= bottom) return 0.0;

        // 正面所在平面上的横向基向量：与 forward 垂直
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);

        int hits = 0;
        int total = 0;
        for (int col = 0; col < SAMPLE_COLUMNS; col++) {
            // 横向偏移：在 [-halfWidth, +halfWidth] 之间均匀取样
            double lateral = SAMPLE_COLUMNS == 1
                ? 0.0
                : (col / (double) (SAMPLE_COLUMNS - 1) - 0.5) * (halfWidth * 2.0);

            for (int row = 0; row < SAMPLE_ROWS; row++) {
                double y = SAMPLE_ROWS == 1
                    ? (bottom + top) / 2.0
                    : bottom + (top - bottom) * row / (double) (SAMPLE_ROWS - 1);

                // 起点取玩家中心所在的水平位置，再按 right 偏移到采样列
                Vec3 start = new Vec3(player.getX(), y, player.getZ()).add(right.scale(lateral));
                Vec3 end = start.add(forward.scale(probeDistance));

                total++;
                if (isBlocked(level, player, start, end)) hits++;
            }
        }

        return total == 0 ? 0.0 : (double) hits / total;
    }

    /**
     * 单条射线是否撞到方块。
     */
    private static boolean isBlocked(Level level, Player player, Vec3 start, Vec3 end) {
        BlockHitResult hit = level.clip(new ClipContext(
            start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        return hit.getType() == HitResult.Type.BLOCK;
    }
}
