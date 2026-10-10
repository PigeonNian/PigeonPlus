package dev.anvilcraft.pigeonplus.fluid;

import dev.anvilcraft.pigeonplus.init.AddonFluids;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class PotionBottleFluidHandler extends ItemAccessResourceHandler<FluidResource> {
    public static final int FLUID_PER_BOTTLE = 250;

    public PotionBottleFluidHandler(ItemAccess itemAccess) {
        super(itemAccess, 1);
    }

    private static PotionContents contentsOf(ItemResource resource) {
        return resource.toStack().get(DataComponents.POTION_CONTENTS);
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        PotionContents contents = contentsOf(accessResource);
        if (contents == null) {
            return FluidResource.EMPTY;
        }
        return FluidResource.of(PotionFluidUtil.withContents(
            new net.neoforged.neoforge.fluids.FluidStack(AddonFluids.POTION.get(), FLUID_PER_BOTTLE), contents));
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        return contentsOf(accessResource) == null ? 0 : FLUID_PER_BOTTLE;
    }

    @Override
    protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        if (newAmount == 0) {
            return ItemResource.of(PotionBottleTypes.emptyFor(accessResource.toStack()));
        }
        return ItemResource.EMPTY;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return false;
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return FLUID_PER_BOTTLE;
    }
}
