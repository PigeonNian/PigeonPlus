package dev.anvilcraft.pigeonplus.integration.jei.category;

import dev.anvilcraft.pigeonplus.integration.jei.AnvilCraftPigeonPlusJeiPlugin;
import dev.anvilcraft.pigeonplus.integration.jei.recipe.PotionBrewingJeiRecipe;
import dev.anvilcraft.pigeonplus.integration.jei.recipe.PotionBrewingJeiRecipes;
import dev.dubhe.anvilcraft.block.power.consumer.HeaterBlock;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiFluidUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PotionBrewingCategory implements IRecipeCategory<PotionBrewingJeiRecipe> {
    private static final int WIDTH = 162;
    private static final int HEIGHT = 64;
    private static final int DISPLAY_AMOUNT = 1000;

    private static final float MODEL_SCALE = 12.0F;

    private static final int ANVIL_Y = 12;
    private static final int CAULDRON_Y = 30;
    private static final int HEATER_Y = 40;
    private static final int ARROW_IN_X = 54;
    private static final int ARROW_OUT_X = 92;
    private static final int ARROW_Y = 22;

    private static final Component HEATER_ACTIVE = Component.translatable(
        "gui.anvilcraft.category.super_heating.need_activated"
    ).withStyle(ChatFormatting.GOLD);
    private static final Component WHOLE_TANK = Component.translatable(
        "gui.anvilcraft_pigeon_plus.category.potion_brewing.whole_tank"
    ).withStyle(ChatFormatting.GRAY);

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrowIn;
    private final IDrawable arrowOut;
    private final ITickTimer timer;
    private final BlockState cauldron;
    private final BlockState heater;

    public PotionBrewingCategory(IGuiHelper helper) {
        this.icon = new DrawableBlockStateIcon(
            Blocks.ANVIL.defaultBlockState(),
            ModBlocks.FISH_TANK.getDefaultState()
        );
        this.slot = JeiRenderHelper.getSlotDefault(helper);
        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOut = JeiRenderHelper.getArrowOutput(helper);
        this.timer = helper.createTickTimer(30, 60, true);
        this.cauldron = ModBlocks.FISH_TANK.getDefaultState();

        this.heater = ModBlocks.HEATER.getDefaultState().setValue(HeaterBlock.OVERLOAD, false);
    }

    @Override
    public IRecipeType<PotionBrewingJeiRecipe> getRecipeType() {
        return AnvilCraftPigeonPlusJeiPlugin.POTION_BREWING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.anvilcraft_pigeon_plus.category.potion_brewing");
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

        builder.addSlot(RecipeIngredientRole.INPUT, JeiSlotUtil.INPUT_X, JeiSlotUtil.ITEM_Y)
            .add(recipe.reagent());
        IRecipeSlotBuilder inputSlot = JeiFluidUtil.addFluidSlot(
            builder,
            RecipeIngredientRole.INPUT,
            JeiSlotUtil.INPUT_X,
            JeiSlotUtil.FLUID_Y,
            16,
            16,
            DISPLAY_AMOUNT,
            false,
            recipe.inputs()
        );
        IRecipeSlotBuilder outputSlot = JeiFluidUtil.addFluidSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            JeiSlotUtil.OUTPUT_X,
            JeiSlotUtil.DEFAULT_Y,
            16,
            16,
            DISPLAY_AMOUNT,
            true,
            recipe.outputs()
        );
        outputSlot.addRichTooltipCallback((slotView, tooltip) -> tooltip.add(WHOLE_TANK));

        if (recipe.inputs().size() > 1
            && recipe.outputs().size() == recipe.inputs().size()) {
            builder.createFocusLink(inputSlot, outputSlot);
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
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlockAt(
            guiGraphics,
            this.heater,
            81,
            HEATER_Y,
            MODEL_SCALE
        );
        RenderSupport.renderBlockAt(
            guiGraphics,
            this.cauldron,
            81,
            CAULDRON_Y,
            MODEL_SCALE
        );
        RenderSupport.renderBlockAt(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            ANVIL_Y + anvilYOffset,
            MODEL_SCALE
        );
        this.arrowIn.draw(guiGraphics, ARROW_IN_X, ARROW_Y);
        this.arrowOut.draw(guiGraphics, ARROW_OUT_X, ARROW_Y);

        this.slot.draw(guiGraphics, JeiSlotUtil.INPUT_X - 1, JeiSlotUtil.ITEM_Y - 1);
        this.slot.draw(guiGraphics, JeiSlotUtil.INPUT_X - 1, JeiSlotUtil.FLUID_Y - 1);
        this.slot.draw(guiGraphics, JeiSlotUtil.OUTPUT_X - 1, JeiSlotUtil.DEFAULT_Y - 1);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        PotionBrewingJeiRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        if (mouseX >= 72 && mouseX <= 90 && mouseY >= HEATER_Y - 1 && mouseY <= HEATER_Y + 18) {
            tooltip.add(ModBlocks.HEATER.get().getName());
            tooltip.add(HEATER_ACTIVE);
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftPigeonPlusJeiPlugin.POTION_BREWING,
            PotionBrewingJeiRecipes.build(DISPLAY_AMOUNT)
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(
            registration, AnvilCraftPigeonPlusJeiPlugin.POTION_BREWING);
        registration.addCraftingStation(
            AnvilCraftPigeonPlusJeiPlugin.POTION_BREWING, new ItemStack(Items.CAULDRON));
        registration.addCraftingStation(
            AnvilCraftPigeonPlusJeiPlugin.POTION_BREWING, new ItemStack(ModBlocks.FISH_TANK));
        registration.addCraftingStation(
            AnvilCraftPigeonPlusJeiPlugin.POTION_BREWING, new ItemStack(ModBlocks.HEATER));
        registration.addCraftingStation(
            AnvilCraftPigeonPlusJeiPlugin.POTION_BREWING, new ItemStack(ModBlocks.BURNING_HEATER));
    }

}
