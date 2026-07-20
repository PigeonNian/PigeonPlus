package dev.anvilcraft.pigeonplus.client.renderer;

import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;

public final class GasTankRenderContext {
    private static final int MIN_ALPHA = 24;
    private static final int MAX_ALPHA = 192;
    private static final ThreadLocal<Integer> TANK_GAS_ALPHA = new ThreadLocal<>();

    public static void renderWithFill(float fill, Runnable renderer) {
        int alpha = Mth.clamp(Math.round(Mth.clamp(fill, 0.0f, 1.0f) * (MAX_ALPHA - MIN_ALPHA)) + MIN_ALPHA, MIN_ALPHA, MAX_ALPHA);
        TANK_GAS_ALPHA.set(alpha);
        try {
            renderer.run();
        } finally {
            TANK_GAS_ALPHA.remove();
        }
    }

    public static int applyTankGasAlpha(FluidStack fluid, int color) {
        Integer alpha = TANK_GAS_ALPHA.get();
        if (alpha == null || !(fluid.getFluid() instanceof GasFluid)) {
            return color;
        }
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private GasTankRenderContext() {
    }
}
