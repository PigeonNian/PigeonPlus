package dev.anvilcraft.pigeonplus.block.entity;

import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AnvilPumpBlockEntity extends BlockEntity {
    private static final int PUMP_DURATION_TICKS = 20;
    private static final int PUMP_HEADLIFT = 10;
    private static final float MAX_EFFICIENCY_FALL_DISTANCE = 20.0F;
    private static final float PISTON_PRESS_STEP = 0.4F;
    private static final float PISTON_RELEASE_STEP = 0.25F;
    private static final int PISTON_RELEASE_DELAY_TICKS = 20;
    private static final int PISTON_RELEASE_ANIMATION_TICKS = 4;
    private static final int IMPACT_UNLOCK_TICKS = PISTON_RELEASE_DELAY_TICKS + PISTON_RELEASE_ANIMATION_TICKS;

    private int remainingPumpTicks;
    private int headlift;
    private float pistonPress;
    private float pistonPressOld;
    private boolean pistonPressing;
    private boolean pistonHolding;
    private int pistonReleaseDelay;
    private boolean impactLocked;
    private int impactUnlockTicks;
    private boolean lastCanPump;

    public AnvilPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public AnvilPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANVIL_PUMP.get(), pos, state);
    }

    public boolean activate(float fallDistance) {
        if (this.impactLocked) {
            return false;
        }
        if (this.getBlockState().getValue(PumpBlock.POWERED)) {
            return false;
        }
        boolean wasPumping = this.canPump();
        int oldHeadlift = this.headlift;
        int nextHeadlift = Math.max(
            1,
            Math.round(Math.min(fallDistance / MAX_EFFICIENCY_FALL_DISTANCE, 1.0F) * PUMP_HEADLIFT)
        );
        this.remainingPumpTicks = PUMP_DURATION_TICKS;
        this.headlift = nextHeadlift;
        this.impactLocked = true;
        this.impactUnlockTicks = IMPACT_UNLOCK_TICKS;
        this.setChanged();
        this.sendUpdate();
        if (this.level != null && !this.level.isClientSide()) {
            if (wasPumping != this.canPump() || oldHeadlift != nextHeadlift) {
                FluidNetworkManager.INSTANCE.markDirty(this.level);
            }
        }
        return true;
    }

    public int getCurrentHeadlift() {
        return this.canPump() ? this.headlift : 0;
    }

    public float getPistonPress(float partialTick) {
        float press = this.pistonPressOld + (this.pistonPress - this.pistonPressOld) * partialTick;
        return Math.max(0.0F, Math.min(press, 1.0F));
    }

    public void startPistonPressAnimation() {
        if (this.pistonPress >= 1.0F || this.pistonPressing) {
            return;
        }
        this.pistonPress = 0.0F;
        this.pistonPressOld = 0.0F;
        this.pistonPressing = true;
        this.pistonHolding = false;
        this.pistonReleaseDelay = PISTON_RELEASE_DELAY_TICKS;
    }

    public boolean canPump() {
        return this.remainingPumpTicks > 0 && !this.getBlockState().getValue(PumpBlock.POWERED);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AnvilPumpBlockEntity entity) {
        if (level.isClientSide()) {
            entity.tickPistonPressAnimation(level, pos);
            return;
        }

        updateRedstoneState(level, pos, state);

        if (entity.remainingPumpTicks > 0) {
            entity.remainingPumpTicks--;
            entity.setChanged();
        }
        entity.tickImpactLock(level, pos);
        entity.updateNetworkState(level);
    }

    private void updateNetworkState(Level level) {
        if (level.isClientSide()) {
            return;
        }
        boolean canPumpNow = this.canPump();
        if (canPumpNow != this.lastCanPump) {
            this.lastCanPump = canPumpNow;
            FluidNetworkManager.INSTANCE.markDirty(level);
        }
    }

    private void tickImpactLock(Level level, BlockPos pos) {
        if (!this.impactLocked) {
            return;
        }
        if (hasAnvilOnTop(level, pos)) {
            this.impactUnlockTicks = IMPACT_UNLOCK_TICKS;
            return;
        }
        if (this.impactUnlockTicks > 0) {
            this.impactUnlockTicks--;
            return;
        }
        this.impactLocked = false;
    }

    private void tickPistonPressAnimation(Level level, BlockPos pos) {
        this.pistonPressOld = this.pistonPress;
        boolean hasAnvilOnTop = hasAnvilOnTop(level, pos);
        if (!this.pistonPressing) {
            if (hasAnvilOnTop) {
                this.pistonHolding = true;
                this.pistonPress = 1.0F;
                this.pistonReleaseDelay = PISTON_RELEASE_DELAY_TICKS;
                return;
            }
            if (this.pistonHolding) {
                if (this.pistonReleaseDelay > 0) {
                    this.pistonReleaseDelay--;
                    return;
                }
                this.pistonHolding = false;
            }
            this.tickPistonReleaseAnimation();
            return;
        }
        this.pistonPress = Math.min(1.0F, this.pistonPress + PISTON_PRESS_STEP);
        if (this.pistonPress >= 1.0F) {
            this.pistonPress = 1.0F;
            this.pistonPressing = false;
            this.pistonHolding = hasAnvilOnTop;
            this.pistonReleaseDelay = PISTON_RELEASE_DELAY_TICKS;
        }
    }

    private void tickPistonReleaseAnimation() {
        if (this.pistonPress <= 0.0F) {
            return;
        }
        if (this.pistonReleaseDelay > 0) {
            this.pistonReleaseDelay--;
            return;
        }
        this.pistonPress = Math.max(0.0F, this.pistonPress - PISTON_RELEASE_STEP);
    }

    private static void updateRedstoneState(Level level, BlockPos pos, BlockState state) {
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(PumpBlock.POWERED) != powered) {
            level.setBlock(pos, state.setValue(PumpBlock.POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean hasAnvilOnTop(Level level, BlockPos pos) {
        return level.getBlockState(pos.above()).getBlock() instanceof AnvilBlock;
    }

    private void sendUpdate() {
        if (this.level != null) {
            this.level.sendBlockUpdated(
                this.worldPosition, this.getBlockState(), this.getBlockState(),
                net.minecraft.world.level.block.Block.UPDATE_CLIENTS
            );
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RemainingPumpTicks", this.remainingPumpTicks);
        tag.putInt("Headlift", this.headlift);
        tag.putBoolean("ImpactLocked", this.impactLocked);
        tag.putInt("ImpactUnlockTicks", this.impactUnlockTicks);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.remainingPumpTicks = tag.getInt("RemainingPumpTicks");
        this.headlift = tag.getInt("Headlift");
        this.impactLocked = tag.getBoolean("ImpactLocked");
        this.impactUnlockTicks = tag.getInt("ImpactUnlockTicks");
    }
}