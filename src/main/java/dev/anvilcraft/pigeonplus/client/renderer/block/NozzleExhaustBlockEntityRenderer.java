package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.pigeonplus.block.entity.NozzleExhaustBlockEntity;
import dev.anvilcraft.pigeonplus.client.renderer.NozzleExhaustRenderer;
import dev.anvilcraft.pigeonplus.client.sound.NozzleSoundController;
import dev.anvilcraft.pigeonplus.util.NozzleExhaustUtil;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 喷口尾焰。
 *
 * <p>26.1 改为三段式。注意本类有两点与原版 BER 不同：
 * <ul>
 *   <li>音效状态机（{@link NozzleSoundController}）仍在 <b>提取阶段</b>推进——
 *       它需要每帧看到结构是否成立，而绘制阶段可能因「不渲染」被跳过。</li>
 *   <li>几何体通过 {@link SubmitNodeCollector#submitCustomGeometry} 提交，
 *       渲染层与缓冲在回调里提供（26.1 不再有外部传入的 {@code MultiBufferSource}）。</li>
 * </ul>
 */
public class NozzleExhaustBlockEntityRenderer
    implements BlockEntityRenderer<NozzleExhaustBlockEntity, NozzleExhaustBlockEntityRenderer.State> {

    private static final int PIGEONPLUS_MAX_RENDER_Y = 2048;

    public NozzleExhaustBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        NozzleExhaustBlockEntity blockEntity,
        State state,
        float partialTick,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        state.valid = false;
        if (blockEntity.getLevel() == null) {
            return;
        }
        LargeCauldronBlockEntity cauldron = NozzleExhaustUtil.getStructuralCauldron(
            blockEntity.getLevel(),
            blockEntity.getBlockPos()
        );
        if (cauldron == null) {
            NozzleSoundController.beginShutdown(blockEntity.getBlockPos());
            return;
        }
        Direction facing = NozzleExhaustUtil.getStructuralFacing(blockEntity.getLevel(), blockEntity.getBlockPos());
        BlockPos outletPos = NozzleExhaustUtil.getStructuralOutletPos(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (facing == null || outletPos == null) {
            NozzleSoundController.beginShutdown(blockEntity.getBlockPos());
            return;
        }
        boolean active = NozzleExhaustUtil.isNozzleActive(blockEntity.getLevel(), blockEntity.getBlockPos())
            && blockEntity.getExhaustPhase() != NozzleExhaustBlockEntity.ExhaustPhase.IDLE;
        if (active) {
            NozzleSoundController.registerActive(blockEntity.getBlockPos());
        } else {
            NozzleSoundController.beginShutdown(blockEntity.getBlockPos());
        }
        float flameProgress = NozzleSoundController.getFlameProgress(blockEntity.getBlockPos());
        if (flameProgress <= 0.0F) {
            return;
        }
        state.valid = true;
        state.outletOffset = new Vec3(
            outletPos.getX() - blockEntity.getBlockPos().getX(),
            outletPos.getY() - blockEntity.getBlockPos().getY(),
            outletPos.getZ() - blockEntity.getBlockPos().getZ()
        );
        state.facing = facing;
        state.time = blockEntity.getLevel().getGameTime() + partialTick;
        state.propellant = switch (blockEntity.getActivePropellant()) {
            case METHANE -> NozzleExhaustRenderer.Propellant.METHANE;
            case HYDROGEN -> NozzleExhaustRenderer.Propellant.HYDROGEN;
            default -> NozzleExhaustRenderer.Propellant.KEROSENE;
        };
        state.visibleLength = NozzleExhaustUtil.getVisibleJetRenderLength(
            blockEntity.getLevel(),
            outletPos,
            facing,
            NozzleExhaustUtil.JET_VISUAL_HEIGHT
        );
        state.flameProgress = flameProgress;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.valid) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(state.outletOffset.x, state.outletOffset.y, state.outletOffset.z);
        // 尾焰用自发光信标束渲染层；缓冲与位姿由回调提供
        collector.submitCustomGeometry(
            poseStack,
            RenderTypes.beaconBeam(NozzleExhaustRenderer.BEAM_TEXTURE, true),
            (pose, buffer) -> NozzleExhaustRenderer.render(
                pose,
                buffer,
                state.time,
                state.facing,
                state.propellant,
                state.visibleLength,
                state.flameProgress
            )
        );
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
    }

    @Override
    public boolean shouldRender(NozzleExhaustBlockEntity blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos())
            .multiply(1.0, 0.0, 1.0)
            .closerThan(cameraPos.multiply(1.0, 0.0, 1.0), this.getViewDistance());
    }

    @Override
    public AABB getRenderBoundingBox(NozzleExhaustBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        AABB fallback = new AABB(
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1.0, PIGEONPLUS_MAX_RENDER_Y, pos.getZ() + 1.0
        );
        if (blockEntity.getLevel() == null) {
            return fallback;
        }
        Direction facing = NozzleExhaustUtil.getStructuralFacing(blockEntity.getLevel(), pos);
        BlockPos outletPos = NozzleExhaustUtil.getStructuralOutletPos(blockEntity.getLevel(), pos);
        if (facing == null || outletPos == null) {
            return fallback;
        }
        return NozzleExhaustUtil.getJetEffectBounds(outletPos, facing, NozzleExhaustUtil.JET_VISUAL_HEIGHT).inflate(2.0);
    }

    /** 渲染状态：提取阶段算好的绘制参数。 */
    public static class State extends BlockEntityRenderState {
        boolean valid;
        Vec3 outletOffset = Vec3.ZERO;
        Direction facing = Direction.UP;
        float time;
        NozzleExhaustRenderer.Propellant propellant = NozzleExhaustRenderer.Propellant.KEROSENE;
        float visibleLength;
        float flameProgress;
    }
}
