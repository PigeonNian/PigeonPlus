package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * 本模组的有序合成配方。
 *
 * <h3>26.1 的写法变化</h3>
 * <ul>
 *   <li>{@code ShapedRecipeBuilder.shaped(...)} 的静态版本现在第一个参数是
 *       {@code HolderGetter<Item>}（原版把「物品解析」与「配方构建」拆开了）。
 *       {@code RegistrumRecipeProvider} 暴露了 {@code getItems()}，
 *       从它取即可。</li>
 *   <li>{@code save(...)} 现在必须显式给出配方 id，且类型从
 *       {@code Identifier} 换成了 {@code ResourceKey<Recipe<?>>}；
 *       provider 的 {@code safeKey(ItemLike)} 会按产物自动推导出规范 id，
 *       与原版一致。</li>
 * </ul>
 */
public class AddonShapedRecipeLoader {
    public AddonShapedRecipeLoader(RegistrumRecipeProvider provider) {
        this.blender(provider);
        this.stasisBeacon(provider);
        this.anvilPump(provider);
        this.feedSpreader(provider);
        this.pigeonAnvil(provider);
    }

    private void blender(RegistrumRecipeProvider provider) {
        pigeonplus$shaped(provider, AddonBlocks.BLENDER)
            .pattern(" S ")
            .pattern("PTI")
            .pattern("III")
            .define('S', Items.IRON_SHOVEL)
            .define('P', ModBlocks.PIPE_STRAIGHT)
            .define('T', ModBlocks.FLUID_TANK)
            .define('I', Items.IRON_INGOT)
            .unlockedBy("has_iron_shovel", provider.has(Items.IRON_SHOVEL))
            .unlockedBy("has_pipe_straight", provider.has(ModBlocks.PIPE_STRAIGHT))
            .unlockedBy("has_fluid_tank", provider.has(ModBlocks.FLUID_TANK))
            .unlockedBy("has_iron_ingot", provider.has(Items.IRON_INGOT))
            .save(provider, provider.safeKey(AddonBlocks.BLENDER));
    }

    private void stasisBeacon(RegistrumRecipeProvider provider) {
        pigeonplus$shaped(provider, AddonBlocks.STASIS_BEACON)
            .pattern("AAA")
            .pattern("ACA")
            .pattern("ABA")
            .define('A', ModItems.FROST_METAL_INGOT)
            .define('B', Items.EMERALD_BLOCK)
            .define('C', ModBlocks.CORRUPTED_BEACON)
            .unlockedBy("has_frost_metal_ingot", provider.has(ModItems.FROST_METAL_INGOT))
            .unlockedBy("has_emerald_block", provider.has(Items.EMERALD_BLOCK))
            .unlockedBy("has_corrupted_beacon", provider.has(ModBlocks.CORRUPTED_BEACON))
            .save(provider, provider.safeKey(AddonBlocks.STASIS_BEACON));
    }

    private void anvilPump(RegistrumRecipeProvider provider) {
        pigeonplus$shaped(provider, AddonBlocks.ANVIL_PUMP)
            .pattern("A")
            .pattern("B")
            .pattern("C")
            .define('A', Items.PISTON)
            .define('B', Items.ANVIL)
            .define('C', ModItems.PIPE)
            .unlockedBy("has_piston", provider.has(Items.PISTON))
            .unlockedBy("has_anvil", provider.has(Items.ANVIL))
            .unlockedBy("has_pipe", provider.has(ModItems.PIPE))
            .save(provider, provider.safeKey(AddonBlocks.ANVIL_PUMP));
    }

    private void feedSpreader(RegistrumRecipeProvider provider) {
        pigeonplus$shaped(provider, AddonBlocks.FEED_SPREADER)
            .pattern(" A ")
            .pattern(" B ")
            .pattern("CCC")
            .define('A', Items.PISTON)
            .define('B', Items.BUCKET)
            .define('C', ModBlocks.POLISHED_HEAVY_IRON_SLAB)
            .unlockedBy("has_piston", provider.has(Items.PISTON))
            .unlockedBy("has_bucket", provider.has(Items.BUCKET))
            .unlockedBy("has_polished_heavy_iron_slab", provider.has(ModBlocks.POLISHED_HEAVY_IRON_SLAB))
            .save(provider, provider.safeKey(AddonBlocks.FEED_SPREADER));
    }

    private void pigeonAnvil(RegistrumRecipeProvider provider) {
        pigeonplus$shaped(provider, AddonBlocks.PIGEON_ANVIL)
            .pattern("FIF")
            .pattern("IAI")
            .pattern("FIF")
            .define('F', Items.FEATHER)
            .define('I', Items.IRON_INGOT)
            .define('A', Items.ANVIL)
            .unlockedBy("has_anvil", provider.has(Items.ANVIL))
            .unlockedBy("has_feather", provider.has(Items.FEATHER))
            .unlockedBy("has_iron_ingot", provider.has(Items.IRON_INGOT))
            .save(provider, provider.safeKey(AddonBlocks.PIGEON_ANVIL));
    }

    /** 统一入口：把 provider 的物品解析器喂给原版的构建器。 */
    private static ShapedRecipeBuilder pigeonplus$shaped(RegistrumRecipeProvider provider, ItemLike result) {
        return ShapedRecipeBuilder.shaped(provider.getItems(), RecipeCategory.MISC, result);
    }
}
