package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonItems;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import net.minecraft.world.item.Items;

public class AddonItemCompressRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        ItemCompressRecipe.builder()
            .requires(Items.PAPER, 16)
            .requires(ModItems.RESIN)
            .result(AddonBlocks.PAPER_BLOCK)
            .save(provider, AnvilCraftPigeonPlus.of("item_compress/paper_block"));

        ItemCompressRecipe.builder()
            .requires(Items.GLASS_BOTTLE, 8)
            .requires(Items.GUNPOWDER)
            .result(AddonItems.SPLASH_BOTTLE, 8)
            .save(provider, AnvilCraftPigeonPlus.of("item_compress/splash_bottle"));

        ItemCompressRecipe.builder()
            .requires(Items.GLASS_BOTTLE, 8)
            .requires(Items.DRAGON_BREATH)
            .result(AddonItems.LINGERING_BOTTLE, 8)
            .result(Items.GLASS_BOTTLE)
            .save(provider, AnvilCraftPigeonPlus.of("item_compress/lingering_bottle"));
    }
}
