package dev.anvilcraft.pigeonplus.integration.jei.recipe;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public record PotionBrewingJeiRecipe(
    ItemStack reagent,
    List<FluidStack> inputs,
    List<FluidStack> outputs
) {
}
