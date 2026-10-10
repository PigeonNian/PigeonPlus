package dev.anvilcraft.pigeonplus.fluid;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.alchemy.Potion;

public final class PotionVariants {
    public static final String LONG_PREFIX = "long_";
    public static final String STRONG_PREFIX = "strong_";

    private PotionVariants() {
    }

    public static String path(Holder<Potion> potion) {
        return potion.unwrapKey()
            .map(ResourceKey::identifier)
            .map(Identifier::getPath)
            .orElse("");
    }

    public static boolean isLong(Holder<Potion> potion) {
        return path(potion).startsWith(LONG_PREFIX);
    }

    public static boolean isStrong(Holder<Potion> potion) {
        return path(potion).startsWith(STRONG_PREFIX);
    }

    public static boolean isEnhanced(Holder<Potion> potion) {
        return isLong(potion) || isStrong(potion);
    }

    public static String basePath(Holder<Potion> potion) {
        String path = path(potion);
        if (path.startsWith(LONG_PREFIX)) {
            return path.substring(LONG_PREFIX.length());
        }
        if (path.startsWith(STRONG_PREFIX)) {
            return path.substring(STRONG_PREFIX.length());
        }
        return path;
    }

    public static boolean isEnhancementOf(Holder<Potion> result, Holder<Potion> input) {
        return isEnhanced(result) && basePath(result).equals(basePath(input));
    }
}
