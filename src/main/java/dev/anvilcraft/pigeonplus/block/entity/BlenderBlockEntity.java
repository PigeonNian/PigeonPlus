package dev.anvilcraft.pigeonplus.block.entity;

import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

public class BlenderBlockEntity extends BlockEntity {
    private static final int AIR_CAPACITY = 1000;
    private static final int AIR_CONSUME_PER_TICK = 20;

    private final FluidTank compressedAirTank = new FluidTank(
        AIR_CAPACITY,
        stack -> stack.getFluid().isSame(AddonFluids.COMPRESSED_AIR.get())
    ) {
        @Override
        protected void onContentsChanged() {
            BlenderBlockEntity.this.onAirChanged();
        }
    };
    private final IFluidHandler inputHandler = new InputOnlyFluidHandler();

    public BlenderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public BlenderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLENDER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlenderBlockEntity blockEntity) {
        if (blockEntity.compressedAirTank.getFluidAmount() > 0) {
            blockEntity.setWorking(true);
            blockEntity.compressedAirTank.drain(AIR_CONSUME_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
        } else {
            blockEntity.setWorking(false);
        }
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        if (side == null || side == this.getBlockState().getValue(BlenderBlock.FACING).getOpposite()) {
            return this.inputHandler;
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag tankTag = new CompoundTag();
        this.compressedAirTank.writeToNBT(registries, tankTag);
        tag.put("CompressedAir", tankTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.compressedAirTank.readFromNBT(registries, tag.getCompound("CompressedAir"));
    }

    private void onAirChanged() {
        this.setChanged();
        this.setWorking(this.compressedAirTank.getFluidAmount() > 0);
    }

    private void setWorking(boolean working) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        BlockState state = this.getBlockState();
        if (state.hasProperty(BlenderBlock.WORKING) && state.getValue(BlenderBlock.WORKING) != working) {
            this.level.setBlock(this.worldPosition, state.setValue(BlenderBlock.WORKING, working), Block.UPDATE_CLIENTS);
        }
    }

    private class InputOnlyFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return compressedAirTank.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return compressedAirTank.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return compressedAirTank.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return compressedAirTank.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return compressedAirTank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
