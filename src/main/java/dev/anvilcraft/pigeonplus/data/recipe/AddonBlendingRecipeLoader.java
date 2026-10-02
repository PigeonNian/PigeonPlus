package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.recipe.anvil.wrap.BlendingRecipe;
import net.neoforged.neoforge.common.Tags;

public class AddonBlendingRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        BlendingRecipe.builder()
            .requires(provider.getItems(), Tags.Items.CROPS, 25)
            .transform(AddonBlocks.MIXED_BIOMASS_CAULDRON.get(), 250)
            .save(provider, provider.safeKey(AnvilCraftPigeonPlus.of("blending/mixed_biomass_from_crops")));
    }
}