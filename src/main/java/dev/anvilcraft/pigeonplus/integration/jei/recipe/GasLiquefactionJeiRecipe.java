package dev.anvilcraft.pigeonplus.integration.jei.recipe;

import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.anvilcraft.pigeonplus.util.GasLiquefactionTracker;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

public record GasLiquefactionJeiRecipe(Fluid gas, Fluid liquid, int gasAmount, int liquidAmount) {
    public static List<GasLiquefactionJeiRecipe> recipes() {
        return List.of(
            new GasLiquefactionJeiRecipe(
                AddonFluids.GASEOUS_BIOGAS.get(),
                AddonFluids.LIQUEFIED_BIOGAS.get(),
                GasLiquefactionTracker.BIOGAS_TO_LIQUEFIED_BIOGAS_RATIO,
                1
            ),
            new GasLiquefactionJeiRecipe(
                AddonFluids.COMPRESSED_AIR.get(),
                AddonFluids.LIQUID_OXYGEN.get(),
                GasLiquefactionTracker.COMPRESSED_AIR_TO_LIQUID_OXYGEN_RATIO,
                1
            )
        );
    }
}
