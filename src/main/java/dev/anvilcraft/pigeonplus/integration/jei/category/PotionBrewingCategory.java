package dev.anvilcraft.pigeonplus.integration.jei.category;

import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.integration.jei.AnvilCraftPigeonPlusJeiPlugin;
import dev.anvilcraft.pigeonplus.integration.jei.recipe.PotionBrewingJeiRecipe;
import dev.anvilcraft.pigeonplus.integration.jei.recipe.PotionBrewingJeiRecipes;
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
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PotionBrewingCategory implements IRecipeCategory<PotionBrewingJeiRecipe> {
    private static final int WIDTH = 162;
    private static final int HEIGHT = 64;
    private static final int DISPLAY_AMOUNT = 1000;

    // 本体的炼药锅模型比例：小锅分支用 12，大锅分支才用 4。
    private static final float MODEL_SCALE = 12.0F;

    // 与本体 AbstractLiquidCategory 的小锅布局一致：
    // 铁砧在上、锅在中、处理方块（这里是搅拌机）紧贴锅下方，箭头分列两侧。
    private static final int ANVIL_Y = 12;
    private static final int CAULDRON_Y = 30;
    private static final int BLENDER_Y = 40;
    private static final int ARROW_IN_X = 54;
    private static final int ARROW_OUT_X = 92;
    private static final int ARROW_Y = 22;

    private static final Component BLENDER_ACTIVE = Component.translatable(
        "gui.anvilcraft_pigeon_plus.category.potion_brewing.need_blender"
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
    private final BlockState blender;

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
        this.blender = AddonBlocks.BLENDER.getDefaultState()
            .setValue(BlenderBlock.HEAD, BlenderBlock.BlenderHead.BLAZE)
            .setValue(BlenderBlock.WORKING, true);
    }

    @Override
    public RecipeType<PotionBrewingJeiRecipe> getRecipeType() {
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
        // 试剂只有一个（折叠的粒度就是试剂），所以物品槽始终是单格。
        builder.addSlot(RecipeIngredientRole.INPUT, JeiSlotUtil.INPUT_X, JeiSlotUtil.ITEM_Y)
            .addItemStack(recipe.reagent());
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
        // 输入与输出按索引一一对应（红石：迅捷→长效迅捷，力量→长效力量……）。
        // 联动让两边同步切换，避免出现「力量 → 长效迅捷」这种错配。

        if (recipe.inputs().size() > 1
            && recipe.outputs().size() == recipe.inputs().size()) {
            builder.createFocusLink(inputSlot, outputSlot);
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
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            ANVIL_Y + anvilYOffset,
            20,
            MODEL_SCALE,
            RenderSupport.SINGLE_BLOCK
        );
        RenderSupport.renderBlock(
            guiGraphics,
            this.cauldron,
            81,
            CAULDRON_Y,
            10,
            MODEL_SCALE,
            RenderSupport.SINGLE_BLOCK
        );
        RenderSupport.renderBlock(
            guiGraphics,
            this.blender,
            81,
            BLENDER_Y,
            0,
            MODEL_SCALE,
            RenderSupport.SINGLE_BLOCK
        );
        this.arrowIn.draw(guiGraphics, ARROW_IN_X, ARROW_Y);
        this.arrowOut.draw(guiGraphics, ARROW_OUT_X, ARROW_Y);

        // 三个槽位都是单格，直接画；坐标与 setRecipe 里的 addSlot 对齐（绘制原点差 1 像素）。
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
        if (mouseX >= 72 && mouseX <= 90 && mouseY >= BLENDER_Y - 1 && mouseY <= BLENDER_Y + 18) {
            tooltip.add(AddonBlocks.BLENDER.get().getName());
            tooltip.add(BLENDER_ACTIVE);
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
        registration.addRecipeCatalyst(
            new ItemStack(Items.CAULDRON), AnvilCraftPigeonPlusJeiPlugin.POTION_BREWING);
        registration.addRecipeCatalyst(
            new ItemStack(ModBlocks.FISH_TANK), AnvilCraftPigeonPlusJeiPlugin.POTION_BREWING);
        registration.addRecipeCatalyst(
            new ItemStack(AddonBlocks.BLENDER), AnvilCraftPigeonPlusJeiPlugin.POTION_BREWING);
        registration.addRecipeCatalyst(
            new ItemStack(AddonBlocks.ELECTRIC_BLENDER), AnvilCraftPigeonPlusJeiPlugin.POTION_BREWING);
    }

}
