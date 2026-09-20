package dev.anvilcraft.pigeonplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.client.SeismicSlamClientState;
import dev.anvilcraft.pigeonplus.util.DoomfistEnchantmentUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/**
 * 裂地重拳的第一人称手部动画：飞行途中举锤蓄力，落地瞬间抡下去。
 *
 * <p>挂在 {@code RenderHandEvent} 上。该事件在
 * {@code ItemInHandRenderer#renderHandsWithItems} 里、{@code renderArmWithItem}
 * <strong>之前</strong>触发，且带着当时的 {@code PoseStack}，
 * 因此在处理函数里改位姿就会作用到随后真正绘制的手持物。
 *
 * <p>攻速/切手等原版动画不受影响：这里只在裂地重拳期间叠加额外的旋转与位移。
 */
public final class SlamHandAnimation {
    /**
     * 举到最高时绕 X 轴的旋转（度）。
     *
     * <p>符号依据原版挥击动画：{@code ItemInHandRenderer#applyItemArmAttackTransform}
     * 用的是负值（-20°），即<strong>负 = 向前/向下挥</strong>。
     * 举起是相反方向，所以取正值。
     */
    private static final float RAISE_ROTATION_X = 60.0f;
    /** 下砸到底时绕 X 轴的旋转（度）。与举起反号，即向下抡。 */
    private static final float SLAM_ROTATION_X = -110.0f;

    /** 举锤时手向上的位移（格），让动作看起来是「举起来」而不是原地转。 */
    private static final float RAISE_TRANSLATE_Y = 0.15f;
    /** 下砸时手向下的位移。 */
    private static final float SLAM_TRANSLATE_Y = -0.45f;

    /** 举锤时向后（朝玩家）的位移，避免举起后穿进视野。 */
    private static final float RAISE_TRANSLATE_Z = 0.1f;

    /**
     * 下砸时把锤子<strong>向前推</strong>的距离（格）。
     *
     * <p>符号约定：第一人称手部以 {@code applyItemArmTransform} 定基准位姿，
     * 其 Z 为 -0.72，相机朝 -Z 看——所以<strong>更负 = 更靠前</strong>（远离玩家、进画面深处）。
     *
     * <p>为什么要前推：下砸时锤子转到接近 -90°，把手会朝向相机、贴到近裁剪面附近，
     * 于是被裁掉、看不到把手。向前推一点就能把它拉回可视范围。
     * 位移与下抡进度成正比，抡到底最靠前，收回时归位。
     */
    private static final float SLAM_TRANSLATE_Z = -2.0f;

    /**
     * 下砸动作中「抡到最低点」所占的进度比例。
     *
     * <p>前段下抡（快、有力），后段收回（回到待机姿态）。
     * 分成两段而不是让进度直接决定角度，是为了让动作有明确的打击感。
     */
    private static final float SLAM_STRIKE_FRACTION = 0.4f;

    private SlamHandAnimation() {
    }

    /**
     * 由 {@code RenderHandEvent} 调用。
     */
    public static void onRenderHand(PoseStack poseStack, InteractionHand hand, float partialTick) {
        // 只看主手：锤子在主手，副手不受影响
        if (hand != InteractionHand.MAIN_HAND) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        // 只有手持带铁拳附魔的铁砧锤才加动画
        if (!DoomfistEnchantmentUtil.hasDoomfist(player.getMainHandItem())) return;

        float raise = SeismicSlamClientState.raiseProgress(partialTick);
        float slam = SeismicSlamClientState.slamSwingProgress(partialTick);
        // 两个阶段都不活跃 → 不动，交给原版
        if (raise <= 0.0f && slam <= 0.0f) return;

        float rotationX;
        float translateY;
        float translateZ;

        if (slam > 0.0f) {
            // 下砸分两段：先抡到底，再收回到**待机姿态**。
            //
            // 关键在角度空间插值，而不是拿举起进度当系数去乘另一个基准角：
            // 举起与下砸是反号的（+60° / -90°），若按进度混用，落地时会一帧内
            // 从举起角跳到下砸角，看着像瞬移而非挥击。
            float startAngle = RAISE_ROTATION_X * SeismicSlamClientState.slamStartRaise();
            if (slam <= SLAM_STRIKE_FRACTION) {
                // 下抡段：从「起手时举到的角度」抡到最低点
                float t = slam / SLAM_STRIKE_FRACTION;
                rotationX = startAngle + (SLAM_ROTATION_X - startAngle) * t;
                translateY = SLAM_TRANSLATE_Y * t;
                translateZ = SLAM_TRANSLATE_Z * t;
            } else {
                // 收回段：从最低点回到 0°（待机），**不是**回到 startAngle。
                // 回到 startAngle 会让满蓄力一击在砸完后从 -90° 抬回 +60°，
                // 看起来像又播了一次「上抬」——砸一下却抬两次。
                float back = 1.0f - (slam - SLAM_STRIKE_FRACTION) / (1.0f - SLAM_STRIKE_FRACTION);
                rotationX = SLAM_ROTATION_X * back;
                translateY = SLAM_TRANSLATE_Y * back;
                // 下砸时前推让把手可见（详见 SLAM_TRANSLATE_Z）；收回时同步归位
                translateZ = SLAM_TRANSLATE_Z * back;
            }
        } else {
            rotationX = RAISE_ROTATION_X * raise;
            translateY = RAISE_TRANSLATE_Y * raise;
            translateZ = RAISE_TRANSLATE_Z * raise;
        }

        // 位移先在「屏幕方向」上施加：保持你按屏幕调好的前后/上下偏移不变。
        poseStack.translate(0.0f, translateY, translateZ);

        // ------------------------------------------------------------------
        // 视角修正：让挥击朝向固定在世界里，不随抬头/低头漂移
        //
        // 手部空间由 GameRenderer#renderItemInHand 建立：建栈时乘入相机旋转
        //     C = Ry(π − yaw) · Rx(−pitch)        （见 Camera.setRotation 的 rotationYXZ）
        // 动画整体为 C · T · Rx(a)，于是锤子指向的世界方向是
        //     C · Rx(a) · u = Ry(π−yaw) · Rx(−pitch) · Rx(a) · u
        //                   = Ry(π−yaw) · Rx(a − pitch) · u
        // 要让它与 pitch 无关，必须有 a − pitch = 常数，即补偿量为 +pitch。
        //
        // 换句话说：Axis.XP 的轴确实水平（挥击平面是竖直的，这点没错），
        // 但**平面内的起始朝向**会随俯仰一起转 —— 轴对不代表朝向对。
        // 只有俯仰 0°（你的参数就是在这个角度调的）时朝向才符合预期。
        //
        // 位置说明：补偿放在 translate **之后**、动画旋转之前。
        //   · 与旋转相邻，两者同轴可直接合并为 Rx(a + pitch)，只改朝向；
        //   · 位移留在外层，仍按屏幕方向生效，你调好的前后/上下偏移不受影响。
        //
        // 只补 pitch、不含 xBob：xBob 是原版平滑跟随俯仰的随动量
        // （xBob += (getXRot()−xBob)*0.5），对应转视线时的滞后摆动，属原版手感，不该抵消。
        //
        // 俯仰 0° 时补偿量为 0，因此你按 0° 调好的数值完全不变。
        // 若实测方向相反（例如低头时反而抡得更高），把这里的正号改成负号即可。
        // ------------------------------------------------------------------
        float pitch = player.getViewXRot(partialTick);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotationX + pitch));
    }
}
