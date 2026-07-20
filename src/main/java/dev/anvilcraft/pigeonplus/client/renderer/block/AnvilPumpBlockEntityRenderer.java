package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.entity.AnvilPumpBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import dev.dubhe.anvilcraft.block.state.Orientation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class AnvilPumpBlockEntityRenderer implements BlockEntityRenderer<AnvilPumpBlockEntity> {
    private static final ModelResourceLocation PISTON = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/anvil_pump_pistion")
    );

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
        if (!(state.getBlock() instanceof PumpBlock) || !state.hasProperty(PumpBlock.ORIENTATION)) {
            return;
        }
        Orientation orientation = state.getValue(PumpBlock.ORIENTATION);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-orientation.getYRotation()));
        poseStack.mulPose(Axis.XP.rotationDegrees(orientation.getXRotation()));
        poseStack.translate(-0.5, -0.5, -0.5);

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
