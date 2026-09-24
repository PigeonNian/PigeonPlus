package dev.anvilcraft.pigeonplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.client.RocketPunchAnimState;
import dev.anvilcraft.pigeonplus.client.RocketPunchClientState;
import dev.anvilcraft.pigeonplus.util.DoomfistEnchantmentUtil;
import dev.anvilcraft.pigeonplus.util.RocketPunchDashRegistry;
import dev.anvilcraft.pigeonplus.util.RocketPunchManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;

/**
 * 火箭重拳的第一人称手部动画，分三段：
 *
 * <ol>
 *   <li><strong>蓄力</strong>：长按右键期间，锤子向后收（绕 Y 轴外翻 + 后拉），
 *       随蓄力进度逐渐到位。到满蓄力后停住不动，把「已经满了」交给 HUD 表达。</li>
 *   <li><strong>冲刺</strong>：松开右键后手臂前伸、锤头朝前，像被推着飞出去。</li>
 *   <li><strong>命中</strong>：打到生物或撞墙的瞬间猛地向前一顿再回弹。</li>
 * </ol>
 *
 * <p>三个阶段互斥且按时间顺序发生，所以这里用「命中 &gt; 冲刺 &gt; 蓄力」的优先级取其一，
 * 而不是把它们叠加：叠加会让过渡帧出现两个动作同时作用的扭曲姿态。
 *
 * <p>挂在 {@code RenderHandEvent} 上，该事件在 {@code ItemInHandRenderer#renderHandsWithItems}
 * 里、{@code renderArmWithItem} <strong>之前</strong>触发且带着当时的 {@code PoseStack}，
 * 因此改位姿就会作用到随后真正绘制的手持物。
 */
public final class RocketPunchHandAnimation {

    // ---------------------------------------------------------------- 蓄力

    /** 蓄力到位时绕 Y 轴的旋转（度）。正值 = 把锤子向身体外侧转开。 */
    private static final float CHARGE_ROTATION_Y = -25.0f;
    /** 蓄力到位时绕 X 轴的旋转（度）。正值 = 向上抬起锤头。 */
    private static final float CHARGE_ROTATION_X = -18.0f;
    /** 蓄力到位时向后的位移（格，+Z = 朝玩家）。 */
    private static final float CHARGE_TRANSLATE_Z = -0.32f;

    // ---------------------------------------------------------------- 冲刺

    /** 冲刺时绕 X 轴的旋转（度）。负 = 向下/向前压，形成前冲姿态。 */
    private static final float DASH_ROTATION_X = -12.0f;
    /** 冲刺时向前（远离玩家）的位移（格，-Z = 向前）。 */
    private static final float DASH_TRANSLATE_Z = -0.58f;
    /** 冲刺时向下的位移（格）。 */
    private static final float DASH_TRANSLATE_Y = -0.04f;

    // ---------------------------------------------------------------- 命中

    /**
     * 命中瞬间手臂前顶的最大距离（格）。
     *
     * <p>比冲刺的前伸更远、更突然，形成「顿挫」——命中之所以看得见，
     * 靠的是这一下短促的额外前推，而不是新的旋转。
     *
     * <p>取值说明：幅度要给足才看得出「砸中了」。参考裂地重拳下砸用到 -2.0
     * （见 {@code SlamHandAnimation#SLAM_TRANSLATE_Z}），这里 -0.55 仍在安全范围内，
     * 不会把锤子推进近裁剪面而被裁掉。
     */
    private static final float HIT_TRANSLATE_Z = -0.95f;
    /** 命中时向下的位移（格）。配合前推，让姿态像「一拳砸进去」而不只是平移。 */
    private static final float HIT_TRANSLATE_Y = -0.08f;
    /** 命中时绕 X 的额外压迫（度）。负 = 猛地向下/向前压。 */
    private static final float HIT_ROTATION_X = -35.0f;
    /**
     * 命中动作达到最大幅度所占的进度比例。
     *
     * <p>前段快速顶出、后段回弹归位：0.3 让「顶出去」只占很短时间，
     * 看起来像撞上硬物的急停，而不是匀速推出去。
     */
    private static final float HIT_STRIKE_FRACTION = 0.3f;

    private RocketPunchHandAnimation() {
    }

