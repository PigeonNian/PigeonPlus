package dev.anvilcraft.pigeonplus.fluid;

import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.List;
import java.util.function.Predicate;

public final class PotionBrewingHelper {
    private PotionBrewingHelper() {
    }

    public static PotionContents resolveContents(FluidStack stored) {
        if (stored.getFluid() instanceof PotionFluid) {
            return PotionFluidUtil.getContents(stored);
        }
        if (stored.is(Fluids.WATER)) {
            return new PotionContents(Potions.WATER);
        }
        return null;
    }

    public static Selection select(
        PotionBrewing brewing,
        PotionContents contents,
        ResourceHandler<ItemResource> input
    ) {
        ItemStack potionItem = new ItemStack(Items.POTION);
        potionItem.set(DataComponents.POTION_CONTENTS, contents);
        int chosenSlot = -1;
        int chosenTier = Integer.MAX_VALUE;
        PotionContents chosenResult = null;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack ingredient = input.getResource(slot).toStack(input.getAmountAsInt(slot));
            if (ingredient.isEmpty()) {
                continue;
            }
            if (!brewing.hasMix(potionItem, ingredient)) {
                continue;
            }
            ItemStack brewed = brewing.mix(ingredient, potionItem);
            if (!brewed.is(potionItem.getItem())) {
                continue;
            }
            PotionContents result = brewed.get(DataComponents.POTION_CONTENTS);
            if (result == null || result.potion().isEmpty()) {
                continue;
            }
            if (contents.potion().isPresent() && result.potion().get().is(contents.potion().get())) {
                continue;
            }
            int tier = brewTier(ingredient, result.potion().get());
            if (tier < 0 || tier >= chosenTier) {
                continue;
            }
            chosenTier = tier;
            chosenSlot = slot;
            chosenResult = result;
        }
        return chosenSlot < 0 ? null : new Selection(chosenSlot, chosenResult);
    }

    private static int brewTier(ItemStack ingredient, Holder<Potion> result) {
        if (result.is(Potions.MUNDANE) || result.is(Potions.THICK)) {
            return -1;
        }
        if (ingredient.is(Items.NETHER_WART)) {
            return 0;
        }
        return PotionVariants.isEnhanced(result) ? 2 : 1;
    }

    public record Selection(int slot, PotionContents result) {
    }

    public static boolean tryMakeSoaked(
        LargeCauldronFluidHandler fluids,
        ResourceHandler<ItemResource> input,
        ResourceHandler<ItemResource> output
    ) {
        try (Transaction transaction = Transaction.openRoot()) {
            boolean soaked = tryMakeSoaked(
                fluids.copyFluids(),
                input,
                output,
                transaction,
                (stored, tx) -> fluids.extract(
                    FluidResource.of(stored),
                    PotionSoakingRecipes.FLUID_COST,
                    tx
                )
            );
            if (!soaked) {
                return false;
            }
            transaction.commit();
            return true;
        }
    }

    public static boolean tryMakeSoaked(
        ResourceHandler<FluidResource> fluids,
        ResourceHandler<ItemResource> input,
        ResourceHandler<ItemResource> output
    ) {
        try (Transaction transaction = Transaction.openRoot()) {
            boolean soaked = tryMakeSoaked(
                List.of(fluids.getResource(0).toStack(fluids.getAmountAsInt(0))),
                input,
                output,
                transaction,
                (stored, tx) -> fluids.extract(
                    FluidResource.of(stored),
                    PotionSoakingRecipes.FLUID_COST,
                    tx
                )
            );
            if (!soaked) {
                return false;
            }
            transaction.commit();
            return true;
        }
    }

    private static boolean tryMakeSoaked(
        List<FluidStack> available,
        ResourceHandler<ItemResource> input,
        ResourceHandler<ItemResource> output,
        TransactionContext transaction,
        FluidDrainer drainer
    ) {
        for (PotionSoakingRecipes.Soakable soakable : PotionSoakingRecipes.all()) {
            int need = soakable.count();
            int slot = findSourceSlot(input, soakable::matches, need);
            if (slot < 0) {
                continue;
            }
            for (FluidStack stored : available) {
                if (stored.getAmount() < PotionSoakingRecipes.FLUID_COST) {
                    continue;
                }
                PotionContents contents = PotionFluidUtil.getContents(stored);
                if (contents == null) {
                    continue;
                }
                ItemStack soaked = soakable.soakedWith(contents);
                if (!consumes(transaction, output, soaked, input, slot, need, stored, drainer)) {
                    continue;
                }
                return true;
            }
        }
        for (PotionSoakingRecipes.Transformation transformation : PotionSoakingRecipes.transformations()) {
            int need = transformation.count();
            int slot = findSourceSlot(input, transformation::matches, need);
            if (slot < 0) {
                continue;
            }
            for (FluidStack stored : available) {
                if (stored.getAmount() < PotionSoakingRecipes.FLUID_COST) {
                    continue;
                }
                PotionContents contents = PotionFluidUtil.getContents(stored);
                if (contents == null || !transformation.acceptsPotion(contents)) {
                    continue;
                }
                if (!consumes(
                    transaction, output, transformation.outputStack(), input, slot, need, stored, drainer)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private static boolean consumes(
        TransactionContext transaction,
        ResourceHandler<ItemResource> output,
        ItemStack product,
        ResourceHandler<ItemResource> input,
        int slot,
        int need,
        FluidStack stored,
        FluidDrainer drainer
    ) {
        try (Transaction nested = Transaction.open(transaction)) {
            int inserted = output.insert(ItemResource.of(product), product.getCount(), nested);
            if (inserted < product.getCount()) {
                return false;
            }
            int extracted = input.extract(slot, input.getResource(slot), need, nested);
            if (extracted < need) {
                return false;
            }
            if (drainer.extract(stored, nested) < PotionSoakingRecipes.FLUID_COST) {
                return false;
            }
            nested.commit();
            return true;
        }
    }

    private static int findSourceSlot(
        ResourceHandler<ItemResource> input,
        Predicate<ItemStack> matches,
        int need
    ) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getResource(slot).toStack(input.getAmountAsInt(slot));
            if (stack.getCount() >= need && matches.test(stack)) {
                return slot;
            }
        }
        return -1;
    }

    @FunctionalInterface
    private interface FluidDrainer {
        int extract(FluidStack stored, TransactionContext transaction);
    }
}
