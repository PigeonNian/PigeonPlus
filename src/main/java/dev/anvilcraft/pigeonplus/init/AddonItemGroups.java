package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.dubhe.anvilcraft.init.item.ModItemGroups;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus.REGISTRUM;


public class AddonItemGroups {
    private static final DeferredRegister<CreativeModeTab> DEFERRED_REGISTER = DeferredRegister.create(
        Registries.CREATIVE_MODE_TAB,
        AnvilCraftPigeonPlus.MOD_ID
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ADDON_ITEMS = DEFERRED_REGISTER.register(
        "addon_items",
        () -> CreativeModeTab.builder()
            .icon(() -> AddonBlocks.NOZZLE.asStack())
            .displayItems((ctx, entries) -> addEnchantedBooks(ctx, entries))
            .title(
                REGISTRUM.addLang(
                    "itemGroup",
                    AnvilCraftPigeonPlus.of("addon_items"),
                    "AnvilCraft: Pigeon+"
                )
            )
            .withTabsBefore(ModItemGroups.ANVILCRAFT_ITEMS.getId())
            .build()
    );

    public static void register(IEventBus modEventBus) {
        DEFERRED_REGISTER.register(modEventBus);
    }

    /**
     * 把本模组的附魔书放进创造模式物品栏。
     *
     * <p>附魔本身是数据包注册表条目而非物品，{@code REGISTRUM.defaultCreativeTab} 的自动登记只
     * 覆盖物品，因此需要在这里手动构造一本附魔书。
     *
     * <p>26.1 移除了 {@code EnchantedBookItem}（连同它的
     * {@code createForEnchantment}）：附魔书现在就是普通的 {@code Items.ENCHANTED_BOOK}
     * 加上 {@code STORED_ENCHANTMENTS} 组件。所以这里手工构造
     * {@link ItemEnchantments} 再写进该组件——必须用
     * {@code STORED_ENCHANTMENTS} 而非 {@code ENCHANTMENTS}，
     * 否则书看起来是空白的。
     *
     * <p>附魔在创造模式物品栏构建时可能尚未同步（服务端未加载对应数据包），
     * 故用 {@code lookup} 而非 {@code getOrThrow}，拿不到就安静跳过，避免整页物品栏崩溃。
     */
    private static void addEnchantedBooks(
        CreativeModeTab.ItemDisplayParameters ctx, CreativeModeTab.Output entries
    ) {
        ctx.holders()
            .lookup(Registries.ENCHANTMENT)
            .flatMap(lookup -> lookup.get(AddonEnchantments.DOOMFIST_KEY))
            .ifPresent(holder -> {
                ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                enchantments.set(holder, 1);
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                book.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
                entries.accept(book);
            });
    }
}
