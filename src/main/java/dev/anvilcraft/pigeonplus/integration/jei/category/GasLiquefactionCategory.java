package dev.anvilcraft.pigeonplus.integration.jei.category;

import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.anvilcraft.pigeonplus.init.AddonItems;
import dev.anvilcraft.pigeonplus.integration.jei.AnvilCraftPigeonPlusJeiPlugin;
import dev.anvilcraft.pigeonplus.integration.jei.recipe.GasLiquefactionJeiRecipe;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GasLiquefactionCategory implements IRecipeCategory<GasLiquefactionJeiRecipe> {
    private static final int WIDTH = 162;
    private static final int HEIGHT = 72;
    private static final int GAS_X = 8;
    private static final int FLUID_Y = 30;
    private static final int LIQUID_X = 138;
    private static final int LEFT_ARROW_X = 40;
    private static final int LEFT_ARROW_Y = 30;
    private static final float TANK_SCALE = 1.6f;

    private final Component title;
    private final IDrawable icon;
    private final IDrawable pump;
    private final IDrawable slotDefault;
    private final IDrawable arrowDefault;

    public GasLiquefactionCategory(IGuiHelper helper) {
        this.title = Component.translatable("gui.anvilcraft_pigeon_plus.category.gas_liquefaction");
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.LARGE_FLUID_TANK));
        this.pump = helper.createDrawableItemStack(new ItemStack(ModBlocks.PUMP));
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.arrowDefault = JeiRenderHelper.getArrowDefault(helper);
    }

    @Override
    @NotNull
    public  RecipeType<GasLiquefactionJeiRecipe> getRecipeType() {
        return AnvilCraftPigeonPlusJeiPlugin.GAS_LIQUEFACTION;
    }

    @Override
    public Component getTitle() {
        return this.title;
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
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    @NotNull
    public void setRecipe(IRecipeLayoutBuilder builder, GasLiquefactionJeiRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, GAS_X, FLUID_Y)
            .setFluidRenderer(recipe.gasAmount(), false, 16, 16)
            .addFluidStack(recipe.gas(), recipe.gasAmount());
        builder.addSlot(RecipeIngredientRole.OUTPUT, LIQUID_X, FLUID_Y)
            .setFluidRenderer(recipe.liquidAmount(), false, 16, 16)
            .addFluidStack(recipe.liquid(), recipe.liquidAmount());
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
            .addItemStack(bucketFor(recipe.liquid()));
    }

    @Override
    public void draw(
        GasLiquefactionJeiRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        this.slotDefault.draw(guiGraphics, GAS_X - 1, FLUID_Y - 1);
        this.slotDefault.draw(guiGraphics, LIQUID_X - 1, FLUID_Y - 1);
        this.arrowDefault.draw(guiGraphics, LEFT_ARROW_X, LEFT_ARROW_Y);
        this.arrowDefault.draw(guiGraphics, 108, 30);
        this.pump.draw(guiGraphics, LEFT_ARROW_X , 40);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((WIDTH - 16 * TANK_SCALE) / 2.0f, 25, 0);
        guiGraphics.pose().scale(TANK_SCALE, TANK_SCALE, 1.0f);
        guiGraphics.renderItem(new ItemStack(ModBlocks.LARGE_FLUID_TANK), 0, 0);
        guiGraphics.pose().popPose();
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            Component.translatable("gui.anvilcraft_pigeon_plus.gas_liquefaction.fill_then"),
            69,
            7,
            0xFF404040,
            false
        );
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            Component.translatable("gui.anvilcraft_pigeon_plus.gas_liquefaction.liquefy"),
            108,
            55,
            0xFF404040,
            false
        );
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            Component.translatable("gui.anvilcraft_pigeon_plus.gas_liquefaction.keep_pumping"),
            32,
            55,
            0xFF404040,
            false
        );
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AnvilCraftPigeonPlusJeiPlugin.GAS_LIQUEFACTION, GasLiquefactionJeiRecipe.recipes());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(AddonBlocks.ANVIL_PUMP), AnvilCraftPigeonPlusJeiPlugin.GAS_LIQUEFACTION);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.PUMP), AnvilCraftPigeonPlusJeiPlugin.GAS_LIQUEFACTION);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.LARGE_FLUID_TANK), AnvilCraftPigeonPlusJeiPlugin.GAS_LIQUEFACTION);
    }

    private static ItemStack bucketFor(Fluid fluid) {
        if (fluid.isSame(AddonFluids.GASEOUS_BIOGAS.get())) {
            return new ItemStack(AddonItems.GASEOUS_BIOGAS_BUCKET.get());
        }
        if (fluid.isSame(AddonFluids.COMPRESSED_AIR.get())) {
            return new ItemStack(AddonItems.COMPRESSED_AIR_BUCKET.get());
        }
        if (fluid.isSame(AddonFluids.LIQUEFIED_BIOGAS.get())) {
            return new ItemStack(AddonItems.LIQUEFIED_BIOGAS_BUCKET.get());
        }
        if (fluid.isSame(AddonFluids.LIQUID_OXYGEN.get())) {
            return new ItemStack(AddonItems.LIQUID_OXYGEN_BUCKET.get());
        }
        return ItemStack.EMPTY;
    }
}
