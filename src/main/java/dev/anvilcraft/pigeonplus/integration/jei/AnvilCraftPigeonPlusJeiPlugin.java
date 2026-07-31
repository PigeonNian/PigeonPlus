package dev.anvilcraft.pigeonplus.integration.jei;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.integration.jei.category.BlendingCategory;
import dev.anvilcraft.pigeonplus.integration.jei.category.GasLiquefactionCategory;
import dev.anvilcraft.pigeonplus.integration.jei.recipe.GasLiquefactionJeiRecipe;
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
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class AnvilCraftPigeonPlusJeiPlugin implements IModPlugin {
    public static final RecipeType<RecipeHolder<BlendingRecipe>> BLENDING =
        RecipeType.createRecipeHolderType(AnvilCraftPigeonPlus.of("blending"));
    public static final RecipeType<GasLiquefactionJeiRecipe> GAS_LIQUEFACTION =
        RecipeType.create(AnvilCraftPigeonPlus.MOD_ID, "gas_liquefaction", GasLiquefactionJeiRecipe.class);

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return AnvilCraftPigeonPlus.of("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers jeiHelpers = registration.getJeiHelpers();
        IGuiHelper guiHelper = jeiHelpers.getGuiHelper();
        registration.addRecipeCategories(new BlendingCategory(guiHelper));
        registration.addRecipeCategories(new GasLiquefactionCategory(guiHelper));
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        BlendingCategory.registerRecipes(registration);
        GasLiquefactionCategory.registerRecipes(registration);
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        BlendingCategory.registerRecipeCatalysts(registration);
        GasLiquefactionCategory.registerRecipeCatalysts(registration);
    }
}
