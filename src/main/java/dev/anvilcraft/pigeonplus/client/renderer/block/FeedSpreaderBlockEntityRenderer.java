package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.block.entity.FeedSpreaderBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

/**
 * 喂食器的旋转桶与下压活塞。
 *
 * <p>26.1 三段式重写；光照不再用外部传入的 {@code packedLight}，
 * 而是在提取阶段用 {@link LevelRenderer#getLightColor} 从方块位置算出，
 * 存进渲染状态供绘制阶段使用。
 */
public class FeedSpreaderBlockEntityRenderer
    implements BlockEntityRenderer<FeedSpreaderBlockEntity, FeedSpreaderBlockEntityRenderer.State> {

    /** 桶模型。需在 {@code ModelEvent.RegisterStandalone} 中注册。 */
    public static final StandaloneModelKey<BlockStateModel> BUCKET_MODEL =
        StandaloneBlockModels.key("block/feed_spreader_bucket");
    /** 活塞模型。需在 {@code ModelEvent.RegisterStandalone} 中注册。 */
    public static final StandaloneModelKey<BlockStateModel> PISTON_MODEL =
        StandaloneBlockModels.key("block/feed_spreader_piston");

    private static final float MAX_PISTON_DROP = 15.0F / 16.0F;

    @SuppressWarnings("unused")
    public FeedSpreaderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        FeedSpreaderBlockEntity blockEntity,
        State state,
        float partialTick,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        state.valid = true;
        // 方块位置的真实光照；基类给的是实体自身的光照，这里要以方块为准
        state.blockLight = LevelRenderer.getLightCoords(level, blockEntity.getBlockPos());
        state.bucketRotation = 720.0F * pigeonplus$easeInOut(blockEntity.getBucketRotation(partialTick));
        state.pistonDrop = MAX_PISTON_DROP * blockEntity.getPistonPress(partialTick);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.valid) {
            return;
        }
        // 桶：绕方块中心自转
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.bucketRotation));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        StandaloneBlockModels.submit(
            poseStack, collector, BUCKET_MODEL, state.blockLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        // 活塞：整体下压
        poseStack.pushPose();
        poseStack.translate(0.0F, -state.pistonDrop, 0.0F);
        StandaloneBlockModels.submit(
            poseStack, collector, PISTON_MODEL, state.blockLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    /** 五次平滑插值（起止都平缓），用于桶的旋转加減速。 */
    private static float pigeonplus$easeInOut(float progress) {
        return progress * progress * progress * (progress * (progress * 6.0F - 15.0F) + 10.0F);
    }

    /** 渲染状态。 */
    public static class State extends BlockEntityRenderState {
        boolean valid;
        int blockLight;
        float bucketRotation;
        float pistonDrop;
    }
}
