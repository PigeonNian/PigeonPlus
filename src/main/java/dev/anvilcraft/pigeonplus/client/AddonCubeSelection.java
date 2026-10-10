package dev.anvilcraft.pigeonplus.client;

import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;

public final class AddonCubeSelection {
    private static boolean enabled;
    private static boolean excluded;

    private AddonCubeSelection() {
    }

    public static void enableOwnNamespace() {
        if (enabled) {
            return;
        }
        enabled = true;
        CubeSelection.enableNamespace(AnvilCraftPigeonPlus.MOD_ID);
    }

    public static void excludeFromPrecisePicking() {
        if (excluded) {
            return;
        }
        excluded = true;

        CubeSelection.exclude(AddonBlocks.LARGE_FIREWORKS_ROCKET.get());
    }
}
