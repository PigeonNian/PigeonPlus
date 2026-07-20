package dev.anvilcraft.pigeonplus.mixin.client;

import dev.anvilcraft.pigeonplus.client.renderer.GasTankRenderContext;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FluidRenderHelper.class)
public class FluidRenderHelperMixin {
    @Redirect(
        method = "renderFluidBox(Lnet/neoforged/neoforge/fluids/FluidStack;FFFFFFLcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack;IZZLnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;getTintColor(Lnet/neoforged/neoforge/fluids/FluidStack;)I"
        )
    )
    private int pigeonplus$applyTankGasAlpha(IClientFluidTypeExtensions extensions, FluidStack fluid) {
        return GasTankRenderContext.applyTankGasAlpha(fluid, extensions.getTintColor(fluid));
    }
}
