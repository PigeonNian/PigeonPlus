package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;

public class AddonRecipeHandler {
    public static void init(RegistrumRecipeProvider provider) {
        new AddonShapedRecipeLoader(provider);
    }
}
