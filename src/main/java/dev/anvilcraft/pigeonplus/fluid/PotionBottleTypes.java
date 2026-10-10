package dev.anvilcraft.pigeonplus.fluid;

import dev.anvilcraft.pigeonplus.init.AddonItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public final class PotionBottleTypes {
    private PotionBottleTypes() {
    }

    @Nullable
    public static Item filledFor(ItemStack bottle) {
        if (bottle.is(Items.GLASS_BOTTLE)) {
            return Items.POTION;
        }
        if (bottle.is(AddonItems.SPLASH_BOTTLE.get())) {
            return Items.SPLASH_POTION;
        }
        if (bottle.is(AddonItems.LINGERING_BOTTLE.get())) {
            return Items.LINGERING_POTION;
        }
        return null;
    }

    public static boolean isPotion(ItemStack stack) {
        return stack.is(Items.POTION)
            || stack.is(Items.SPLASH_POTION)
            || stack.is(Items.LINGERING_POTION);
    }

    public static ItemStack emptyFor(ItemStack potion) {
        if (potion.is(Items.SPLASH_POTION)) {
            return new ItemStack(AddonItems.SPLASH_BOTTLE.get());
        }
        if (potion.is(Items.LINGERING_POTION)) {
            return new ItemStack(AddonItems.LINGERING_BOTTLE.get());
        }
        return new ItemStack(Items.GLASS_BOTTLE);
    }
}
