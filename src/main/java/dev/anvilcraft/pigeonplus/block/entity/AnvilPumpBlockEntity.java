package dev.anvilcraft.pigeonplus.block.entity;

import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.block.entity.fluid.PumpBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class AnvilPumpBlockEntity extends PumpBlockEntity {
    private static final int PUMP_DURATION_TICKS = 20;
    private static final float MAX_EFFICIENCY_FALL_DISTANCE = 20.0F;

    private int remainingPumpTicks;
    private int headlift;

    public AnvilPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANVIL_PUMP.get(), pos, state);
    }

    public void activate(float fallDistance) {
        int nextHeadlift = Math.max(
            1,
            Math.round(Math.min(fallDistance / MAX_EFFICIENCY_FALL_DISTANCE, 1.0F) * PumpBlockEntity.PUMP_HEADLIFT)
        );
        boolean wasPumping = this.canPump();
        int oldHeadlift = this.headlift;
        this.remainingPumpTicks = PUMP_DURATION_TICKS;
        this.headlift = nextHeadlift;
        this.setChanged();
        this.sendUpdate();
        if (this.level != null && !this.level.isClientSide()) {
            if (wasPumping != this.canPump() || oldHeadlift != nextHeadlift) {
                FluidNetworkManager.INSTANCE.markDirty(this.level);
            }
        }
    }

    public int getCurrentHeadlift() {
        return this.canPump() ? this.headlift : 0;
    }

    @Override
    public int getInputPower() {
        return 0;
    }

    @Override
    public boolean canPump() {
        return this.remainingPumpTicks > 0 && !this.getBlockState().getValue(PumpBlock.POWERED);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AnvilPumpBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }

        updateRedstoneState(level, pos, state);

        boolean wasPumping = entity.canPump();
        if (entity.remainingPumpTicks > 0) {
            entity.remainingPumpTicks--;
            entity.setChanged();
        }
        boolean canPumpNow = entity.canPump();
        if (wasPumping != canPumpNow || canPumpNow != entity.isLastCanPump()) {
            entity.setLastCanPump(canPumpNow);
            FluidNetworkManager.INSTANCE.markDirty(level);
        }
    }

    private static void updateRedstoneState(Level level, BlockPos pos, BlockState state) {
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(PumpBlock.POWERED) != powered) {
            level.setBlock(pos, state.setValue(PumpBlock.POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RemainingPumpTicks", this.remainingPumpTicks);
        tag.putInt("Headlift", this.headlift);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.remainingPumpTicks = tag.getInt("RemainingPumpTicks");
        this.headlift = tag.getInt("Headlift");
    }
}
