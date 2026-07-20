package dev.anvilcraft.pigeonplus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public final class GasContainerRenderUtil {
    public static boolean hasGas(List<FluidStack> fluids) {
        for (FluidStack fluid : fluids) {
            if (!fluid.isEmpty() && fluid.getFluid() instanceof GasFluid) {
                return true;
            }
        }
        return false;
    }

    public static void renderLayeredFluidBox(
        List<FluidStack> fluids,
        double renderCapacity,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ,
        MultiBufferSource buffers,
        PoseStack pose,
        int light
    ) {
        if (renderCapacity <= 0) {
            return;
        }

        List<FluidStack> liquids = new ArrayList<>();
        List<FluidStack> gases = new ArrayList<>();
        for (FluidStack fluid : fluids) {
            if (fluid.isEmpty()) {
                continue;
            }
            if (fluid.getFluid() instanceof GasFluid) {
                gases.add(fluid);
            } else {
                liquids.add(fluid);
            }
        }

        float liquidTop = renderLiquids(liquids, renderCapacity, minX, minY, minZ, maxX, maxY, maxZ, buffers, pose, light);
        if (gases.isEmpty()) {
            return;
        }
        float gasBottom = liquids.isEmpty() ? minY : liquidTop;
        if (gasBottom >= maxY) {
            return;
        }
        for (FluidStack gas : gases) {
            float fill = Mth.clamp((float) (gas.getAmount() / renderCapacity), 0.0f, 1.0f);
            GasTankRenderContext.renderWithFill(fill, () -> FluidRenderHelper.INSTANCE.renderFluidBox(
                gas,
                minX,
                gasBottom,
                minZ,
                maxX,
                maxY,
                maxZ,
                buffers,
                pose,
                light,
                true,
                false
            ));
        }
    }

    private static float renderLiquids(
        List<FluidStack> liquids,
        double renderCapacity,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ,
        MultiBufferSource buffers,
        PoseStack pose,
        int light
    ) {
        float cursor = minY;
        float height = maxY - minY;
        for (FluidStack liquid : liquids) {
            if (cursor >= maxY) {
                break;
            }
            float layerTop = Math.min(maxY, cursor + height * (float) (liquid.getAmount() / renderCapacity));
            if (layerTop > cursor) {
                FluidRenderHelper.INSTANCE.renderFluidBox(
                    liquid,
                    minX,
                    cursor,
                    minZ,
                    maxX,
                    layerTop,
                    maxZ,
                    buffers,
                    pose,
                    light,
                    true,
                    false
                );
            }
            cursor = layerTop;
        }
        return cursor;
    }

    private GasContainerRenderUtil() {
    }
}
