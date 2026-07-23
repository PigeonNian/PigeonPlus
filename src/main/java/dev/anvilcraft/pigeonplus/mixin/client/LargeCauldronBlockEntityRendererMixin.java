package dev.anvilcraft.pigeonplus.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.pigeonplus.init.AddonVaporizationSources;
import dev.anvilcraft.pigeonplus.client.renderer.GasContainerRenderUtil;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LargeCauldronBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(LargeCauldronBlockEntityRenderer.class)
    public class LargeCauldronBlockEntityRendererMixin {
    @Unique
    private static final float PIGEONPLUS_WALL = 0.25F + 0.001F;
    @Unique
    private static final float PIGEONPLUS_MIN_XZ = -1.0F + PIGEONPLUS_WALL;
    @Unique
    private static final float PIGEONPLUS_MAX_XZ = 2.0F - PIGEONPLUS_WALL;
    @Unique
    private static final float PIGEONPLUS_MIN_Y = -0.5F + 0.001F;
    @Unique
    private static final float PIGEONPLUS_MAX_Y = 1.75F - 0.001F;

    @Inject(method = "drawFluids", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$renderGasAboveLiquid(
        LargeCauldronFluidHandler handler,
        PoseStack pose,
        MultiBufferSource buffers,
        int light,
        CallbackInfo ci
    ) {
        List<FluidStack> layers = new ArrayList<>();
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluid = handler.getFluidInTank(tank);
            if (!fluid.isEmpty()) {
                layers.add(fluid);
            }
        }
        if (!GasContainerRenderUtil.hasGas(layers)) {
            return;
        }
        GasContainerRenderUtil.renderLayeredFluidBox(
            layers,
            (double) LargeCauldronFluidHandler.TANK_COUNT * LargeCauldronFluidHandler.TANK_CAPACITY,
            PIGEONPLUS_MIN_XZ,
            PIGEONPLUS_MIN_Y,
            PIGEONPLUS_MIN_XZ,
            PIGEONPLUS_MAX_XZ,
            PIGEONPLUS_MAX_Y,
            PIGEONPLUS_MAX_XZ,
            buffers,
            pose,
            light
        );
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
        ci.cancel();
    }

    @Shadow
    private void drawFire(PoseStack pose, MultiBufferSource buffers, int overlay, float surfaceY) {
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Ldev/dubhe/anvilcraft/client/renderer/blockentity/LargeCauldronBlockEntityRenderer;drawFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V"
        )
    )
    private void pigeonplus$hideIgnitedFireTexture(
        LargeCauldronBlockEntityRenderer instance,
        PoseStack pose,
        MultiBufferSource buffers,
        int overlay,
        float surfaceY,
        LargeCauldronBlockEntity cauldron,
        float partialTick,
        PoseStack renderPose,
        MultiBufferSource renderBuffers,
        int light,
        int renderOverlay
    ) {
        if (AddonVaporizationSources.hasAnyPropellant(cauldron)) {
            return;
        }
        this.drawFire(pose, buffers, overlay, surfaceY);
    }
}
