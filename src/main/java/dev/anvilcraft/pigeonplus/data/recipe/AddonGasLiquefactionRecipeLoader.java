package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.anvilcraft.pigeonplus.recipe.GasLiquefactionRecipe;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 气体液化配方。
 *
 * <h3>26.1 的写法变化</h3>
 * {@code RecipeOutput.accept} 的 id 参数从 {@code Identifier} 换成了
 * {@code ResourceKey<Recipe<?>>}，故统一走 provider 的
 * {@code safeKey(Identifier)} 转换。
 *
 * <p>原先还有一条「安山岩氧气 → 液氧」的配方，输入用的是 AnvilCraft 的
 * {@code ModFluids.OXYGEN}。26.1 的 AnvilCraft 已移除该流体，
 * 配方无法再构造，故删除。
 */
public class AddonGasLiquefactionRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        pigeonplus$accept(provider, "gas_liquefaction/compressed_air", new GasLiquefactionRecipe(
            new FluidStack(AddonFluids.COMPRESSED_AIR.get(), 415),
            new FluidStack(AddonFluids.LIQUID_OXYGEN.get(), 1)
        ));

        pigeonplus$accept(provider, "gas_liquefaction/compressed_air_hydrogen", new GasLiquefactionRecipe(
            new FluidStack(AddonFluids.COMPRESSED_AIR.get(), 830),
            new FluidStack(AddonFluids.LIQUID_HYDROGEN.get(), 1)
        ));

        pigeonplus$accept(provider, "gas_liquefaction/gaseous_biogas", new GasLiquefactionRecipe(
            new FluidStack(AddonFluids.GASEOUS_BIOGAS.get(), 512),
            new FluidStack(AddonFluids.LIQUEFIED_BIOGAS.get(), 1)
        ));
    }

    private static void pigeonplus$accept(
        RegistrumRecipeProvider provider, String path, GasLiquefactionRecipe recipe
    ) {
        Identifier id = AnvilCraftPigeonPlus.of(path);
        provider.accept(provider.safeKey(id), recipe, null);
    }
}
