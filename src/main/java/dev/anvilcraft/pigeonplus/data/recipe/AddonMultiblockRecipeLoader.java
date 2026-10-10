package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockBuilder;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;

import java.util.Map;

public class AddonMultiblockRecipeLoader {
    private AddonMultiblockRecipeLoader() {
    }

    public static void init(RegistrumRecipeProvider provider) {
        MultiblockBuilder builder = MultiblockRecipe.builder(AddonBlocks.LARGE_FIREWORKS_ROCKET, 1);
        for (String[] layer : LargeFireworksRocketDisplayRecipe.INPUT_GRID) {
            builder.layer(layer);
        }
        for (Map.Entry<Character, BlockStatePredicate.Builder> entry
            : LargeFireworksRocketDisplayRecipe.INPUT_MAPPING.entrySet()) {
            builder.symbol(entry.getKey(), entry.getValue());
        }
        builder.save(provider, AnvilCraftPigeonPlus.of("multiblock/large_fireworks_rocket"));
    }
}
