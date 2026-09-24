package dev.anvilcraft.pigeonplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.pigeonplus.client.UppercutClientState;
import dev.anvilcraft.pigeonplus.util.DoomfistEnchantmentUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;

/**
 * 上勾拳的第一人称手部动画：锤头<strong>从下方起手</strong>，直接自下而上挑出去。
 *
 * <p>没有单独的下压过程——动作一开始锤头就已经在低位，立刻向上抡。
 * 这样起手即出拳，不存在「先压再打」的前摇。
 *
 * <p>分两段：
 * <ol>
 *   <li><strong>上挑</strong>：从低位（负角）一路抡到斜上方（正角）。</li>
 *   <li><strong>归位</strong>：从斜上方落回待机（0°）。</li>
 * </ol>
 *
 * <h3>角度符号</h3>
 * 手部空间是相机对齐的：{@code +X = 屏幕右}、{@code +Y = 屏幕上}、{@code -Z = 视线前方}。
 * 锤身位于手部枢轴<strong>上方</strong>（手握着锤柄下端），设锤头相对枢轴在 {@code (0, +L, 0)}，
 * 施加 {@code Rx(t)} 后 {@code y' = L·cos t}：
 * <pre>
 *   t =    0°  → 锤头朝上   （待机）
 *   t =  -90°  → 锤头朝正前
 *   t = -160°  → 锤头朝下   （起手位）
 *   t =  +35°  → 锤头斜上   （上挑收势）
 * </pre>
 * 即<strong>负 = 锤头向下，正 = 锤头向上</strong>。
 * 这与现有裂地重拳参数一致（{@code RAISE = +60°} 为举到最高、{@code SLAM = -110°} 为下砸到底）。
 *
 * <p>挂在 {@code RenderHandEvent} 上，该事件在 {@code ItemInHandRenderer#renderHandsWithItems}
 * 里、{@code renderArmWithItem} <strong>之前</strong>触发且带着当时的 {@code PoseStack}。
 */
public final class UppercutHandAnimation {
    // ---------------------------------------------------------------- 关键姿态

    /**
     * 起手位：锤头所在的角度（度）。
     *
     * <p>-160° 即锤头朝下、略偏身前。这就是动作的<strong>起点</strong>，
     * 所以第一帧锤子就已经在低位——没有下压过程，起手即出拳。
     *
     * <p><strong>想让起手位置更靠下就调大绝对值</strong>（上限 -180°，即正下方）；
     * 想更偏前就调小（如 -120°）。
     */
    private static final float WINDUP_ROTATION_X = -160.0f;

    /**
     * 上挑到位时锤头所在的角度（度）。
     *
     * <p>正角 = 锤头向上。取 +35° 停在「斜上略偏后」的收势位置，
     * 而不是停到正上方（+90°）——斜上更留着往前的冲劲，正上方是纯收势、显得泄劲。
     */
    private static final float STRIKE_ROTATION_X = 35.0f;

    /** 起手时手向下沉的距离（格）。 */
    private static final float WINDUP_TRANSLATE_Y = -0.05f;
    /** 起手时向前的位移（格，-Z = 向前），避免锤头在低位时手贴到近裁剪面。 */
    private static final float WINDUP_TRANSLATE_Z = -0.08f;
    /** 上挑到位时手向上抬的距离（格）。 */
    private static final float STRIKE_TRANSLATE_Y = 0.12f;
    /** 上挑到位时向后的位移（格，+Z = 朝玩家），避免锤头上扬时穿进视野。 */
    private static final float STRIKE_TRANSLATE_Z = 0.05f;

    // ---------------------------------------------------------------- 枢轴

    /**
     * 旋转枢轴相对手部基准位<strong>向下</strong>的偏移（格）。
     *
     * <p>枢轴越靠下，锤子绕越接近柄尾的点转动，动作看起来越像「握着锤柄末端抡」。
     * 正值 = 枢轴下移。这是纯观感参数：想让旋转中心更靠下就调大。
     */
    private static final float PIVOT_DROP_Y = 0.12f;

    // ---------------------------------------------------------------- 时间分配

    /**
     * 上挑结束（到达顶点）的进度点。
     *
     * <p>0 → 0.6：约 6 tick 完成主挥击。行程近 200°，给足这几 tick 才不至于看着像瞬移。
     * 之后 0.6 → 1.0 归位，让收尾从容。
     */
    private static final float STRIKE_END = 0.6f;

    private UppercutHandAnimation() {
    }

    /**
     * 由 {@code RenderHandEvent} 调用。
     *
     * @param equipProgress 装备进度，取自事件（切手时短暂非零），用于还原原版手部枢轴
     */
    public static void onRenderHand(
        PoseStack poseStack,
        InteractionHand hand,
        float partialTick,
        float equipProgress
    ) {
        // 只看主手：锤子在主手
        if (hand != InteractionHand.MAIN_HAND) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        // 只有手持带铁拳附魔的铁砧锤才加动画
        if (!DoomfistEnchantmentUtil.hasDoomfist(player.getMainHandItem())) return;

        float swing = UppercutClientState.swingProgress(partialTick);
        if (swing <= 0.0f) return;

        float rotationX;
        float translateY;
        float translateZ;

        if (swing <= STRIKE_END) {
            // 上挑：从低位（起手即有角度）一路抡到斜上方。
            // t 从 0 开始，所以第一帧就在 WINDUP_ROTATION_X，没有前置的下压过渡。
            float t = easeOut(swing / STRIKE_END);
            rotationX = Mth.lerp(t, WINDUP_ROTATION_X, STRIKE_ROTATION_X);
            translateY = Mth.lerp(t, WINDUP_TRANSLATE_Y, STRIKE_TRANSLATE_Y);
            translateZ = Mth.lerp(t, WINDUP_TRANSLATE_Z, STRIKE_TRANSLATE_Z);
        } else {
            // 归位：从收势角落回待机（0°）。
            // 落回 0° 而不是落回起手位，否则看着像又抡了一次。
            // 用平方让它前快后慢，收尾更干脆。
            float back = 1.0f - (swing - STRIKE_END) / (1.0f - STRIKE_END);
            float eased = back * back;
            rotationX = STRIKE_ROTATION_X * eased;
            translateY = STRIKE_TRANSLATE_Y * eased;
            translateZ = STRIKE_TRANSLATE_Z * eased;
        }

        // 位移先在「屏幕方向」上施加（相对手部，不参与枢轴补偿）。
        // 旋转走枢轴版：绕握把（并可再下移）转动，而不是绕相机原点。
        poseStack.translate(0.0f, translateY, translateZ);
        HandViewCorrection.applyWithArmPivot(
            poseStack, player, partialTick, rotationX, equipProgress, PIVOT_DROP_Y
        );
    }

    /**
     * 缓出插值：起步快、接近终点时变慢。
     */
    private static float easeOut(float t) {
        float inv = 1.0f - Mth.clamp(t, 0.0f, 1.0f);
        return 1.0f - inv * inv;
    }
}