    /**
     * 由 {@code RenderHandEvent} 调用。
     */
    public static void onRenderHand(PoseStack poseStack, InteractionHand hand, float partialTick) {
        // 只看主手：锤子在主手
        if (hand != InteractionHand.MAIN_HAND) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        if (!DoomfistEnchantmentUtil.hasDoomfist(player.getMainHandItem())) return;

        float rotationX = 0.0f;
        float rotationY = 0.0f;
        float translateY = 0.0f;
        float translateZ = 0.0f;
        boolean active = false;

        // 优先级：命中 > 冲刺 > 蓄力。同一时刻只让一个阶段生效。
        float hit = RocketPunchAnimState.hitProgress(partialTick);
        boolean dashing = RocketPunchDashRegistry.isDashing(player.getUUID());

        if (hit > 0.0f) {
            active = true;
            // 前段顶出、后段回弹：用两段插值而不是「进度 × 固定值」，
            // 否则回弹会与顶出对称，看起来像慢慢推出去而不是撞上去。
            if (hit <= HIT_STRIKE_FRACTION) {
                float t = easeOut(hit / HIT_STRIKE_FRACTION);
                translateZ = HIT_TRANSLATE_Z * t;
                translateY = HIT_TRANSLATE_Y * t;
                rotationX = HIT_ROTATION_X * t;
            } else {
                float back = 1.0f - (hit - HIT_STRIKE_FRACTION) / (1.0f - HIT_STRIKE_FRACTION);
                // 回弹到过冲后的余量，用平方让它衰减得更快，收尾更干脆
                float eased = back * back;
                translateZ = HIT_TRANSLATE_Z * eased;
                translateY = HIT_TRANSLATE_Y * eased;
                rotationX = HIT_ROTATION_X * eased;
            }
        } else if (dashing) {
            active = true;
            // 冲刺是一个持续状态，姿态保持不动即可：
            // 开始与结束都由状态切换完成，不需要在这里插值（插值反而会显得软）。
            rotationX = DASH_ROTATION_X;
            translateY = DASH_TRANSLATE_Y;
            translateZ = DASH_TRANSLATE_Z;
        } else if (player.isUsingItem() && DoomfistEnchantmentUtil.hasDoomfist(player.getUseItem())) {
            // 蓄力进度：与 RocketPunchChargeHud 用同一套算法，保证手部动作与进度条同步
            float elapsed = RocketPunchManager.TOTAL_CHARGE_TICKS - player.getUseItemRemainingTicks() + partialTick;
            float progress = Mth.clamp(elapsed / RocketPunchManager.FULL_CHARGE_TICKS, 0.0f, 1.0f);
            if (progress > 0.0f) {
                active = true;
                // 缓出：起手快、接近满时变慢，最后一段「稳稳压住」
                float eased = easeOut(progress);
                rotationY = CHARGE_ROTATION_Y * eased;
                rotationX = CHARGE_ROTATION_X * eased;
                translateZ = CHARGE_TRANSLATE_Z * eased;
            }
        }

        if (!active) return;

        // 位移先在「屏幕方向」上施加
        poseStack.translate(0.0f, translateY, translateZ);

        // 视角修正（与裂地重拳一致）：先补掉俯仰，再施加动画旋转。
        // 必须放在所有旋转之前，才能精确抵消相机旋转里的 Rx(-pitch)；
        // 若把它并进下面的 X 旋转，因为中间还隔着 Y 旋转就无法精确相消。
        // 这里同样**不**做枢轴补偿：本技能的角度是按旧枢轴调定的。
        HandViewCorrection.apply(poseStack, player, partialTick);

        // 先绕 Y 再绕 X：先把手横向甩开、再抬起，符合「抡起来蓄力」的直觉。
        // 顺序反过来的话，Y 轴旋转会把已经抬起的锤子再横向推一下，看着别扭。
        if (rotationY != 0.0f) {
            poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(rotationX));
    }

    /**
     * 缓出插值：起步快、接近终点时变慢。
     *
     * <p>用 {@code 1-(1-t)²} 而不是线性 {@code t}：蓄力动作线性推进会显得很「机械」，
     * 缓出则像有惯性——甩出去时快，稳住时慢。
     */
    private static float easeOut(float t) {
        float inv = 1.0f - Mth.clamp(t, 0.0f, 1.0f);
        return 1.0f - inv * inv;
    }
}
