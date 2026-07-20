package dev.anvilcraft.pigeonplus.integration.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
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
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BlendingCategory extends AbstractProgressCategory<BlendingRecipe> {
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
        JeiSlotUtil.addInputSlots(builder, recipe.getInputItems());
        if (!recipe.getResultItems().isEmpty()) {
            JeiSlotUtil.addOutputSlots(builder, recipe.getResultItems());
        }

        HasCauldronSimple hasCauldron = recipe.getHasCauldron();
        if (HasCauldron.isNotEmpty(hasCauldron.fluid())) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
                .addFluidStack(BuiltInRegistries.FLUID.get(hasCauldron.fluid()));
        }
        if (HasCauldron.isNotEmpty(hasCauldron.transform())) {
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                .addFluidStack(BuiltInRegistries.FLUID.get(hasCauldron.transform()));
        }
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

        JeiSlotUtil.drawInputSlots(guiGraphics, slotDefault, recipe.getInputItems().size());
        if (!recipe.getResultItems().isEmpty()) {
            if (JeiRecipeUtil.isChance(recipe.getResultItems())) {
                JeiSlotUtil.drawOutputSlots(guiGraphics, slotProbability, recipe.getResultItems().size());
            } else {
                JeiSlotUtil.drawOutputSlots(guiGraphics, slotDefault, recipe.getResultItems().size());
            }
        } else if (recipe.isProduceFluid()) {
            Block result = recipe.getHasCauldron().getTransformCauldron();
            BlockState state = CauldronUtil.getStateFromContentAndLevel(result, 1);
            RenderSupport.renderBlock(guiGraphics, state, 133, 30, 0, 12, RenderSupport.SINGLE_BLOCK);
        }

        HasCauldronSimple hasCauldron = recipe.getHasCauldron();
        if (recipe.isProduceFluid()) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.scale(0.8f, 0.8f, 1.0f);
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable(
                    "gui.anvilcraft.category.solid_liquid.produce_fluid",
                    hasCauldron.produce(),
                    hasCauldron.getTransformCauldron().getName()
                ),
                0,
                70,
                0xFF000000,
                false
            );
            pose.popPose();
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
        if (mouseX >= 124 && mouseX <= 140 && mouseY >= 24 && mouseY <= 42 && recipe.getResultItems().isEmpty()) {
            tooltip.add(recipe.getHasCauldron().getTransformCauldron().getName());
        }
    }

    private BlockState getDisplayedInputCauldron(BlendingRecipe recipe) {
        if (recipe.isProduceFluid() && !recipe.isConsumeFluid()) {
            return Blocks.CAULDRON.defaultBlockState();
        }
        return CauldronUtil.fullState(recipe.getHasCauldron().getFluidCauldron());
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
