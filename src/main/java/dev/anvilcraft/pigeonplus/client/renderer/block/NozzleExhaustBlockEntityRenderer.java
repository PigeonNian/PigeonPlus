package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.pigeonplus.block.entity.NozzleExhaustBlockEntity;
import dev.anvilcraft.pigeonplus.client.renderer.NozzleExhaustRenderer;
import dev.anvilcraft.pigeonplus.client.sound.NozzleSoundController;
import dev.anvilcraft.pigeonplus.init.AddonVaporizationSources;
import dev.anvilcraft.pigeonplus.util.NozzleExhaustUtil;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NozzleExhaustBlockEntityRenderer implements BlockEntityRenderer<NozzleExhaustBlockEntity> {
    private static final int PIGEONPLUS_MAX_RENDER_Y = 2048;

    public NozzleExhaustBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        NozzleExhaustBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        LargeCauldronBlockEntity cauldron = NozzleExhaustUtil.getStructuralCauldron(
            blockEntity.getLevel(),
            blockEntity.getBlockPos()
        );
        if (cauldron == null) {
            NozzleSoundController.cleanup();
            return;
        }
        Direction facing = NozzleExhaustUtil.getStructuralFacing(blockEntity.getLevel(), blockEntity.getBlockPos());
        BlockPos outletPos = NozzleExhaustUtil.getStructuralOutletPos(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (facing == null || outletPos == null) {
            NozzleSoundController.cleanup();
            return;
        }
        AddonVaporizationSources.JetPropellant propellant = NozzleExhaustUtil.getJetPropellant(blockEntity.getLevel(), cauldron);
        poseStack.pushPose();
        poseStack.translate(
            outletPos.getX() - blockEntity.getBlockPos().getX(),
            outletPos.getY() - blockEntity.getBlockPos().getY(),
            outletPos.getZ() - blockEntity.getBlockPos().getZ()
        );
        NozzleExhaustRenderer.render(
            poseStack,
            bufferSource,
            blockEntity.getLevel().getGameTime() + partialTick,
            blockEntity.getBlockPos(),
            facing,
            propellant == AddonVaporizationSources.JetPropellant.METHANE
                ? NozzleExhaustRenderer.Propellant.METHANE
                : NozzleExhaustRenderer.Propellant.KEROSENE,
            NozzleExhaustUtil.getVisibleJetRenderLength(
                blockEntity.getLevel(),
                outletPos,
                facing,
                NozzleExhaustUtil.JET_VISUAL_HEIGHT
            )
        );
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(NozzleExhaustBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
    }

    @Override
    public boolean shouldRender(NozzleExhaustBlockEntity blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos())
            .multiply(1.0, 0.0, 1.0)
            .closerThan(cameraPos.multiply(1.0, 0.0, 1.0), this.getViewDistance());
    }

    @Override
    public AABB getRenderBoundingBox(NozzleExhaustBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        if (blockEntity.getLevel() == null) {
            return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, PIGEONPLUS_MAX_RENDER_Y, pos.getZ() + 1.0);
        }
        Direction facing = NozzleExhaustUtil.getStructuralFacing(blockEntity.getLevel(), pos);
        BlockPos outletPos = NozzleExhaustUtil.getStructuralOutletPos(blockEntity.getLevel(), pos);
        if (facing == null || outletPos == null) {
            return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, PIGEONPLUS_MAX_RENDER_Y, pos.getZ() + 1.0);
        }
        return NozzleExhaustUtil.getJetEffectBounds(outletPos, facing, NozzleExhaustUtil.JET_VISUAL_HEIGHT).inflate(2.0);
    }
}
