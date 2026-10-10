package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.pigeonplus.block.entity.LargeFireworksRocketBlockEntity;
import dev.anvilcraft.pigeonplus.entity.LargeFireworksRocketEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LargeFireworksRocketBlockEntityRenderer
    implements BlockEntityRenderer<LargeFireworksRocketBlockEntity, LargeFireworksRocketBlockEntityRenderer.State> {

    private static final float PEARL_SCALE = LargeFireworksRocketEntity.PEARL_RENDER_SCALE;

    private final ItemModelResolver itemModelResolver;

    public LargeFireworksRocketBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        LargeFireworksRocketBlockEntity blockEntity,
        State state,
        float partialTick,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        state.present = blockEntity.getLevel() != null && blockEntity.getPearlCount() > 0;
        if (!state.present) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        Level level = blockEntity.getLevel();
        BlockPos pearlPos = BlockPos.containing(
            pos.getX() + 0.5,
            pos.getY() + LargeFireworksRocketEntity.PEARL_RENDER_HEIGHT,
            pos.getZ() + 0.5
        );
        state.pearlLight = level == null
            ? LightCoordsUtil.FULL_BRIGHT
            : net.minecraft.client.renderer.LevelRenderer.getLightCoords(level, pearlPos);
        this.itemModelResolver.updateForTopItem(
            state.pearl, new ItemStack(Items.ENDER_PEARL), ItemDisplayContext.GROUND, level, null, (int) pos.asLong());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.present) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, LargeFireworksRocketEntity.PEARL_RENDER_HEIGHT, 0.5);
        poseStack.scale(PEARL_SCALE, PEARL_SCALE, PEARL_SCALE);
        state.pearl.submit(
            poseStack, collector, state.pearlLight, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(LargeFireworksRocketBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        double top = pos.getY() + LargeFireworksRocketEntity.PEARL_RENDER_HEIGHT + 1.0;
        return new AABB(
            pos.getX() - 1.0, pos.getY(), pos.getZ() - 1.0,
            pos.getX() + 2.0, top, pos.getZ() + 2.0
        );
    }

    public static class State extends BlockEntityRenderState {
        public boolean present;
        public int pearlLight = LightCoordsUtil.FULL_BRIGHT;
        public final ItemStackRenderState pearl = new ItemStackRenderState();
    }
}
