package dev.anvilcraft.pigeonplus.integration.jei.recipe;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public record PotionSoakingJeiRecipe(
    ItemStack input,
    List<FluidStack> potions,
    List<ItemStack> results
) {
}
