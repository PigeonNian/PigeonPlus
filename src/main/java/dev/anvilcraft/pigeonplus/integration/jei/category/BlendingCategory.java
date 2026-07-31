package dev.anvilcraft.pigeonplus.integration.jei.category;

import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonRecipeTypes;
import dev.anvilcraft.pigeonplus.integration.jei.AnvilCraftPigeonPlusJeiPlugin;
import dev.anvilcraft.pigeonplus.recipe.anvil.wrap.BlendingRecipe;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.AbstractProgressCategory;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiFluidUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiItemUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BlendingCategory extends AbstractProgressCategory<BlendingRecipe> {
    private static final String INPUT_FLUID = "input_fluid";
    private static final String OUTPUT_FLUID = "output_fluid";

    public BlendingCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(
                Blocks.CAULDRON.defaultBlockState(),
                AddonBlocks.BLENDER.getDefaultState().setValue(BlenderBlock.WORKING, true)
            ),
            Component.translatable("gui.anvilcraft_pigeon_plus.category.blending")
        );
    }

    @Override
    public RecipeType<RecipeHolder<BlendingRecipe>> getRecipeType() {
        return AnvilCraftPigeonPlusJeiPlugin.BLENDING;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BlendingRecipe> recipeHolder, IFocusGroup focuses) {
        BlendingRecipe recipe = recipeHolder.value();
        HasCauldronSimple cauldron = recipe.getHasCauldron();
        boolean hasInputItems = !recipe.getInputItems().isEmpty();
        boolean hasOutputItems = !recipe.getResultItems().isEmpty();
        boolean hasInputFluid = hasInputFluid(cauldron);
        boolean hasOutputFluid = hasOutputFluid(cauldron);
        boolean inputMixed = hasInputItems && hasInputFluid;
        boolean outputMixed = hasOutputItems && hasOutputFluid;

        if (hasInputItems) {
            if (inputMixed) {
                JeiItemUtil.addItemInputSlots(builder, recipe.getInputItems());
            } else {
                JeiItemUtil.addDefaultInputSlots(builder, recipe.getInputItems());
            }
        }
        if (hasInputFluid) {
            if (inputMixed) {
                JeiFluidUtil.addFluidInputSlot(builder, INPUT_FLUID, 16, 16, cauldron);
            } else {
                JeiFluidUtil.addDefaultInputSlot(builder, INPUT_FLUID, 16, 16, cauldron);
            }
        }
        if (hasOutputItems) {
            if (outputMixed) {
                JeiItemUtil.addItemOutputSlots(builder, recipe.getResultItems());
            } else {
                JeiItemUtil.addDefaultOutputSlots(builder, recipe.getResultItems());
            }
        }
        if (hasOutputFluid) {
            if (outputMixed) {
                JeiFluidUtil.addFluidOutputSlot(builder, OUTPUT_FLUID, 16, 16, cauldron);
            } else {
                JeiFluidUtil.addDefaultOutputSlot(builder, OUTPUT_FLUID, 16, 16, cauldron);
            }
        }
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder,
        RecipeHolder<BlendingRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        JeiFluidUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void draw(
        RecipeHolder<BlendingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        BlendingRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderSupport.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            12 + anvilYOffset,
            20,
            12,
            RenderSupport.SINGLE_BLOCK
        );
        RenderSupport.renderBlock(
            guiGraphics,
            getDisplayedInputCauldron(recipe),
            81,
            30,
            10,
            12,
            RenderSupport.SINGLE_BLOCK
        );
        RenderSupport.renderBlock(
            guiGraphics,
            AddonBlocks.BLENDER.getDefaultState().setValue(BlenderBlock.WORKING, true),
            81,
            40,
            0,
            12,
            RenderSupport.SINGLE_BLOCK
        );

        arrowIn.draw(guiGraphics, 54, 20);
        arrowOut.draw(guiGraphics, 92, 19);

        HasCauldronSimple cauldron = recipe.getHasCauldron();
        boolean hasInputItems = !recipe.getInputItems().isEmpty();
        boolean hasOutputItems = !recipe.getResultItems().isEmpty();
        boolean hasInputFluid = hasInputFluid(cauldron);
        boolean hasOutputFluid = hasOutputFluid(cauldron);
        boolean inputMixed = hasInputItems && hasInputFluid;
        boolean outputMixed = hasOutputItems && hasOutputFluid;

        if (hasInputItems) {
            if (inputMixed) {
                JeiSlotUtil.drawItemInputSlots(guiGraphics, slotDefault, recipe.getInputItems().size());
            } else {
                JeiSlotUtil.drawDefaultInputSlots(guiGraphics, slotDefault, recipe.getInputItems().size());
            }
        }
        if (hasOutputItems) {
            if (JeiRecipeUtil.isChance(recipe.getResultItems())) {
                if (outputMixed) {
                    JeiSlotUtil.drawItemOutputSlots(guiGraphics, slotProbability, recipe.getResultItems().size());
                } else {
                    JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, slotProbability, recipe.getResultItems().size());
                }
            } else if (outputMixed) {
                JeiSlotUtil.drawItemOutputSlots(guiGraphics, slotDefault, recipe.getResultItems().size());
            } else {
                JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, slotDefault, recipe.getResultItems().size());
            }
        }
        if (hasInputFluid) {
            if (inputMixed) {
                JeiSlotUtil.drawFluidInputSlots(guiGraphics, slotDefault, 1);
            } else {
                JeiSlotUtil.drawDefaultInputSlots(guiGraphics, slotDefault, 1);
            }
        }
        if (hasOutputFluid) {
            if (outputMixed) {
                JeiSlotUtil.drawFluidOutputSlots(guiGraphics, slotDefault, 1);
            } else {
                JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, slotDefault, 1);
            }
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<BlendingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        BlendingRecipe recipe = recipeHolder.value();
        if (mouseX >= 72 && mouseX <= 90) {
            if (mouseY >= 24 && mouseY <= 43) {
                tooltip.add(getDisplayedInputCauldron(recipe).getBlock().getName());
            }
            if (mouseY >= 34 && mouseY <= 53) {
                tooltip.add(AddonBlocks.BLENDER.get().getName());
            }
        }
    }

    private BlockState getDisplayedInputCauldron(BlendingRecipe recipe) {
        if (recipe.isProduceFluid() && !recipe.isConsumeFluid()) {
            return Blocks.CAULDRON.defaultBlockState();
        }
        return CauldronUtil.fullState(recipe.getHasCauldron().getFluidCauldron());
    }

    private static boolean hasInputFluid(HasCauldronSimple cauldron) {
        return cauldron.fluidTag() != null || HasCauldron.isNotEmpty(cauldron.fluid());
    }

    private static boolean hasOutputFluid(HasCauldronSimple cauldron) {
        return HasCauldron.isNotEmpty(cauldron.transform());
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftPigeonPlusJeiPlugin.BLENDING,
            JeiRecipeUtil.getRecipeHoldersFromType(AddonRecipeTypes.BLENDING_TYPE.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftPigeonPlusJeiPlugin.BLENDING);
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), AnvilCraftPigeonPlusJeiPlugin.BLENDING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FISH_TANK), AnvilCraftPigeonPlusJeiPlugin.BLENDING);
        registration.addRecipeCatalyst(new ItemStack(AddonBlocks.BLENDER), AnvilCraftPigeonPlusJeiPlugin.BLENDING);
    }
}
