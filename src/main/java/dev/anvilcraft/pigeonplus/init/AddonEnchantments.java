package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

public class AddonEnchantments {

    /**
     * 铁拳：专属铁砧锤的附魔，使铁砧锤化为纯粹的近战武器。
     *
     * <p>该附魔本身不提供任何数值效果，其作用完全由
     * {@code dev.anvilcraft.pigeonplus.mixin.AnvilHammerItemMixin} 实现——当铁砧锤带有本附魔时，
     * 剥离旋转方块、拆除方块、便携铁砧、火箭跳与护目镜等“扳手”类功能。
     */
    public static final ResourceKey<Enchantment> DOOMFIST_KEY =
        ResourceKey.create(Registries.ENCHANTMENT, AnvilCraftPigeonPlus.of("doomfist"));

    /**
     * 附魔注册
     *
     * @param context 引导上下文
     */
    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<Item> itemHolderGetter = context.lookup(Registries.ITEM);
        context.register(
            DOOMFIST_KEY,
            Enchantment.enchantment(
                Enchantment.definition(
                    itemHolderGetter.getOrThrow(ModItemTags.ANVIL_HAMMER),
                    1,
                    1,
                    Enchantment.constantCost(25),
                    Enchantment.constantCost(50),
                    8,
                    EquipmentSlotGroup.MAINHAND
                )
            ).build(DOOMFIST_KEY.location())
        );
    }
}
