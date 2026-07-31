package dev.anvilcraft.pigeonplus.mixin.client;

import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.category.FluidMixingCategory;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(FluidMixingCategory.class)
public class FluidMixingCategoryMixin {
    @Unique
    private static final int PIGEONPLUS_HEATER_X = 100;
    @Unique
    private static final int PIGEONPLUS_HEATER_Y = 41;
    @Unique
    private static final Component PIGEONPLUS_HEATER_ACTIVE = Component.translatable(
        "gui.anvilcraft.category.super_heating.need_activated"
    ).withStyle(ChatFormatting.GOLD);

    @Shadow
    @Final
    private IDrawable slot;

    @Inject(method = "setFluidMixingRecipe", at = @At("TAIL"))
    private static void pigeonplus$addMixedBiomassHeaterSlot(
        IRecipeLayoutBuilder builder,
        FluidMixingRecipe recipe,
        CallbackInfo ci
    ) {
        if (!pigeonplus$isMixedBiomassRecipe(recipe)) {
            return;
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, PIGEONPLUS_HEATER_X + 1, PIGEONPLUS_HEATER_Y + 1)
            .addItemStacks(java.util.List.of(ModBlocks.HEATER.asStack(), ModBlocks.BURNING_HEATER.asStack()))
            .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(PIGEONPLUS_HEATER_ACTIVE));
    }

    @Inject(method = "draw*", at = @At("TAIL"))
    private void pigeonplus$drawMixedBiomassHeaterSlot(
        RecipeHolder<FluidMixingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY,
        CallbackInfo ci
    ) {
        if (pigeonplus$isMixedBiomassRecipe(recipeHolder.value())) {
            this.slot.draw(guiGraphics, PIGEONPLUS_HEATER_X, PIGEONPLUS_HEATER_Y);
        }
    }

    @Unique
    private static boolean pigeonplus$isMixedBiomassRecipe(FluidMixingRecipe recipe) {
        return recipe.getFluidIngredients().size() == 1
            && Arrays.stream(recipe.getFluidIngredients().getFirst().getFluids())
                .anyMatch(fluid -> fluid.getFluid().isSame(AddonFluids.MIXED_BIOMASS.get()))
            && recipe.getFluidResults().stream()
                .map(FluidStack::getFluid)
                .anyMatch(fluid -> fluid.isSame(AddonFluids.GASEOUS_BIOGAS.get()));
    }
}
