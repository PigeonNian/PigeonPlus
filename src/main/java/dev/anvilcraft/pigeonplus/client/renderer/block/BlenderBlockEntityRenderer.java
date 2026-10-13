package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.BlenderBlock;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.Map;

public class BlenderBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final float ROTATION_DEGREES_PER_TICK = 45.0f;

    private static final float HEAD_LIFT = 0.875f;

    private static final double RENDER_BOX_REACH = 1.0;
    private static final double RENDER_BOX_HEIGHT = 4.0;

    private static final Map<BlenderBlock.BlenderHead, ModelResourceLocation> HEAD_MODELS = Map.of(
        BlenderBlock.BlenderHead.IRON, headModel("blender_head"),
        BlenderBlock.BlenderHead.BLAZE, headModel("blender_head_blaze")
    );

    private static ModelResourceLocation headModel(String name) {
        return new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/blender/" + name),
            "standalone");
    }

    public BlenderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockState blockState = blockEntity.getBlockState();
        ModelResourceLocation headLocation = HEAD_MODELS.get(blockState.getValue(BlenderBlock.HEAD));
        if (headLocation == null) return;

        BlockPos pos = blockEntity.getBlockPos();
        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        RandomSource random = RandomSource.create();
        ModelData modelData = ModelData.EMPTY;

        BakedModel headModel = modelManager.getModel(headLocation);

        // Check for large cauldron core 2 blocks above
        boolean hasCauldronAbove = isLargeCauldronCore(level, pos.above(2));

        poseStack.pushPose();
        float angle = blockState.getValue(BlenderBlock.WORKING)
            ? (level.getGameTime() + partialTick) * ROTATION_DEGREES_PER_TICK
            : 0.0f;
        poseStack.translate(0.0, HEAD_LIFT, 0.0);
        poseStack.translate(0.5, 0.5, 0.5);
        if (hasCauldronAbove) {
            poseStack.scale(3.0f, 3.0f, 3.0f);
            poseStack.translate(0.0, 0.25, 0.0);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.translate(-0.5, -0.5, -0.5);

        for (RenderType renderType : headModel.getRenderTypes(blockState, random, modelData)) {
            VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
            dispatcher.getModelRenderer().renderModel(
                poseStack.last(), vertexConsumer, blockState, headModel,
                1.0F, 1.0F, 1.0F, packedLight, packedOverlay,
                modelData, renderType
            );
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        return new AABB(blockEntity.getBlockPos())
            .inflate(RENDER_BOX_REACH)
            .expandTowards(0.0, RENDER_BOX_HEIGHT, 0.0);
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
