package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.pigeonplus.init.AddonEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

public final class DoomfistEnchantmentUtil {
    private DoomfistEnchantmentUtil() {
    }

    /**
     * 判断物品是否带有铁拳附魔。
     *
     * <p>这里直接遍历物品堆栈的附魔条目并按 {@link ResourceKey} 比对，而不是走
     * {@code EnchantmentHelper.getItemEnchantmentLevel}——后者需要一个已经解析好的
     * {@link Holder}，在附魔未被数据包加载或物品在别的注册表环境下时容易拿不到。
     */
    public static boolean hasDoomfist(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemEnchantments enchantments = stack.getTagEnchantments();
        if (enchantments.isEmpty()) return false;
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (isDoomfist(holder)) return true;
        }
        return false;
    }

    /**
     * 判断实体主手或副手是否持有带铁拳附魔的铁砧锤。
     */
    public static boolean isWieldingDoomfist(@Nullable LivingEntity entity) {
        if (entity == null) return false;
        return hasDoomfist(entity.getMainHandItem()) || hasDoomfist(entity.getOffhandItem());
    }

    private static boolean isDoomfist(Holder<Enchantment> holder) {
        return holder.unwrapKey().filter(AddonEnchantments.DOOMFIST_KEY::equals).isPresent();
    }
}
