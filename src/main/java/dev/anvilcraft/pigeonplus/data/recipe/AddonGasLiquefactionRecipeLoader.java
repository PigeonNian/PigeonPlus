package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.anvilcraft.pigeonplus.recipe.GasLiquefactionRecipe;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.neoforged.neoforge.fluids.FluidStack;

public class AddonGasLiquefactionRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        provider.accept(
            AnvilCraftPigeonPlus.of("gas_liquefaction/compressed_air"),
            new GasLiquefactionRecipe(
                new FluidStack(AddonFluids.COMPRESSED_AIR.get(), 415),
                new FluidStack(AddonFluids.LIQUID_OXYGEN.get(), 1)
            ),
            null
        );

        provider.accept(
            AnvilCraftPigeonPlus.of("gas_liquefaction/gaseous_biogas"),
            new GasLiquefactionRecipe(
                new FluidStack(AddonFluids.GASEOUS_BIOGAS.get(), 512),
                new FluidStack(AddonFluids.LIQUEFIED_BIOGAS.get(), 1)
            ),
            null
        );

        provider.accept(
            AnvilCraftPigeonPlus.of("gas_liquefaction/oxygen"),
            new GasLiquefactionRecipe(
                new FluidStack(ModFluids.OXYGEN.get(), 100),
                new FluidStack(AddonFluids.LIQUID_OXYGEN.get(), 1)
            ),
            null
        );
    }
}
