package dev.anvilcraft.pigeonplus.integration.jei;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.integration.jei.category.BlendingCategory;
import dev.anvilcraft.pigeonplus.integration.jei.category.GasLiquefactionCategory;
import dev.anvilcraft.pigeonplus.integration.jei.category.PotionBrewingCategory;
import dev.anvilcraft.pigeonplus.integration.jei.category.PotionBulkBrewingCategory;
import dev.anvilcraft.pigeonplus.integration.jei.category.PotionSoakingCategory;
import dev.anvilcraft.pigeonplus.integration.jei.recipe.PotionBrewingJeiRecipe;
import dev.anvilcraft.pigeonplus.integration.jei.recipe.PotionSoakingJeiRecipe;
import dev.anvilcraft.pigeonplus.recipe.GasLiquefactionRecipe;
import dev.anvilcraft.pigeonplus.recipe.anvil.wrap.BlendingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class AnvilCraftPigeonPlusJeiPlugin implements IModPlugin {
    public static final IRecipeHolderType<BlendingRecipe> BLENDING =
        IRecipeHolderType.create(AnvilCraftPigeonPlus.of("blending"));
    public static final IRecipeHolderType<GasLiquefactionRecipe> GAS_LIQUEFACTION =
        IRecipeHolderType.create(AnvilCraftPigeonPlus.of("gas_liquefaction"));
    public static final IRecipeType<PotionBrewingJeiRecipe> POTION_BREWING =
        IRecipeType.create(AnvilCraftPigeonPlus.of("potion_brewing"), PotionBrewingJeiRecipe.class);
    public static final IRecipeType<PotionBrewingJeiRecipe> POTION_BULK_BREWING =
        IRecipeType.create(AnvilCraftPigeonPlus.of("potion_bulk_brewing"), PotionBrewingJeiRecipe.class);
    public static final IRecipeType<PotionSoakingJeiRecipe> POTION_SOAKING =
        IRecipeType.create(AnvilCraftPigeonPlus.of("potion_soaking"), PotionSoakingJeiRecipe.class);

    @Override
    public @NotNull Identifier getPluginUid() {
        return AnvilCraftPigeonPlus.of("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers jeiHelpers = registration.getJeiHelpers();
        IGuiHelper guiHelper = jeiHelpers.getGuiHelper();
        registration.addRecipeCategories(new BlendingCategory(guiHelper));
        registration.addRecipeCategories(new GasLiquefactionCategory(guiHelper));
        registration.addRecipeCategories(new PotionBrewingCategory(guiHelper));
        registration.addRecipeCategories(new PotionBulkBrewingCategory(guiHelper));
        registration.addRecipeCategories(new PotionSoakingCategory(guiHelper));
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        BlendingCategory.registerRecipes(registration);
        GasLiquefactionCategory.registerRecipes(registration);
        PotionBrewingCategory.registerRecipes(registration);
        PotionBulkBrewingCategory.registerRecipes(registration);
        PotionSoakingCategory.registerRecipes(registration);
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        BlendingCategory.registerRecipeCatalysts(registration);
        GasLiquefactionCategory.registerRecipeCatalysts(registration);
        PotionBrewingCategory.registerRecipeCatalysts(registration);
        PotionBulkBrewingCategory.registerRecipeCatalysts(registration);
        PotionSoakingCategory.registerRecipeCatalysts(registration);
    }
}
