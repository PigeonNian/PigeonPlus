package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.List;

public class AddonFluidMixingRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        provider.accept(
            AnvilCraftPigeonPlus.of("fluid_mixing/mixed_biomass_to_gaseous_biogas"),
            new FluidMixingRecipe(
                List.of(SizedFluidIngredient.of(AddonFluids.MIXED_BIOMASS.get(), FluidType.BUCKET_VOLUME)),
                List.of(),
                List.of(new FluidStack(AddonFluids.GASEOUS_BIOGAS.get(), 2 * FluidType.BUCKET_VOLUME)),
                true
            ),
            null
        );
    }
}
