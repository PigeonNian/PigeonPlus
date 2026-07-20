package dev.anvilcraft.pigeonplus.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.pigeonplus.client.renderer.GasTankRenderContext;
import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import dev.dubhe.anvilcraft.client.renderer.FluidTankRenderUtil;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidTankRenderUtil.class)
public class FluidTankRenderUtilMixin {
    @Inject(method = "drawFluidInTank", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$renderGasAsFullTank(
        PoseStack ps,
        MultiBufferSource mbs,
        int light,
        FluidStack fluid,
        float fill,
        CallbackInfo ci
    ) {
        if (!(fluid.getFluid() instanceof GasFluid)) {
            return;
        }
        float tankWall = FluidTankRenderUtil.TANK_W;
        float clampedFill = Mth.clamp(fill, 0.0f, 1.0f);
        GasTankRenderContext.renderWithFill(clampedFill, () -> FluidRenderHelper.INSTANCE.renderFluidBox(
            fluid,
            tankWall,
            tankWall,
            tankWall,
            1.0f - tankWall,
            1.0f - tankWall,
            1.0f - tankWall,
            mbs,
            ps,
            light,
            true,
            false
        ));
        ci.cancel();
    }
}
