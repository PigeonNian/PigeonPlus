package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.anvilcraft.pigeonplus.recipe.GasLiquefactionRecipe;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

public class AddonGasLiquefactionRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        pigeonplus$accept(provider, "gas_liquefaction/compressed_air", new GasLiquefactionRecipe(
            new FluidStackTemplate(AddonFluids.COMPRESSED_AIR.get(), 415),
            new FluidStackTemplate(AddonFluids.LIQUID_OXYGEN.get(), 1)
        ));

        pigeonplus$accept(provider, "gas_liquefaction/compressed_air_hydrogen", new GasLiquefactionRecipe(
            new FluidStackTemplate(AddonFluids.COMPRESSED_AIR.get(), 830),
            new FluidStackTemplate(AddonFluids.LIQUID_HYDROGEN.get(), 1)
        ));

        pigeonplus$accept(provider, "gas_liquefaction/gaseous_biogas", new GasLiquefactionRecipe(
            new FluidStackTemplate(AddonFluids.GASEOUS_BIOGAS.get(), 512),
            new FluidStackTemplate(AddonFluids.LIQUEFIED_BIOGAS.get(), 1)
        ));

        pigeonplus$accept(provider, "gas_liquefaction/oxygen", new GasLiquefactionRecipe(
            new FluidStackTemplate(ModFluids.OXYGEN.get(), 100),
            new FluidStackTemplate(AddonFluids.LIQUID_OXYGEN.get(), 1)
        ));
    }

    private static void pigeonplus$accept(
        RegistrumRecipeProvider provider, String path, GasLiquefactionRecipe recipe
    ) {
        Identifier id = AnvilCraftPigeonPlus.of(path);
        provider.accept(provider.safeKey(id), recipe, null);
    }
}
