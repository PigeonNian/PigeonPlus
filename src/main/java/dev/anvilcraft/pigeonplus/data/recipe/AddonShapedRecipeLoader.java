package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

public class AddonShapedRecipeLoader {
    public AddonShapedRecipeLoader(RegistrumRecipeProvider provider) {
        this.blender(provider);
        this.stasisBeacon(provider);
        this.anvilPump(provider);
        this.feedSpreader(provider);
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

    private void stasisBeacon(RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AddonBlocks.STASIS_BEACON)
            .pattern("AAA")
            .pattern("ACA")
            .pattern("ABA")
            .define('A', ModItems.FROST_METAL_INGOT)
            .define('B', Items.EMERALD_BLOCK)
            .define('C', ModBlocks.CORRUPTED_BEACON)
            .unlockedBy("has_frost_metal_ingot", RegistrumRecipeProvider.has(ModItems.FROST_METAL_INGOT))
            .unlockedBy("has_emerald_block", RegistrumRecipeProvider.has(   Items.EMERALD_BLOCK))
            .unlockedBy("has_corrupted_beacon", RegistrumRecipeProvider.has(ModBlocks.CORRUPTED_BEACON))
            .save(provider);
    }

    private void anvilPump(RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AddonBlocks.ANVIL_PUMP)
            .pattern("A")
            .pattern("B")
            .pattern("C")
            .define('A', Items.PISTON)
            .define('B', Items.ANVIL)
            .define('C', ModItems.PIPE)
            .unlockedBy("has_piston", RegistrumRecipeProvider.has(Items.PISTON))
            .unlockedBy("has_anvil", RegistrumRecipeProvider.has(Items.ANVIL))
            .unlockedBy("has_pipe", RegistrumRecipeProvider.has(ModItems.PIPE))
            .save(provider);
    }

    private void feedSpreader(RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AddonBlocks.FEED_SPREADER)
            .pattern(" A ")
            .pattern(" B ")
            .pattern("CCC")
            .define('A', Items.PISTON)
            .define('B', Items.BUCKET)
            .define('C', ModBlocks.POLISHED_HEAVY_IRON_SLAB)
            .unlockedBy("has_piston", RegistrumRecipeProvider.has(Items.PISTON))
            .unlockedBy("has_bucket", RegistrumRecipeProvider.has(Items.BUCKET))
            .unlockedBy("has_polished_heavy_iron_slab", RegistrumRecipeProvider.has(ModBlocks.POLISHED_HEAVY_IRON_SLAB))
            .save(provider);
    }
}
