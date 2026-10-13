package dev.anvilcraft.pigeonplus.integration.jei.category;

import dev.anvilcraft.pigeonplus.init.AddonBlocks;
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
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 大型炼药锅的「批量酿造」分类。
 *
 * <p>与小锅版是同一套配方，区别只在容量：大锅一次炼制 8B（8 个储罐各 1 桶），
 * 所以这里展示 8000mB，且一次只消耗 1 个材料。
 *
 * <p>排版严格照搬本体固液反应的大锅布局（{@code AbstractLiquidReactionCategory}）：
 * 输入按 2x2 网格摆放（有加热器时网格固定为 4 格），加热器是右下角的<b>催化剂槽位</b>，
 * 绘制顺序为「槽位 → 箭头 → 巨型铁砧 → 大锅」，比例 7.5。
 */
public class PotionBulkBrewingCategory implements IRecipeCategory<PotionBrewingJeiRecipe> {
    private static final int WIDTH = 162;
    private static final int HEIGHT = 64;

    // 大锅一次 8B。
    private static final int DISPLAY_AMOUNT = 8000;

    // 与本体 AbstractLiquidReactionCategory 一致。
    private static final float MODEL_SCALE = 7.5F;
    private static final int HEATER_INPUT_GRID_SIZE = 4;
    private static final int HEATER_X = 100;
    private static final int HEATER_Y = 41;

    private static final Component BLENDER_ACTIVE = Component.translatable(
        "gui.anvilcraft_pigeon_plus.category.potion_bulk_brewing.need_blender"
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
        // 图标是一口大锅。本体 FluidReactionCategory 用的就是物品图标：
        // helper.createDrawableItemStack(ModBlocks.LARGE_CAULDRON.asStack())，
        // 走物品模型所以大小天然合适，不需要自己调缩放。
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
    public RecipeType<PotionBrewingJeiRecipe> getRecipeType() {
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
        // 有加热器时输入网格固定为 4 格，药水占第 0 格、材料占第 1 格。
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
            .addItemStack(recipe.reagent())
            .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(BULK));
        builder.addSlot(RecipeIngredientRole.CATALYST, HEATER_X + 1, HEATER_Y + 1)
            .addItemStack(new ItemStack(AddonBlocks.ELECTRIC_BLENDER))
            .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(BLENDER_ACTIVE));

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
        // 输入与输出按索引一一对应，联动让两边同步切换。
        // JEI 要求两端候选数量完全相等，否则抛异常、整个分类点不开，所以两端都验证。
        if (recipe.inputs().size() > 1
            && recipe.outputs().size() == recipe.inputs().size()) {
            builder.createFocusLink(fluidInput, fluidOutput);
        }
        // 药水流体没有桶，JeiFluidUtil 那套「用桶物品登记」在这里是空转，
        // 所以补上药水物品，玩家拿药水物品按 U/R 才能查到酿造配方。
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
            .addItemStacks(PotionBrewingJeiRecipes.potionItemsOf(recipe.inputs()));
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
            .addItemStacks(PotionBrewingJeiRecipes.potionItemsOf(recipe.outputs()));
    }

    @Override
    public void draw(
        PotionBrewingJeiRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        // 本体大锅布局的绘制顺序：先槽位，再箭头，最后方块。
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
        RenderSupport.renderBlock(
            guiGraphics, this.giantAnvil, 81, 23 + anvilYOffset, 20, MODEL_SCALE, RenderSupport.SINGLE_BLOCK);
        RenderSupport.renderBlock(
            guiGraphics, this.largeCauldron, 81, 45, 10, MODEL_SCALE, RenderSupport.SINGLE_BLOCK);
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
        registration.addRecipeCatalyst(
            new ItemStack(ModBlocks.LARGE_CAULDRON), AnvilCraftPigeonPlusJeiPlugin.POTION_BULK_BREWING);
        registration.addRecipeCatalyst(
            new ItemStack(AddonBlocks.ELECTRIC_BLENDER), AnvilCraftPigeonPlusJeiPlugin.POTION_BULK_BREWING);
    }

    // 以下三个定位方法与本体 AbstractLiquidReactionCategory 完全一致。

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
