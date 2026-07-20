package dev.anvilcraft.pigeonplus.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.pigeonplus.client.renderer.GasTankRenderContext;
import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LargeFluidTankBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LargeFluidTankBlockEntityRenderer.class)
public class LargeFluidTankBlockEntityRendererMixin {
    private static final float TANK_W = 4 / 16.0f + 0.001f;

    @Inject(method = "drawFluidInTank", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$renderGasAsFullTank(
        PoseStack ps,
        MultiBufferSource mbs,
        int light,
        FluidStack fluid,
        double layerBottom,
        double layerTop,
        CallbackInfo ci
    ) {
        if (!(fluid.getFluid() instanceof GasFluid)) {
            return;
        }
        float fill = Mth.clamp((float) (layerTop - layerBottom), 0.0f, 1.0f);
        GasTankRenderContext.renderWithFill(fill, () -> FluidRenderHelper.INSTANCE.renderFluidBox(
            fluid,
            TANK_W - 1.0f,
            TANK_W - 1.0f,
            TANK_W - 1.0f,
            2.0f - TANK_W,
            2.0f - TANK_W,
            2.0f - TANK_W,
            mbs,
            ps,
            light,
            true,
            false
        ));
        ci.cancel();
    }
}
