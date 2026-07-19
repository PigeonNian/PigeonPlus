package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.entity.BlenderBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class BlenderBlockEntityRenderer implements BlockEntityRenderer<BlenderBlockEntity> {
    private static final ModelResourceLocation TOP_MODEL = new ModelResourceLocation(
        ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/blender_top"),
        "standalone");

    public BlenderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BlenderBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockState blockState = blockEntity.getBlockState();
        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        RandomSource random = RandomSource.create();
        ModelData modelData = ModelData.EMPTY;

        // Render top (rotating)
        BakedModel topModel = modelManager.getModel(TOP_MODEL);
        poseStack.pushPose();
        float angle = (level.getGameTime() + partialTick) * 3.0f;
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.translate(-0.5, 0.0, -0.5);
        for (RenderType renderType : topModel.getRenderTypes(blockState, random, modelData)) {
            VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
            dispatcher.getModelRenderer().renderModel(
                poseStack.last(), vertexConsumer, blockState, topModel,
                1.0F, 1.0F, 1.0F, packedLight, packedOverlay,
                modelData, renderType
            );
        }
        poseStack.popPose();
    }
}
