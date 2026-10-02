package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.block.AnvilPumpBlock;
import dev.anvilcraft.pigeonplus.block.entity.AnvilPumpBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

/**
 * 铁砧泵的活塞部分。
 *
 * <p>26.1 的方块实体渲染器改为三段式（{@code createRenderState} /
 * {@code extractRenderState} / {@code submit}），本类按新结构重写：
 * 提取阶段只做「读数据 + 算插值」，绘制阶段才碰 {@code PoseStack} 与提交器。
 */
public class AnvilPumpBlockEntityRenderer
    implements BlockEntityRenderer<AnvilPumpBlockEntity, AnvilPumpBlockEntityRenderer.State> {

    /** 活塞模型。需在 {@code ModelEvent.RegisterStandalone} 中注册。 */
    public static final StandaloneModelKey<BlockStateModel> PISTON =
        StandaloneBlockModels.key("block/anvil_pump_pistion");

    private static final float MAX_PISTON_DROP = 8.0F / 16.0F;

    @SuppressWarnings("unused")
    public AnvilPumpBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        AnvilPumpBlockEntity blockEntity,
        State state,
        float partialTick,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        BlockState blockState = blockEntity.getBlockState();
        state.valid = blockState.getBlock() instanceof AnvilPumpBlock
            && blockState.hasProperty(AnvilPumpBlock.FACING);
        if (!state.valid) {
            return;
        }
        state.facing = blockState.getValue(AnvilPumpBlock.FACING);
        state.pistonDrop = MAX_PISTON_DROP * blockEntity.getPistonPress(partialTick);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.valid) {
            return;
        }
        poseStack.pushPose();
        // 绕方块中心按朝向旋转，再做活塞下压
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-AnvilPumpBlock.getYRotation(state.facing)));
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.translate(0.0F, -state.pistonDrop, 0.0F);

        StandaloneBlockModels.submit(
            poseStack, collector, PISTON, state.lightCoords, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    /**
     * 渲染状态：只保存绘制所需的、已插值好的数据。
     *
     * <p>{@code lightCoords} / {@code breakProgress} 由基类
     * {@code BlockEntityRenderer#extractRenderState} 的默认实现填好，
     * 这里只需补自己的业务字段。
     */
    public static class State extends BlockEntityRenderState {
        boolean valid;
        Direction facing = Direction.NORTH;
        float pistonDrop;
    }
}
