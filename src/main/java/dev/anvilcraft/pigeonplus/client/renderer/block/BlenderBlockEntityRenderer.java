package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.anvilcraft.pigeonplus.block.entity.BlenderBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

/**
 * 搅拌机的旋转顶部。
 *
 * <p>26.1 改为 {@code createRenderState} / {@code extractRenderState} / {@code submit}
 * 三段式；本类据此重写，并且渲染独立模型改用 {@link StandaloneBlockModels}。
 */
public class BlenderBlockEntityRenderer
    implements BlockEntityRenderer<BlenderBlockEntity, BlenderBlockEntityRenderer.State> {

    private static final float ROTATION_DEGREES_PER_TICK = 45.0f;

    /** 顶部旋转部件模型。需在 {@code ModelEvent.RegisterStandalone} 中注册。 */
    public static final StandaloneModelKey<BlockStateModel> TOP_MODEL =
        StandaloneBlockModels.key("block/blender_top");

    public BlenderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        BlenderBlockEntity blockEntity,
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
        BlockState blockState = blockEntity.getBlockState();
        state.angle = blockState.getValue(BlenderBlock.WORKING)
            ? (level.getGameTime() + partialTick) * ROTATION_DEGREES_PER_TICK
            : 0.0f;
        // 上方两格是大型炼药锅核心时整体放大并下移，与锅体衔接
        state.scaleUp = pigeonplus$isLargeCauldronCore(level, blockEntity.getBlockPos().above(2));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.valid) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        if (state.scaleUp) {
            poseStack.scale(2.0f, 2.0f, 2.0f);
            poseStack.translate(0.0, -0.2, 0.0);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(state.angle));
        poseStack.translate(-0.5, -0.5, -0.5);

        StandaloneBlockModels.submit(
            poseStack, collector, TOP_MODEL, state.lightCoords, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    /**
     * 判断某位置是否是大型炼药锅的「中中心」部件。
     *
     * <p>用属性名与字符串值比对（而不是引用 AnvilCraft 的枚举常量）：
     * 大炼药锅在 26.1 里被挪了包，直接依赖枚举会在其再次移动时连带崩编译。
     */
    private static boolean pigeonplus$isLargeCauldronCore(Level level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        Identifier id = blockState.getBlock().builtInRegistryHolder().key().location();
        if (!id.equals(Identifier.fromNamespaceAndPath("anvilcraft", "large_cauldron"))) {
            return false;
        }
        Property<?> halfProperty = blockState.getBlock().getStateDefinition().getProperty("half");
        if (halfProperty == null) {
            return false;
        }
        return "mid_center".equals(blockState.getValue(halfProperty).toString());
    }

    /** 渲染状态。 */
    public static class State extends BlockEntityRenderState {
        boolean valid;
        float angle;
        boolean scaleUp;
    }
}
