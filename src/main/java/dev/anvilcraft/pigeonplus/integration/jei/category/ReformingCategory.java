package dev.anvilcraft.pigeonplus.integration.jei.category;

import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonRecipeTypes;
import dev.anvilcraft.pigeonplus.integration.jei.AnvilCraftPigeonPlusJeiPlugin;
import dev.anvilcraft.pigeonplus.recipe.ReformingRecipe;
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

/**
 * 蒸汽重整的 JEI 展示。
 *
 * <p>排版沿用「批量酿造」（同样源自本体 {@code AbstractLiquidReactionCategory} 的大锅布局）：
 * 输入按 2×2 网格摆放，右下角是催化剂槽位，绘制顺序为「槽位 → 箭头 → 巨型铁砧 → 大锅」，比例 7.5。
 *
 * <p>与批量酿造唯一的差别是催化剂占了<b>两格</b>：搅拌机和磁铁块各占一格。
 * 它们必须同时存在，塞进同一格会变成 JEI 的轮播展示，看起来像「二选一」，
 * 所以这里把磁铁块单独放一格，并把分类高度加高 18 像素容纳第二格。
 */
public class ReformingCategory implements IRecipeCategory<RecipeHolder<ReformingRecipe>> {
    private static final int WIDTH = 162;
    private static final int HEIGHT = 82;

    // 与本体 AbstractLiquidReactionCategory 一致。
    private static final float MODEL_SCALE = 7.5F;
    private static final int HEATER_INPUT_GRID_SIZE = 4;
    private static final int HEATER_X = 100;
    private static final int HEATER_Y = 41;
    /** 磁铁块单独占一格，放在搅拌机**下方**，避免和输出槽/箭头挤在一起。 */
    private static final int MAGNET_X = 100;
    private static final int MAGNET_Y = 59;

    private static final Component REQUIREMENT = Component.translatable(
        "gui.anvilcraft_pigeon_plus.category.reforming.requirement"
    ).withStyle(ChatFormatting.GOLD);

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrowIn;
    private final IDrawable arrowOut;
    private final ITickTimer timer;
    private final BlockState largeCauldron;
    private final BlockState giantAnvil;

    public ReformingCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.MAGNET_BLOCK));
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
    public RecipeType<RecipeHolder<ReformingRecipe>> getRecipeType() {
        return AnvilCraftPigeonPlusJeiPlugin.REFORMING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.anvilcraft_pigeon_plus.category.reforming");
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
        RecipeHolder<ReformingRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        ReformingRecipe recipe = recipeHolder.value();
        for (int index = 0; index < recipe.inputs().size(); index++) {
            var input = recipe.inputs().get(index);
            SlotPosition position = inputPosition(index);
            JeiFluidUtil.addFluidSlot(
                builder,
                RecipeIngredientRole.INPUT,
                position.x() + 1,
                position.y() + 1,
                16,
                16,
                input.getAmount(),
                false,
                List.of(input)
            );
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, HEATER_X + 1, HEATER_Y + 1)
            .addItemStacks(List.of(
                new ItemStack(AddonBlocks.BLENDER),
                new ItemStack(AddonBlocks.ELECTRIC_BLENDER)
            ))
            .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(REQUIREMENT));
        // 磁铁块单独一格：两者缺一不可，轮播会让人误以为「有其一即可」。
        builder.addSlot(RecipeIngredientRole.CATALYST, MAGNET_X + 1, MAGNET_Y + 1)
            .addItemStack(new ItemStack(ModBlocks.MAGNET_BLOCK))
            .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(REQUIREMENT));

        SlotPosition outputPosition = fluidOutputPosition();
        JeiFluidUtil.addFluidSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            outputPosition.x() + 1,
            outputPosition.y() + 1,
            16,
            16,
            recipe.output().getAmount(),
            true,
            List.of(recipe.output())
        );
    }

    @Override
    public void draw(
        RecipeHolder<ReformingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        // 本体大锅布局的绘制顺序：先槽位，再箭头，最后方块。
        ReformingRecipe recipe = recipeHolder.value();
        for (int index = 0; index < recipe.inputs().size(); index++) {
            SlotPosition position = inputPosition(index);
            this.slot.draw(guiGraphics, position.x(), position.y());
        }
        this.slot.draw(guiGraphics, HEATER_X, HEATER_Y);
        this.slot.draw(guiGraphics, MAGNET_X, MAGNET_Y);
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
            AnvilCraftPigeonPlusJeiPlugin.REFORMING,
            JeiRecipeUtil.getRecipeHoldersFromType(AddonRecipeTypes.REFORMING_TYPE.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // 只有「大型炼药锅 + 巨型铁砧」这套组合能执行重整：
        // 普通铁砧、皇家/余烬/浮霜/超限/幽匿铁砧都不行，不能沿用本体的通用铁砧催化剂列表。
        registration.addRecipeCatalyst(
            new ItemStack(ModBlocks.GIANT_ANVIL), AnvilCraftPigeonPlusJeiPlugin.REFORMING);
        registration.addRecipeCatalyst(
            ModBlocks.LARGE_CAULDRON.asStack(), AnvilCraftPigeonPlusJeiPlugin.REFORMING);
        registration.addRecipeCatalyst(
            new ItemStack(AddonBlocks.BLENDER), AnvilCraftPigeonPlusJeiPlugin.REFORMING);
        registration.addRecipeCatalyst(
            new ItemStack(AddonBlocks.ELECTRIC_BLENDER), AnvilCraftPigeonPlusJeiPlugin.REFORMING);
        registration.addRecipeCatalyst(
            ModBlocks.MAGNET_BLOCK.asStack(), AnvilCraftPigeonPlusJeiPlugin.REFORMING);
    }

    // 以下三个定位方法与本体 AbstractLiquidReactionCategory 完全一致。

    private static SlotPosition inputPosition(int index) {
        if (ReformingCategory.HEATER_INPUT_GRID_SIZE == 1) {
            return new SlotPosition(15, 23);
        }
        if (ReformingCategory.HEATER_INPUT_GRID_SIZE == 2) {
            return new SlotPosition(6 + index * 19, 23);
        }
        if (ReformingCategory.HEATER_INPUT_GRID_SIZE == 3) {
            return index == 0
                   ? new SlotPosition(15, 14)
                   : new SlotPosition(6 + (index - 1) * 19, 33);
        }
        return new SlotPosition(6 + index % 2 * 19, 14 + index / 2 * 19);
    }

    private static SlotPosition fluidOutputPosition() {
        return new SlotPosition(129, 24);
    }

    private record SlotPosition(int x, int y) {
    }
}
