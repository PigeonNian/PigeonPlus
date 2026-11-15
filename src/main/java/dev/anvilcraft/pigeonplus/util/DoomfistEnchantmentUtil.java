package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.pigeonplus.init.AddonEnchantments;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
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
     * 判断某个附魔实例是否就是铁拳。按 {@link ResourceKey} 比对，不依赖 Holder 解析。
     */
    public static boolean isDoomfist(Holder<Enchantment> holder) {
        return holder.unwrapKey().filter(AddonEnchantments.DOOMFIST_KEY::equals).isPresent();
    }

    /**
     * 判断物品是否仍然是「带铁拳的铁砧锤」。
     *
     * <p>铁拳只属于铁砧锤：砂轮转移、命令、其它模组的附魔转移都可能把这条附魔挪到别的东西上，
     * 而 {@link #hasDoomfist} 只认附魔本身。技能与「剥离扳手功能」都必须走这个更严格的判断，
     * 否则剑、头盔之类拿到铁拳后能白嫖整套技能（还不用付出失去工具功能的代价）。
     */
    public static boolean isDoomfistHammer(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItemTags.ANVIL_HAMMER) && hasDoomfist(stack);
    }

    /**
     * 判断实体主手或副手是否持有带铁拳附魔的铁砧锤。
     */
    public static boolean isWieldingDoomfist(@Nullable LivingEntity entity) {
        if (entity == null) return false;
        return isDoomfistHammer(entity.getMainHandItem()) || isDoomfistHammer(entity.getOffhandItem());
    }
}
