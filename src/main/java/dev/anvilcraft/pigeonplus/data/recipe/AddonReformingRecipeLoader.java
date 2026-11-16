package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.anvilcraft.pigeonplus.recipe.ReformingRecipe;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/**
 * 蒸汽重整配方。
 *
 * <p>现实链路是「天然气 + 蒸汽 → 合成气 → 变换 → 氢气」，本模组没有 CO / CO₂ 流体，
 * 因此把重整与变换合并为净反应 CH₄ + 2 H₂O → CO₂↑ + 4 H₂，二氧化碳视为废气排掉。
 *
 * <p>产气量按体积换算：1 mB 液化沼气约等于 512 mB 甲烷气（与本体 512:1 的液化比一致），
 * 完全重整产出 4 倍体积的氢气，即 2048 mB。
 */
public final class AddonReformingRecipeLoader {
    private AddonReformingRecipeLoader() {
    }

    public static void init(RegistrumRecipeProvider provider) {
        provider.accept(
            AnvilCraftPigeonPlus.of("reforming/liquefied_biogas_to_hydrogen"),
            new ReformingRecipe(
                List.of(
                    new FluidStack(AddonFluids.LIQUEFIED_BIOGAS.get(), 1),
                    new FluidStack(Fluids.WATER, 2)
                ),
                new FluidStack(ModFluids.HYDROGEN.get(), 2048)
            ),
            null
        );
    }
}
