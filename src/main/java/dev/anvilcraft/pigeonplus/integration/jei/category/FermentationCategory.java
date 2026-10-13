package dev.anvilcraft.pigeonplus.integration.jei.category;

import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonRecipeTypes;
import dev.anvilcraft.pigeonplus.integration.jei.AnvilCraftPigeonPlusJeiPlugin;
import dev.anvilcraft.pigeonplus.recipe.FermentationRecipe;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiFluidUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class FermentationCategory implements IRecipeCategory<RecipeHolder<FermentationRecipe>> {
    private static final int WIDTH = 162;
    private static final int HEIGHT = 64;
    private static final float MODEL_SCALE = 7.5F;

    private static final int INPUT_X = 15;
    private static final int INPUT_Y = 23;
    private static final int OUTPUT_X = 129;
    private static final int OUTPUT_Y = 24;
    private static final int HINT_X = 100;
    private static final int HINT_Y = 41;

    private static final Component REQUIREMENT = Component.translatable(
        "gui.anvilcraft_pigeon_plus.category.fermentation.requirement"
    ).withStyle(ChatFormatting.GOLD);

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrowIn;
    private final IDrawable arrowOut;
    private final ITickTimer timer;
    private final BlockState largeCauldron;
    private final BlockState giantAnvil;

    public FermentationCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(AddonBlocks.BLENDER));
        this.slot = JeiRenderHelper.getSlotDefault(helper);
        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOut = JeiRenderHelper.getArrowOutput(helper);
        this.timer = helper.createTickTimer(30, 60, true);
        this.largeCauldron = ModBlocks.LARGE_CAULDRON.getDefaultState()
            .setValue(LargeCauldronBlock.HALF, Cube3x3PartHalf.MID_CENTER);
        this.giantAnvil = JeiBlockIngredientUtil.getRenderablePreviewState(
            ModBlocks.GIANT_ANVIL.getDefaultState());
    }

    @Override
    public RecipeType<RecipeHolder<FermentationRecipe>> getRecipeType() {
        return AnvilCraftPigeonPlusJeiPlugin.FERMENTATION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.anvilcraft_pigeon_plus.category.fermentation");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<FermentationRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        FermentationRecipe recipe = recipeHolder.value();
        JeiFluidUtil.addFluidSlot(
            builder,
            RecipeIngredientRole.INPUT,
            INPUT_X + 1,
            INPUT_Y + 1,
            16,
            16,
            recipe.input().getAmount(),
            false,
            List.of(recipe.input())
        );
        JeiFluidUtil.addFluidSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            OUTPUT_X + 1,
            OUTPUT_Y + 1,
            16,
            16,
            recipe.output().getAmount(),
            true,
            List.of(recipe.output())
        );
        builder.addSlot(RecipeIngredientRole.CATALYST, HINT_X + 1, HINT_Y + 1)
            .addItemStacks(List.of(
                new ItemStack(AddonBlocks.BLENDER),
                new ItemStack(AddonBlocks.ELECTRIC_BLENDER)
            ))
            .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(REQUIREMENT));
    }

    @Override
    public void draw(
        RecipeHolder<FermentationRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        this.slot.draw(guiGraphics, INPUT_X, INPUT_Y);
        this.slot.draw(guiGraphics, OUTPUT_X, OUTPUT_Y);
        this.slot.draw(guiGraphics, HINT_X, HINT_Y);
        this.arrowIn.draw(guiGraphics, 47, 30);
        this.arrowOut.draw(guiGraphics, 99, 29);

        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer) / 3.0F;
        RenderSupport.renderBlock(
            guiGraphics, this.giantAnvil, 81, 23 + anvilYOffset, 20, MODEL_SCALE, RenderSupport.SINGLE_BLOCK);
        RenderSupport.renderBlock(
            guiGraphics, this.largeCauldron, 81, 45, 10, MODEL_SCALE, RenderSupport.SINGLE_BLOCK);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftPigeonPlusJeiPlugin.FERMENTATION,
            JeiRecipeUtil.getRecipeHoldersFromType(AddonRecipeTypes.FERMENTATION_TYPE.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
            ModBlocks.LARGE_CAULDRON.asStack(), AnvilCraftPigeonPlusJeiPlugin.FERMENTATION);
        registration.addRecipeCatalyst(
            ModBlocks.GIANT_ANVIL.asStack(), AnvilCraftPigeonPlusJeiPlugin.FERMENTATION);
        registration.addRecipeCatalyst(
            new ItemStack(AddonBlocks.BLENDER), AnvilCraftPigeonPlusJeiPlugin.FERMENTATION);
        registration.addRecipeCatalyst(
            new ItemStack(AddonBlocks.ELECTRIC_BLENDER), AnvilCraftPigeonPlusJeiPlugin.FERMENTATION);
    }
}
