package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;

public class AddonRecipeHandler {
    public static void init(RegistrumRecipeProvider provider) {
        new AddonShapedRecipeLoader(provider);
        AddonBlendingRecipeLoader.init(provider);
        AddonLiquidChocolateRecipeLoader.init(provider);
        AddonItemInjectRecipeLoader.init(provider);
        AddonItemCompressRecipeLoader.init(provider);
        AddonProceduralProcessRecipeLoader.init(provider);
        AddonFermentationRecipeLoader.init(provider);
        AddonReformingRecipeLoader.init(provider);
        AddonGasLiquefactionRecipeLoader.init(provider);
        AddonMultiblockRecipeLoader.init(provider);
        AddonMultiblockConversionRecipeLoader.init(provider);
        AddonEnchantmentRecipeLoader.init(provider);
    }
}
