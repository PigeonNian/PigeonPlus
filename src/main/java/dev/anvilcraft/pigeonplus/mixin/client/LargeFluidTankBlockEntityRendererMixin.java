package dev.anvilcraft.pigeonplus.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.pigeonplus.client.renderer.GasContainerRenderUtil;
import dev.anvilcraft.pigeonplus.client.renderer.GasTankRenderContext;
import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LargeFluidTankBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;

@Mixin(LargeFluidTankBlockEntityRenderer.class)
public class LargeFluidTankBlockEntityRendererMixin {
    private static final float TANK_W = 4 / 16.0f + 0.001f;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$renderGasAboveLiquid(
        LargeFluidTankBlockEntity tank,
        float tickDelta,
        PoseStack ms,
        MultiBufferSource vertexConsumers,
        int light,
        int overlay,
        CallbackInfo ci
    ) {
        if (!tank.isMainPart()) {
            return;
        }
        List<FluidStack> fluids = tank.getStoredFluids().stream()
            .filter(fluid -> !fluid.isEmpty())
            .sorted(Comparator
                .comparingInt(FluidStack::getAmount)
                .reversed()
                .thenComparing(fluid -> BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString()))
            .toList();
        if (!GasContainerRenderUtil.hasGas(fluids)) {
            return;
        }

        long totalAmount = fluids.stream().mapToLong(FluidStack::getAmount).sum();
        long renderAmount = tank.isEnhanced()
            ? Math.max(totalAmount, LargeFluidTankBlockEntity.INFINITY_THRESHOLD)
            : LargeFluidTankBlockEntity.BASE_CAPACITY;
        GasContainerRenderUtil.renderLayeredFluidBox(
            fluids,
            renderAmount,
            TANK_W - 1.0f,
            TANK_W - 1.0f,
            TANK_W - 1.0f,
            2.0f - TANK_W,
            2.0f - TANK_W,
            2.0f - TANK_W,
            vertexConsumers,
            ms,
            light
        );
        ci.cancel();
    }

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
