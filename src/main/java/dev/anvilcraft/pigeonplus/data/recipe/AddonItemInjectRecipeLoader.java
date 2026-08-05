package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class AddonItemInjectRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        ItemInjectRecipe.builder()
            .requires(ModBlocks.EMBER_METAL_BLOCK)
            .inputBlock(ModBlocks.GIANT_ANVIL)
            .result(AddonBlocks.NOZZLE)
            .result(ModBlocks.HEAVY_IRON_BLOCK)
            .save(provider, AnvilCraftPigeonPlus.of("item_inject/nozzle"));

        // 展示：鸽子铁砧是正经铁砧，砸白羊毛会掉羽毛（若不需要专属配方，删掉这段即可）
        ItemInjectRecipe.builder()
            .requires(Blocks.WHITE_WOOL)
            .inputBlock(AddonBlocks.PIGEON_ANVIL)
            .result(Items.FEATHER, 3)
            .save(provider, AnvilCraftPigeonPlus.of("item_inject/pigeon_anvil_feather"));
    }
}
