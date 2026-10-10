package dev.anvilcraft.pigeonplus.fluid;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PotionFluidUtil {
    private PotionFluidUtil() {
    }

    public static boolean isPotion(FluidStack stack) {
        return stack.getFluid() instanceof PotionFluid
            && stack.get(DataComponents.POTION_CONTENTS) != null;
    }

    @Nullable
    public static PotionContents getContents(FluidStack stack) {
        return stack.getFluid() instanceof PotionFluid
            ? stack.get(DataComponents.POTION_CONTENTS)
            : null;
    }

    public static FluidStack withContents(FluidStack stack, PotionContents contents) {
        stack.set(DataComponents.POTION_CONTENTS, contents);
        return stack;
    }

    public static List<FluidStack> allPotions(@Nullable ResourceHandler<FluidResource> fluids) {
        if (fluids == null) {
            return List.of();
        }
        Map<PotionContents, FluidStack> found = new LinkedHashMap<>();
        for (int tank = 0; tank < fluids.size(); tank++) {
            FluidStack stack = fluids.getResource(tank).toStack(fluids.getAmountAsInt(tank));
            if (stack.isEmpty()) {
                continue;
            }
            PotionContents contents = getContents(stack);
            if (contents != null) {
                found.putIfAbsent(contents, stack.copy());
            }
        }
        return List.copyOf(found.values());
    }
}
