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
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
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

        BlockPos pos = blockEntity.getBlockPos();
        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        RandomSource random = RandomSource.create();
        ModelData modelData = ModelData.EMPTY;

        BakedModel topModel = modelManager.getModel(TOP_MODEL);

        // Check for large cauldron core 2 blocks above
        boolean hasCauldronAbove = isLargeCauldronCore(level, pos.above(2));

        poseStack.pushPose();
        float angle = (level.getGameTime() + partialTick) * 3.0f;
        poseStack.translate(0.5, 0.5, 0.5);
        if (hasCauldronAbove) {
            poseStack.scale(2.0f, 2.0f, 2.0f);
            poseStack.translate(0.0, -0.2, 0.0);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.translate(-0.5, -0.5, -0.5);

        BlockState blockState = blockEntity.getBlockState();
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

    private static boolean isLargeCauldronCore(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        ResourceLocation id = state.getBlock().builtInRegistryHolder().key().location();
        if (!id.equals(ResourceLocation.fromNamespaceAndPath("anvilcraft", "large_cauldron"))) {
            return false;
        }
        Property<?> halfProp = state.getBlock().getStateDefinition().getProperty("half");
        if (halfProp == null) return false;
        return "mid_center".equals(state.getValue(halfProp).toString());
    }
}
