package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.anvilcraft.pigeonplus.recipe.FermentationRecipe;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

public class AddonFermentationRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        provider.accept(
            AnvilCraftPigeonPlus.of("fermentation/mixed_biomass_to_gaseous_biogas"),
            new FermentationRecipe(
                new FluidStack(AddonFluids.MIXED_BIOMASS.get(), FluidType.BUCKET_VOLUME),
                new FluidStack(AddonFluids.GASEOUS_BIOGAS.get(), 2 * FluidType.BUCKET_VOLUME)
            ),
            null
        );
    }
}
