package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.fluid.PotionBottleTypes;
import dev.anvilcraft.pigeonplus.fluid.PotionFluid;
import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class PotionBottleDispenseBehavior implements DispenseItemBehavior {
    private static final int FLUID_PER_BOTTLE = 250;

    private final DispenseItemBehavior fallback;

    public PotionBottleDispenseBehavior(DispenseItemBehavior fallback) {
        this.fallback = fallback;
    }

    @Override
    public ItemStack dispense(BlockSource source, ItemStack stack) {
        ItemStack filled = this.tryFillFromTank(source, stack);
        return filled != null ? filled : this.fallback.dispense(source, stack);
    }

    private ItemStack tryFillFromTank(BlockSource source, ItemStack stack) {
        Item filledItem = PotionBottleTypes.filledFor(stack);
        if (filledItem == null) {
            return null;
        }
        Direction facing = source.state().getValue(DispenserBlock.FACING);
        BlockPos pos = source.pos().relative(facing);
        ResourceHandler<FluidResource> handler =
            source.level().getCapability(Capabilities.Fluid.BLOCK, pos, facing.getOpposite());
        if (handler == null || handler.size() == 0) {
            return null;
        }
        FluidStack stored = handler.getResource(0).toStack(handler.getAmountAsInt(0));
        if (!(stored.getFluid() instanceof PotionFluid) || stored.getAmount() < FLUID_PER_BOTTLE) {
            return null;
        }
        PotionContents contents = PotionFluidUtil.getContents(stored);
        if (contents == null) {
            return null;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int drained = handler.extract(FluidResource.of(stored), FLUID_PER_BOTTLE, transaction);
            if (drained < FLUID_PER_BOTTLE) {
                return null;
            }
            transaction.commit();
        }
        ItemStack potion = new ItemStack(filledItem);
        potion.set(DataComponents.POTION_CONTENTS, contents);
        source.level().levelEvent(1000, source.pos(), 0);
        source.level().levelEvent(2000, source.pos(), facing.get3DDataValue());
        source.level().playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        stack.shrink(1);
        if (stack.isEmpty()) {
            return potion;
        }
        ItemStack leftover = source.blockEntity().insertItem(potion);
        if (!leftover.isEmpty()) {
            DefaultDispenseItemBehavior.spawnItem(
                source.level(), leftover, 6, facing, DispenserBlock.getDispensePosition(source));
        }
        return stack;
    }
}
