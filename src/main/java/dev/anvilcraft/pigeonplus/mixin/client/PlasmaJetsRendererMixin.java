package dev.anvilcraft.pigeonplus.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.pigeonplus.client.renderer.NozzlePlasmaJetRenderer;
import dev.anvilcraft.pigeonplus.util.NozzlePlasmaJetUtil;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.PlasmaJetsRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlasmaJetsRenderer.class)
public class PlasmaJetsRendererMixin {
    private static final int PIGEONPLUS_MAX_RENDER_Y = 2048;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$renderNozzleJet(
        PlasmaJetsBlockEntity entity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay,
        CallbackInfo ci
    ) {
        if (entity.getLevel() == null || NozzlePlasmaJetUtil.getStructuralCauldron(entity.getLevel(), entity.getBlockPos()) == null) {
            return;
        }
        ci.cancel();
        NozzlePlasmaJetRenderer.render(
            poseStack,
            bufferSource,
            entity.getLevel().getGameTime() + partialTick,
            entity.getBlockPos(),
            NozzlePlasmaJetRenderer.Propellant.KEROSENE
        );
    }

    public boolean shouldRenderOffScreen() {
        return true;
    }

    public int getViewDistance() {
        return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
    }

    public boolean shouldRender(PlasmaJetsBlockEntity blockEntity, Vec3 cameraPosition) {
        return Vec3.atCenterOf(blockEntity.getBlockPos())
            .multiply(1.0, 0.0, 1.0)
            .closerThan(cameraPosition.multiply(1.0, 0.0, 1.0), this.getViewDistance());
    }

    public AABB getRenderBoundingBox(PlasmaJetsBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, PIGEONPLUS_MAX_RENDER_Y, pos.getZ() + 1.0);
    }
}
