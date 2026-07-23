package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;

public class AddonItemInjectRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        ItemInjectRecipe.builder()
            .requires(ModBlocks.EMBER_METAL_BLOCK)
            .inputBlock(ModBlocks.GIANT_ANVIL)
            .result(AddonBlocks.NOZZLE)
            .result(ModBlocks.HEAVY_IRON_BLOCK)
            .save(provider, AnvilCraftPigeonPlus.of("item_inject/nozzle"));
    }
}
