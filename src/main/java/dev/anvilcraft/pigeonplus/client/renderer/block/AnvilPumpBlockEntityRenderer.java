package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.AnvilPumpBlock;
import dev.anvilcraft.pigeonplus.block.entity.AnvilPumpBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class AnvilPumpBlockEntityRenderer implements BlockEntityRenderer<AnvilPumpBlockEntity> {
    private static final ModelResourceLocation PISTON = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/anvil_pump_pistion")
    );
    private static final float MAX_PISTON_DROP = 8.0F / 16.0F;

    @SuppressWarnings("unused")
    public AnvilPumpBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        AnvilPumpBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof AnvilPumpBlock) || !state.hasProperty(AnvilPumpBlock.FACING)) {
            return;
        }
        Direction facing = state.getValue(AnvilPumpBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-AnvilPumpBlock.getYRotation(facing)));
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.translate(0.0F, -MAX_PISTON_DROP * blockEntity.getPistonPress(partialTick), 0.0F);

        BakedModel piston = Minecraft.getInstance().getModelManager().getModel(PISTON);
        Minecraft.getInstance()
            .getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                poseStack.last(),
                buffer.getBuffer(RenderType.cutout()),
                null,
                piston,
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay
            );
        poseStack.popPose();
    }
}
