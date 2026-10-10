package dev.anvilcraft.pigeonplus.fluid;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jspecify.annotations.NonNull;

public class PotionFluidType extends FluidType {
    private static final String FLAVORED_KEY = "fluid.anvilcraft_pigeon_plus.potion.of";
    private static final String LONG_KEY = "fluid.anvilcraft_pigeon_plus.potion.long";
    private static final String STRONG_KEY = "fluid.anvilcraft_pigeon_plus.potion.strong";

    private static final String POTION_NAME_PREFIX = "item.minecraft.potion.effect.";

    public PotionFluidType(FluidType.Properties properties) {
        super(properties);
    }

    @Override
    public @NonNull Component getDescription(FluidStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || contents.potion().isEmpty()) {
            return super.getDescription(stack);
        }
        Component base = contents.getName(POTION_NAME_PREFIX);
        return Component.translatable(FLAVORED_KEY, variantPrefix(contents.potion().get()), base);
    }

    private static Component variantPrefix(Holder<Potion> potion) {
        if (PotionVariants.isLong(potion)) {
            return Component.translatable(LONG_KEY);
        }
        if (PotionVariants.isStrong(potion)) {
            return Component.translatable(STRONG_KEY);
        }
        return Component.empty();
    }
}
