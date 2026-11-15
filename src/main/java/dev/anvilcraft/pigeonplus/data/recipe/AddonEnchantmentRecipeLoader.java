package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonEnchantments;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
import dev.dubhe.anvilcraft.util.FluidStackPredicate;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 铁拳液态魔咒的获取配方。
 *
 * <p>思路沿用 AnvilCraft 本体给丝触、经验修补、余烬熔炼等魔咒准备的那套「固液转化」：
 * 炼药锅里放空白液态魔咒，把主题材料丢在锅上，铁砧砸下来，空白液体被就地转换成
 * 对应魔咒的液态魔咒。铁拳不进入任何原版附魔标签（附魔台/交易/战利品/怪物装备），
 * 因此这条反应就是它唯一的来源。
 *
 * <p>数字：1 mB 空白 → 1 mB 铁拳，正好够自动附魔台给一把铁砧锤附 1 级。
 * 大型炼药锅对每种流体单独占一格，消耗量不必等于锅里总量，但保持 1:1 最省心。
 */
public final class AddonEnchantmentRecipeLoader {
    private AddonEnchantmentRecipeLoader() {
    }

    public static void init(RegistrumRecipeProvider provider) {
        FluidStack doomfist = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 1);
        doomfist.set(ModComponents.LIQUID_ENCHANTMENT, AddonEnchantments.DOOMFIST_KEY);

        SolidLiquidRecipe.builder()
            .cauldron(
                FluidStackPredicate.builder()
                    .fluid(ModFluids.LIQUID_ENCHANTMENT)
                    // 只认空白液态魔咒：已经带魔咒的液体（如经验修补）不能被这套材料顶替。
                    .component(builder -> builder.expectNull(ModComponents.LIQUID_ENCHANTMENT))
                    .build()
            )
            .consume(1)
            .requires(Items.ANVIL)
            .requires(Items.IRON_BLOCK, 4)
            .transform(doomfist)
            .save(provider, AnvilCraftPigeonPlus.of("solid_liquid/doomfist"));
    }
}
