package dev.anvilcraft.pigeonplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.pigeonplus.client.SeismicSlamClientState;
import dev.anvilcraft.pigeonplus.util.AirborneUtil;
import dev.anvilcraft.pigeonplus.util.DoomfistEnchantmentUtil;
import dev.anvilcraft.pigeonplus.util.SkillCooldowns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 指向性裂地重拳的地面指示器。
 *
 * <p>悬空且技能可用时，在准星所指的地面上画一个扇形轮廓，
 * 形状与伤害判定完全一致（半径 {@value #RADIUS} 格、张角 2×{@value #HALF_ANGLE}°），
 * 让玩家在飞出去之前就能看清会打到哪。
 *
 * <p>用扇形而不是一个圆圈：这个技能的伤害是扇形，指示器若画成圆，
 * 玩家会以为身后也能打到，与实际判定不符。
 */
public final class SlamIndicatorRenderer {
    /** 与 {@code SlamManager.SECTOR_RADIUS} 保持一致。 */
    private static final double RADIUS = 7.5;
    /** 与 {@code SlamManager.SECTOR_HALF_ANGLE} 保持一致（度）。 */
    private static final double HALF_ANGLE = 30.0;
    /** 弧线分段数，越大越圆滑。 */
    private static final int ARC_SEGMENTS = 24;

    /** 可用时的颜色（深蓝色）。 */
    private static final float[] COLOR_READY = {0.09f, 0.19f, 0.62f, 0.95f};

    /** 指示器抬离地面一点，避免与地面 z-fighting。 */
    private static final double Y_OFFSET = 0.03;

    private SlamIndicatorRenderer() {
    }

    /**
     * 由 {@code RenderLevelStageEvent} 调用。
     *
     * @param poseStack     事件提供的位姿栈
     * @param modelView     事件提供的 modelView 矩阵
     * @param cameraPos     相机世界坐标
     */
    public static void render(PoseStack poseStack, Matrix4f modelView, Vec3 cameraPos) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) return;
        if (minecraft.options.hideGui) return;

        // 只在悬空、手持铁拳、技能可用时提示
        if (!DoomfistEnchantmentUtil.isWieldingDoomfist(player)) return;
        if (SkillCooldowns.isOnCooldownClient(SkillCooldowns.Skill.SEISMIC_SLAM)) return;
        if (SeismicSlamClientState.isAirborne()) return;
        if (!AirborneUtil.isAirborne(player)) return;

        Vec3 landing = findLanding(minecraft, player);
        if (landing == null) return;

        // 朝准星的**水平**方向画扇形：伤害判定用的就是水平朝向
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0, look.z);
        if (forward.lengthSqr() < 1.0E-6) return;
        forward = forward.normalize();

        poseStack.pushPose();
        // 关键：把事件给的 modelView 矩阵并入栈。AFTER_LEVEL 阶段位姿栈还是
        // 「世界空间」，不并入这个矩阵，我们算出的相机相对坐标就会落在错误的位置
        // （表现为完全看不到，或飘在天上/地下）。
        poseStack.last().pose().mul(modelView);
        // 世界坐标 → 相机相对坐标（与项目里既有的世界渲染器一致）
        poseStack.translate(landing.x - cameraPos.x, landing.y + Y_OFFSET - cameraPos.y, landing.z - cameraPos.z);
        Matrix4f matrix = poseStack.last().pose();

        VertexConsumer consumer = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        // 扇形轮廓 = 两条半径 + 一段弧，围成闭合形状。
        // 不画中心那条线：它指的方向已经由扇形张角表达，多一条反而显得杂乱。
        Vec3 leftEdge = rotateY(forward, Math.toRadians(-HALF_ANGLE));
        Vec3 rightEdge = rotateY(forward, Math.toRadians(HALF_ANGLE));
        emitLine(consumer, matrix, 0.0, 0.0, leftEdge.x * RADIUS, leftEdge.z * RADIUS);
        emitLine(consumer, matrix, 0.0, 0.0, rightEdge.x * RADIUS, rightEdge.z * RADIUS);

        Vec3 prev = null;
        for (int i = 0; i <= ARC_SEGMENTS; i++) {
            double angle = Math.toRadians(-HALF_ANGLE + (2.0 * HALF_ANGLE) * i / ARC_SEGMENTS);
            Vec3 point = rotateY(forward, angle).scale(RADIUS);
            if (prev != null) {
                emitLine(consumer, matrix, prev.x, prev.z, point.x, point.z);
            }
            prev = point;
        }

        minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
        poseStack.popPose();
    }

    /**
     * 沿视线找地面落点，规则与 {@code SlamManager#findLandingSpot} 一致。
     */
    private static Vec3 findLanding( Minecraft minecraft, LocalPlayer player) {
        BlockHitResult hit = (BlockHitResult) player.pick(32.0, 0.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) return null;

        Level level = minecraft.level;
        BlockPos hitPos = hit.getBlockPos();
        BlockPos landingPos = hit.getDirection() == Direction.UP
            ? hitPos.above()
            : hitPos.relative(hit.getDirection());

        if (!Block.canSupportRigidBlock(level, landingPos.below())) return null;
        if (!level.getBlockState(landingPos).getCollisionShape(level, landingPos).isEmpty()) return null;

        return new Vec3(landingPos.getX() + 0.5, landingPos.getY(), landingPos.getZ() + 0.5);
    }

    private static void emitLine(VertexConsumer consumer, Matrix4f matrix, double x1, double z1, double x2, double z2) {
        consumer.addVertex(matrix, (float) x1, 0.0f, (float) z1)
            .setColor(COLOR_READY[0], COLOR_READY[1], COLOR_READY[2], COLOR_READY[3])
            .setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(matrix, (float) x2, 0.0f, (float) z2)
            .setColor(COLOR_READY[0], COLOR_READY[1], COLOR_READY[2], COLOR_READY[3])
            .setNormal(0.0f, 1.0f, 0.0f);
    }

    /**
     * 绕 Y 轴旋转水平向量。与 {@code SlamManager} 用同一套公式，
     * 避免指示器与伤害判定方向不一致。
     */
    private static Vec3 rotateY(Vec3 vec, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(vec.x * cos + vec.z * sin, 0.0, -vec.x * sin + vec.z * cos);
    }
}
