package dev.anvilcraft.pigeonplus.integration.jei.category;

import dev.anvilcraft.pigeonplus.integration.jei.AnvilCraftPigeonPlusJeiPlugin;
import dev.anvilcraft.pigeonplus.integration.jei.recipe.PotionBrewingJeiRecipe;
import dev.anvilcraft.pigeonplus.integration.jei.recipe.PotionBrewingJeiRecipes;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiFluidUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class PotionBulkBrewingCategory implements IRecipeCategory<PotionBrewingJeiRecipe> {
    private static final int WIDTH = 162;
    private static final int HEIGHT = 64;

    private static final int DISPLAY_AMOUNT = 8000;

    private static final float MODEL_SCALE = 7.5F;
    private static final int HEATER_INPUT_GRID_SIZE = 4;
    private static final int HEATER_X = 100;
    private static final int HEATER_Y = 41;

    private static final Component HEATER_ACTIVE = Component.translatable(
        "gui.anvilcraft.category.super_heating.need_activated"
    ).withStyle(ChatFormatting.GOLD);
    private static final Component BULK = Component.translatable(
        "gui.anvilcraft_pigeon_plus.category.potion_bulk_brewing.bulk"
    ).withStyle(ChatFormatting.GRAY);

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrowIn;
    private final IDrawable arrowOut;
    private final ITickTimer timer;
    private final BlockState largeCauldron;
    private final BlockState giantAnvil;

    public PotionBulkBrewingCategory(IGuiHelper helper) {

        this.icon = helper.createDrawableItemStack(ModBlocks.LARGE_CAULDRON.asStack());
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
    public IRecipeType<PotionBrewingJeiRecipe> getRecipeType() {
        return AnvilCraftPigeonPlusJeiPlugin.POTION_BULK_BREWING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.anvilcraft_pigeon_plus.category.potion_bulk_brewing");
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
        PotionBrewingJeiRecipe recipe,
        IFocusGroup focuses
    ) {

        SlotPosition fluidPosition = inputPosition(0);
        IRecipeSlotBuilder fluidInput = JeiFluidUtil.addFluidSlot(
            builder,
            RecipeIngredientRole.INPUT,
            fluidPosition.x() + 1,
            fluidPosition.y() + 1,
            16,
            16,
            DISPLAY_AMOUNT,
            true,
            recipe.inputs()
        );
        SlotPosition itemPosition = inputPosition(1);
        builder.addSlot(RecipeIngredientRole.INPUT, itemPosition.x() + 1, itemPosition.y() + 1)
            .add(recipe.reagent())
            .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(BULK));
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, HEATER_X + 1, HEATER_Y + 1)
            .addItemStacks(List.of(ModBlocks.HEATER.asStack(), ModBlocks.BURNING_HEATER.asStack()))
            .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(HEATER_ACTIVE));

        SlotPosition outputPosition = fluidOutputPosition();
        IRecipeSlotBuilder fluidOutput = JeiFluidUtil.addFluidSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            outputPosition.x() + 1,
            outputPosition.y() + 1,
            16,
            16,
            DISPLAY_AMOUNT,
            true,
            recipe.outputs()
        );

        if (recipe.inputs().size() > 1
            && recipe.outputs().size() == recipe.inputs().size()) {
            builder.createFocusLink(fluidInput, fluidOutput);
        }

        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
            .addItemStacks(PotionBrewingJeiRecipes.potionItemsOf(recipe.inputs()));
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
            .addItemStacks(PotionBrewingJeiRecipes.potionItemsOf(recipe.outputs()));
    }

    @Override
    public void draw(
        PotionBrewingJeiRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor guiGraphics,
        double mouseX,
        double mouseY
    ) {

        SlotPosition fluidPosition = inputPosition(0);
        this.slot.draw(guiGraphics, fluidPosition.x(), fluidPosition.y());
        SlotPosition itemPosition = inputPosition(1);
        this.slot.draw(guiGraphics, itemPosition.x(), itemPosition.y());
        this.slot.draw(guiGraphics, HEATER_X, HEATER_Y);
        SlotPosition outputPosition = fluidOutputPosition();
        this.slot.draw(guiGraphics, outputPosition.x(), outputPosition.y());

        this.arrowIn.draw(guiGraphics, 47, 30);
        this.arrowOut.draw(guiGraphics, 99, 29);

        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer) / 3.0F;
        RenderSupport.renderBlockAt(
            guiGraphics, this.largeCauldron, 81, 45, MODEL_SCALE);
        RenderSupport.renderBlockAt(
            guiGraphics, this.giantAnvil, 81, 23 + anvilYOffset, MODEL_SCALE);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftPigeonPlusJeiPlugin.POTION_BULK_BREWING,
            PotionBrewingJeiRecipes.build(DISPLAY_AMOUNT)
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(
            registration, AnvilCraftPigeonPlusJeiPlugin.POTION_BULK_BREWING);
        registration.addCraftingStation(
            AnvilCraftPigeonPlusJeiPlugin.POTION_BULK_BREWING, new ItemStack(ModBlocks.LARGE_CAULDRON));
        registration.addCraftingStation(
            AnvilCraftPigeonPlusJeiPlugin.POTION_BULK_BREWING, new ItemStack(ModBlocks.HEATER));
        registration.addCraftingStation(
            AnvilCraftPigeonPlusJeiPlugin.POTION_BULK_BREWING, new ItemStack(ModBlocks.BURNING_HEATER));
    }

    private static SlotPosition inputPosition(int index) {
        if (PotionBulkBrewingCategory.HEATER_INPUT_GRID_SIZE == 1) {
            return new SlotPosition(15, 23);
        }
        if (PotionBulkBrewingCategory.HEATER_INPUT_GRID_SIZE == 2) {
            return new SlotPosition(6 + index * 19, 23);
        }
        if (PotionBulkBrewingCategory.HEATER_INPUT_GRID_SIZE == 3) {
            return index == 0
                   ? new SlotPosition(15, 14)
                   : new SlotPosition(6 + (index - 1) * 19, 33);
        }
        return new SlotPosition(6 + index % 2 * 19, 14 + index / 2 * 19);
    }

    private static SlotPosition fluidOutputPosition() {
        return new SlotPosition(129, outputRow());
    }

    private static int outputRow() {
        return 24;
    }

    private record SlotPosition(int x, int y) {
    }
}
