package dev.anvilcraft.pigeonplus.client;

import dev.anvilcraft.pigeonplus.util.RocketPunchDashRegistry;
import net.minecraft.util.Mth;

/**
 * 火箭重拳手部动画的计时状态。
 *
 * <p>只负责「击打到目标」这一段的计时。蓄力与冲刺都能从既有状态直接读出
 * （{@code player.isUsingItem()} 与 {@link RocketPunchDashRegistry}），
 * 不需要额外记时间；而命中是一瞬间的事件，必须有地方记下「刚打中了」这个事实，
 * 否则动作会在同一 tick 内结束、看不见。
 */
public final class RocketPunchAnimState {
    /**
     * 命中动作的时长（tick）。
     *
     * <p>要短：命中是「顿挫」而不是「挥一套」。7 tick（0.35 秒）足够看出
     * 「猛地顶出去 → 回弹」两个节拍，又不至于让玩家觉得动作拖沓。
     */
    private static final int HIT_DURATION = 7;

    /** 命中动作的剩余 tick。 */
    private static int hitTicks;

    private RocketPunchAnimState() {
    }

    /**
     * 触发一次命中动作（打到生物或撞到墙时调用）。
     *
     * <p><strong>已在播放时不重新触发</strong>。这是必要的：命中生物有两条链路——
     * 客户端预测（前方检测到生物）先触发一次，1~2 tick 后服务端的
     * {@code RocketPunchStopPacket} 确认又调一次。若不做去重，第二次会把动画
     * 从头开始，看起来像「顿了两下」。
     * 等到动作播完（{@code hitTicks} 归零）后，新的命中仍能正常触发。
     */
    public static void triggerHit() {
        if (hitTicks > 0) return;
        hitTicks = HIT_DURATION;
    }

    /** 客户端每 tick 调用，递减命中计时。 */
    public static void clientTick() {
        if (hitTicks > 0) hitTicks--;
    }

    /**
     * 命中动作的进度：0.0 = 刚触发，1.0 = 播放完毕。
     *
     * <p>沿用 {@code SeismicSlamClientState} 的算法（用剩余 tick 反推已用时长再按
     * {@code partialTick} 插值），保证与渲染帧对齐、动作随帧率平滑。
     */
    public static float hitProgress(float partialTick) {
        if (hitTicks <= 0) return 0.0f;
        float elapsed = HIT_DURATION - (hitTicks - partialTick);
        return Mth.clamp(elapsed / HIT_DURATION, 0.0f, 1.0f);
    }

    /**
     * 断线或换维度时清空，避免残留的命中动作在下次进入世界时闪一下。
     */
    public static void reset() {
        hitTicks = 0;
    }
}
