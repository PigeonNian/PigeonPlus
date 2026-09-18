package dev.anvilcraft.pigeonplus.client.hud;

import dev.anvilcraft.pigeonplus.util.DoomfistEnchantmentUtil;
import dev.anvilcraft.pigeonplus.util.RocketPunchManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * 火箭重拳的蓄力条：4 段逐渐缩小的梯形竖向堆叠，随蓄力由下往上依次填满。
 *
 * <p>形状为透视感（“前倾”）的梯形柱：最下面一段最宽，往上逐段收窄，左右对称向中心收。
 * 宽度是<strong>整组连续变化</strong>的，所以 4 段之间的斜边能连成一条直线；
 * 段与段之间的空隙保留，仍然看得出是 4 条。
 *
 * <p>{@code GuiGraphics} 只能画矩形，因此每个梯形按 1px 高的横条逐行栅格化——
 * 这样任意高度/宽度下斜边都是精确的，不依赖纹理或着色器。
 *
 * <p>填充自下而上：底部那段的底边先亮起，满一段才轮到上一段，共 4 段对应
 * {@link RocketPunchManager#FULL_CHARGE_TICKS}（满蓄力 20 tick）。
 * 蓄满后进入维持期，进度停在满格不动。
 */
public final class RocketPunchChargeHud {
    /** 段数。 */
    private static final int BAR_COUNT = 4;
    /** 每段高度（像素）。 */
    private static final int BAR_HEIGHT = 3;
    /** 相邻段之间的空隙。 */
    private static final int BAR_GAP = 1;

    /** 最下面一段的宽度（像素），即最宽处。 */
    private static final int MAX_WIDTH = 38;
    /** 最上面一段的宽度（像素），即最窄处。与 MAX_WIDTH 的差值决定“前倾”的陡峭程度。 */
    private static final int MIN_WIDTH = 16;

    /** 整组相对准心向下的偏移。 */
    private static final int CROSSHAIR_OFFSET_Y = 14;
    /** 水平居中于准心。 */
    private static final int CENTER_DIVISOR = 2;

    /** 已填充的亮蓝色。 */
    private static final int COLOR_FILLED = 0xFF35A7FF;
    /** 蓄满后整组切换成的更亮颜色（常亮，不做脉冲）。 */
    private static final int COLOR_FULL = 0xFFB8F0FF;
    /** 未填充的暗蓝色，保留轮廓让 4 段都能被看见。 */
    private static final int COLOR_EMPTY = 0x55203A55;

    /** 满蓄力瞬间「胀大回弹」动画的时长（tick）。0.25 秒。 */
    private static final float POP_TICKS = 5.0f;
    /** 膨胀的最大额外比例。0.35 = 最大放大到 1.35 倍。 */
    private static final float POP_AMPLITUDE = 0.15f;

    /** 整组占用的高度（像素）。 */
    private static final int STACK_HEIGHT = BAR_COUNT * BAR_HEIGHT + (BAR_COUNT - 1) * BAR_GAP;

    private RocketPunchChargeHud() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.screen != null) return;
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isUsingItem()) return;
        if (!DoomfistEnchantmentUtil.hasDoomfist(player.getUseItem())) return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(minecraft.isPaused());
        // 蓄力量 = 已用时长；满蓄力后维持期内钳到 1.0 不再增长
        float elapsed = RocketPunchManager.TOTAL_CHARGE_TICKS - player.getUseItemRemainingTicks() + partialTick;
        float progress = Mth.clamp(elapsed / RocketPunchManager.FULL_CHARGE_TICKS, 0.0f, 1.0f);

        int centerX = graphics.guiWidth() / CENTER_DIVISOR;
        int stackTop = graphics.guiHeight() / CENTER_DIVISOR + CROSSHAIR_OFFSET_Y;

        // 蓄满后整组切成更亮的颜色并保持常亮（维持期内 progress 已钳在 1.0）
        boolean full = progress >= 1.0f;
        int fillColor = full ? COLOR_FULL : COLOR_FILLED;

        // 满蓄力瞬间的胀大回弹：从 progress 到顶的那一刻起算，一个 sin 半波的“鼓起→回落”。
        // 因为维持期内 progress 恒为 1.0，用“超出满蓄力的时长”来单独驱动动画计时。
        float pop = 0.0f;
        if (full) {
            float overTicks = Mth.clamp(elapsed - RocketPunchManager.FULL_CHARGE_TICKS, 0.0f, POP_TICKS);
            // 0→1 走完半个周期，sin 从 0 升到 1 再回到 0
            pop = POP_AMPLITUDE * Mth.sin(overTicks / POP_TICKS * (float) Math.PI);
        }
        float scale = 1.0f + pop;

        // 以整组自身中心为锚点缩放，避免放大时位置漂移
        float centerY = stackTop + (STACK_HEIGHT - 1) / 2.0f;

        // 每段承担的蓄力比例
        float segment = 1.0f / BAR_COUNT;
        for (int index = 0; index < BAR_COUNT; index++) {
            // index 0 是最下面那段：由下往上点亮
            int barTop = stackTop + (BAR_COUNT - 1 - index) * (BAR_HEIGHT + BAR_GAP);

            float filled = Mth.clamp((progress - index * segment) / segment, 0.0f, 1.0f);
            int filledHeight = Math.round(BAR_HEIGHT * filled);
            // 该段中「已填充」区域的顶边；此线以下为亮色，以上为暗色
            int fillTop = barTop + BAR_HEIGHT - filledHeight;

            // 逐行栅格化，左右边缘各自按该行高度插值
            for (int row = 0; row < BAR_HEIGHT; row++) {
                int y = barTop + row;
                int color = y >= fillTop ? fillColor : COLOR_EMPTY;
                // 半宽而非全宽：由 centerX 向两侧镜像展开，对称性由构造保证
                int halfWidth = halfWidthAt(y, stackTop, scale);
                int left = centerX - halfWidth;
                int right = centerX + halfWidth;
                // 宽高同比放大：把该行围绕整组中心纵向展开
                int top = Math.round(centerY + (y - centerY) * scale);
                int bottom = Math.max(top + 1, Math.round(centerY + (y + 1 - centerY) * scale));
                graphics.fill(left, top, right, bottom, color);
            }
        }
    }

    /**
     * 求某一屏幕行对应的梯形<strong>半宽</strong>（像素）。
     *
     * <p>为什么返回半宽而不是全宽：梯形左右对称于 {@code centerX}，若先算全宽再写
     * {@code centerX - width / 2}，一旦宽度是奇数就无法对半平分，{@code Math.round}
     * 会让整行恒定偏向一侧——实测中间那些奇数宽度（35、37…51）全部偏右 1px，
     * 看起来就是左右斜面不对称。
     *
     * <p>改为由 {@code centerX} 向两侧镜像展开（{@code centerX ± halfWidth}），
     * 左右边界在同一行上必然等距，对称性由构造保证，与该行宽度奇偶无关。
     *
     * @param y        目标行
     * @param stackTop 整组最顶部的行
     * @param scale    满蓄力胀大动画的缩放系数
     */
    private static int halfWidthAt(int y, int stackTop, float scale) {
        int bottomY = stackTop + STACK_HEIGHT - 1;
        float span = STACK_HEIGHT - 1;
        // 0 = 最底部，1 = 最顶部
        float fromBottom = Mth.clamp((bottomY - y) / span, 0.0f, 1.0f);
        float width = MAX_WIDTH - (MAX_WIDTH - MIN_WIDTH) * fromBottom;
        // 先缩放再取半宽，最后四舍五入到整数像素
        return Math.max(1, Math.round(width * scale / 2.0f));
    }
}
