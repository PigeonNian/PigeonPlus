package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.recipe.GasLiquefactionRecipe;
import dev.anvilcraft.pigeonplus.recipe.anvil.wrap.BlendingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AddonRecipeTypes {
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(Registries.RECIPE_TYPE, AnvilCraftPigeonPlus.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, AnvilCraftPigeonPlus.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BlendingRecipe>> BLENDING_TYPE =
        registerType("blending");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlendingRecipe>> BLENDING_SERIALIZER =
        RECIPE_SERIALIZERS.register("blending", BlendingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<GasLiquefactionRecipe>> GAS_LIQUEFACTION_TYPE =
        registerType("gas_liquefaction");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GasLiquefactionRecipe>> GAS_LIQUEFACTION_SERIALIZER =
        RECIPE_SERIALIZERS.register("gas_liquefaction", GasLiquefactionRecipe.Serializer::new);

    private static <T extends Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> registerType(String name) {
        return RECIPE_TYPES.register(
            name, () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return AnvilCraftPigeonPlus.of(name).toString();
                }
            }
        );
    }

    public static void register(IEventBus bus) {
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }
}
