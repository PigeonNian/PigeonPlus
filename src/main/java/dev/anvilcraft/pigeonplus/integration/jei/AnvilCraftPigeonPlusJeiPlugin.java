package dev.anvilcraft.pigeonplus.integration.jei;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.integration.jei.category.BlendingCategory;
import dev.anvilcraft.pigeonplus.integration.jei.category.FermentationCategory;
import dev.anvilcraft.pigeonplus.integration.jei.category.GasLiquefactionCategory;
import dev.anvilcraft.pigeonplus.integration.jei.category.PotionBrewingCategory;
import dev.anvilcraft.pigeonplus.integration.jei.category.PotionBulkBrewingCategory;
import dev.anvilcraft.pigeonplus.integration.jei.category.PotionSoakingCategory;
import dev.anvilcraft.pigeonplus.integration.jei.recipe.PotionBrewingJeiRecipe;
import dev.anvilcraft.pigeonplus.integration.jei.recipe.PotionSoakingJeiRecipe;
import dev.anvilcraft.pigeonplus.recipe.FermentationRecipe;
import dev.anvilcraft.pigeonplus.recipe.GasLiquefactionRecipe;
import dev.anvilcraft.pigeonplus.recipe.anvil.wrap.BlendingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public class AnvilCraftPigeonPlusJeiPlugin implements IModPlugin {
    public static final RecipeType<RecipeHolder<BlendingRecipe>> BLENDING =
        RecipeType.createRecipeHolderType(AnvilCraftPigeonPlus.of("blending"));
    public static final RecipeType<RecipeHolder<GasLiquefactionRecipe>> GAS_LIQUEFACTION =
        RecipeType.createRecipeHolderType(AnvilCraftPigeonPlus.of("gas_liquefaction"));
    public static final RecipeType<PotionBrewingJeiRecipe> POTION_BREWING =
        RecipeType.create(AnvilCraftPigeonPlus.MOD_ID, "potion_brewing", PotionBrewingJeiRecipe.class);
    public static final RecipeType<PotionBrewingJeiRecipe> POTION_BULK_BREWING =
        RecipeType.create(AnvilCraftPigeonPlus.MOD_ID, "potion_bulk_brewing", PotionBrewingJeiRecipe.class);
    public static final RecipeType<PotionSoakingJeiRecipe> POTION_SOAKING =
        RecipeType.create(AnvilCraftPigeonPlus.MOD_ID, "potion_soaking", PotionSoakingJeiRecipe.class);
    public static final RecipeType<RecipeHolder<FermentationRecipe>> FERMENTATION =
        RecipeType.createRecipeHolderType(AnvilCraftPigeonPlus.of("fermentation"));

    @Override
    public ResourceLocation getPluginUid() {
        return AnvilCraftPigeonPlus.of("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers jeiHelpers = registration.getJeiHelpers();
        IGuiHelper guiHelper = jeiHelpers.getGuiHelper();
        registration.addRecipeCategories(new BlendingCategory(guiHelper));
        registration.addRecipeCategories(new FermentationCategory(guiHelper));
        registration.addRecipeCategories(new GasLiquefactionCategory(guiHelper));
        registration.addRecipeCategories(new PotionBrewingCategory(guiHelper));
        registration.addRecipeCategories(new PotionBulkBrewingCategory(guiHelper));
        registration.addRecipeCategories(new PotionSoakingCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        BlendingCategory.registerRecipes(registration);
        FermentationCategory.registerRecipes(registration);
        GasLiquefactionCategory.registerRecipes(registration);
        PotionBrewingCategory.registerRecipes(registration);
        PotionBulkBrewingCategory.registerRecipes(registration);
        PotionSoakingCategory.registerRecipes(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        BlendingCategory.registerRecipeCatalysts(registration);
        FermentationCategory.registerRecipeCatalysts(registration);
        GasLiquefactionCategory.registerRecipeCatalysts(registration);
        PotionBrewingCategory.registerRecipeCatalysts(registration);
        PotionBulkBrewingCategory.registerRecipeCatalysts(registration);
        PotionSoakingCategory.registerRecipeCatalysts(registration);
    }
}
