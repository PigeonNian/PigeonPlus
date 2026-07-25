package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.entity.FeedSpreaderBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
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

public class FeedSpreaderBlockEntityRenderer implements BlockEntityRenderer<FeedSpreaderBlockEntity> {
    private static final ModelResourceLocation BUCKET_MODEL = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/feed_spreader_bucket")
    );
    private static final ModelResourceLocation PISTON_MODEL = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/feed_spreader_piston")
    );
    private static final float MAX_PISTON_DROP = 15.0F / 16.0F;

    @SuppressWarnings("unused")
    public FeedSpreaderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        FeedSpreaderBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        int baseLight = getBaseLight(blockEntity, packedLight);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(720.0F * easeInOut(blockEntity.getBucketRotation(partialTick))));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        renderModel(blockEntity.getBlockState(), BUCKET_MODEL, poseStack, bufferSource, baseLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, -MAX_PISTON_DROP * blockEntity.getPistonPress(partialTick), 0.0F);
        renderModel(blockEntity.getBlockState(), PISTON_MODEL, poseStack, bufferSource, baseLight, packedOverlay);
        poseStack.popPose();
    }

    private static int getBaseLight(FeedSpreaderBlockEntity blockEntity, int fallbackLight) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return fallbackLight;
        }
        return LevelRenderer.getLightColor(level, blockEntity.getBlockPos());
    }

    private static float easeInOut(float progress) {
        return progress * progress * progress * (progress * (progress * 6.0F - 15.0F) + 10.0F);
    }

    private static void renderModel(
        BlockState state,
        ModelResourceLocation modelLocation,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        ModelManager modelManager = minecraft.getModelManager();
        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        BakedModel model = modelManager.getModel(modelLocation);
        RandomSource random = RandomSource.create();
        ModelData modelData = ModelData.EMPTY;
        for (RenderType renderType : model.getRenderTypes(state, random, modelData)) {
            VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
            dispatcher.getModelRenderer().renderModel(
                poseStack.last(),
                vertexConsumer,
                state,
                model,
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay,
                modelData,
                renderType
            );
        }
    }
}
