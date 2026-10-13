package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
import net.minecraft.world.level.block.Blocks;

public class AddonProceduralProcessRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        ProceduralProcessRecipeBuilder.of(ModBlocks.HEATER.get())
            .addStep(
                BlockCompressRecipe.builder()
                    .input(ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK.get())
                    .input(ModBlocks.HEATER.get())
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .addStep(
                BlockCompressRecipe.builder()
                    .input(Blocks.IRON_BARS)
                    .input(ModBlocks.WIP_BLOCK.get())
                    .result(ModBlocks.WIP_BLOCK.get())
                    .buildRecipe()
            )
            .result(AddonBlocks.ELECTRIC_BLENDER)
            .icon(AddonBlocks.ELECTRIC_BLENDER.asStack())
            .displayedModels(AnvilCraftPigeonPlus.of("electric_blender"))
            .save(provider, AnvilCraftPigeonPlus.of("procedural_process/electric_blender"));
    }
}
