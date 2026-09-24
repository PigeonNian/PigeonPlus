package dev.anvilcraft.pigeonplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;

/**
 * 第一人称手部动画的视角修正，由三个技能的动画共用。
 *
 * <h2>视角修正</h2>
 * 手部动画所处的空间是「相机对齐」的：{@code GameRenderer#renderItemInHand} 建栈时
 * 乘入了相机旋转的逆矩阵。因此动画里绕 X 轴的旋转，其朝向会随玩家抬头/低头一起转，
 * 同一套角度参数只有俯仰 0°（参数通常就是在这个角度下调的）时才符合预期。
 *
 * <p>推导：手部空间由 {@code C = Ry(π − yaw) · Rx(−pitch)} 建立，
 * 在动画旋转之前先补一个 {@code Rx(pitch)}，二者同轴相邻直接相消，
 * 俯仰角被精确消掉。俯仰 0° 时补偿量为 0，因此按 0° 调好的数值不受影响。
 *
 * <h2>枢轴补偿（仅上勾拳使用）</h2>
 * 「绕哪个点转」是另一件事。原版在触发 {@code RenderHandEvent} 时，
 * 位姿栈里<strong>还没有</strong>手部基准位移——那次平移是事件<strong>之后</strong>才由
 * {@code ItemInHandRenderer#applyItemArmTransform} 施加的：
 * <pre>
 *   translate(arm == RIGHT ? +0.56 : -0.56, -0.52 - equipProgress * 0.6, -0.72)
 * </pre>
 * 所以事件里的坐标原点是<strong>相机</strong>（≈ 脸部中心），不是手。
 * 直接 {@code mulPose} 会让手绕相机原点转，大幅甩动，看起来像「以锤头为轴翻转」。
 * 见 {@link #applyWithArmPivot}。
 *
 * <p><strong>注意</strong>：枢轴补偿会改变旋转中心，因此会让已经调好的角度观感全变。
 * 裂地重拳与火箭重拳的数值是在旧枢轴（相机原点）下调定的，故<strong>不</strong>使用它；
 * 只有上勾拳用它。
 */
public final class HandViewCorrection {
    /** 手部基准位（原版 {@code applyItemArmTransform} 的常量）。 */
    private static final float ARM_X = 0.56f;
    private static final float ARM_Y = -0.52f;
    private static final float ARM_Z = -0.72f;
    /** 装备进度对 Y 的系数（原版同样是 -0.6）。 */
    private static final float EQUIP_Y = -0.6f;

    private HandViewCorrection() {
    }

    /**
     * 施加俯仰补偿，并返回绕 X 轴的动画角。
     *
     * <p>调用方随后自行 {@code mulPose(Axis.XP.rotationDegrees(...))}。
     * 这样保持「补偿」与「动画」两步分开，与裂地重拳、火箭重拳原本的写法一致
     * ——让已经调好的角度数值继续保持原有效果。
     *
     * <p><strong>调用位置</strong>：必须在 {@code translate} 之后、所有动画旋转之前。
     * 位移留在外层才能保持「屏幕方向」，按屏幕调好的前后/上下偏移不受影响。
     */
    public static void apply(PoseStack poseStack, LocalPlayer player, float partialTick) {
        poseStack.mulPose(Axis.XP.rotationDegrees(player.getViewXRot(partialTick)));
    }

    /**
     * 绕<strong>手部枢轴</strong>施加动画旋转，并同时完成俯仰补偿。
     *
     * <p>等价于在「原版把手放到握把位置」之后再做旋转，因此旋转中心是握把而非相机原点。
     *
     * <p>推导：原版会在本事件之后追加 {@code T(p)}，实际链是「本方法插入的 M」再乘 {@code T(p)}。
     * 目标是 {@code C · Rx(pitch) · T(p) · R_anim}，则
     * <pre>
     *   M · T(p) = Rx(pitch) · T(p) · R_anim
     *   =>  M = Rx(pitch) · T(p) · R_anim · T(p)⁻¹
     * </pre>
     * 也就是俯仰补偿要在 {@code T(p)} <strong>之外</strong>。若写进里面
     * （{@code T·Rx(pitch)·R·T⁻¹}），俯仰会变成绕手部原点的旋转、视角修正随之失效。
     *
     * @param rotationX     绕 X 的角度（度），即俯仰 0° 时的目标角度
     * @param equipProgress 装备进度，取自 {@code RenderHandEvent#getEquipProgress()}。
     *                      切手时会短暂非零，取真实值才能与随后的原版平移精确对齐
     * @param pivotDropY    枢轴相对手部基准位<strong>向下</strong>的额外偏移（格）。
     *                      正值 = 枢轴下移，锤子绕更靠下的点转（像握着锤柄更下端）。
     *                      上勾拳想让旋转中心更靠下时用它，其它技能传 0
     */
    public static void applyWithArmPivot(
        PoseStack poseStack,
        LocalPlayer player,
        float partialTick,
        float rotationX,
        float equipProgress,
        float pivotDropY
    ) {
        // 左右手在 X 上镜像，必须按主手取符号，否则左手持锤时枢轴会算到另一侧
        float px = player.getMainArm() == HumanoidArm.RIGHT ? ARM_X : -ARM_X;
        // 向下 = Y 减小，故减去偏移
        float py = ARM_Y + equipProgress * EQUIP_Y - pivotDropY;

        // 1) 俯仰补偿：放在最外层（紧贴相机旋转）才能精确相消
        poseStack.mulPose(Axis.XP.rotationDegrees(player.getViewXRot(partialTick)));
        // 2) 进入手部枢轴坐标系
        poseStack.translate(px, py, ARM_Z);
        // 3) 动画本体：此时原点就是握把
        poseStack.mulPose(Axis.XP.rotationDegrees(rotationX));
        // 4) 退出，抵消第 2 步；随后原版的 T(p) 让整体回到预期链
        poseStack.translate(-px, -py, -ARM_Z);
    }
}
