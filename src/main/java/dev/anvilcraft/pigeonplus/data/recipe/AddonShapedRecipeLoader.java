package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

public class AddonShapedRecipeLoader {
    public AddonShapedRecipeLoader(RegistrumRecipeProvider provider) {
        this.blender(provider);
    }

    private void blender(RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AddonBlocks.BLENDER)
            .pattern(" S ")
            .pattern("PTI")
            .pattern("III")
            .define('S', Items.IRON_SHOVEL)
            .define('P', ModBlocks.PIPE_STRAIGHT)
            .define('T', ModBlocks.FLUID_TANK)
            .define('I', Items.IRON_INGOT)
            .unlockedBy("has_iron_shovel", RegistrumRecipeProvider.has(Items.IRON_SHOVEL))
            .unlockedBy("has_pipe_straight", RegistrumRecipeProvider.has(ModBlocks.PIPE_STRAIGHT))
            .unlockedBy("has_fluid_tank", RegistrumRecipeProvider.has(ModBlocks.FLUID_TANK))
            .unlockedBy("has_iron_ingot", RegistrumRecipeProvider.has(Items.IRON_INGOT))
            .save(provider);
    }
}
