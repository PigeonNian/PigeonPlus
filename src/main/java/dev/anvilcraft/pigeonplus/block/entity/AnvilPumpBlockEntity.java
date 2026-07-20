package dev.anvilcraft.pigeonplus.block.entity;

import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.block.entity.fluid.PumpBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class AnvilPumpBlockEntity extends PumpBlockEntity {
    private static final int PUMP_DURATION_TICKS = 20;
    private static final float MAX_EFFICIENCY_FALL_DISTANCE = 20.0F;
    private static final float PISTON_PRESS_STEP = 0.25F;
    private static final int PISTON_RELEASE_DELAY_TICKS = 20;

    private int remainingPumpTicks;
    private int headlift;
    private float pistonPress;
    private float pistonPressOld;
    private boolean pistonPressing;
    private int pistonReleaseDelay;
    private boolean impactLocked;

    public AnvilPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANVIL_PUMP.get(), pos, state);
    }

    public boolean activate(float fallDistance) {
        if (this.impactLocked) {
            return false;
        }
        int nextHeadlift = Math.max(
            1,
            Math.round(Math.min(fallDistance / MAX_EFFICIENCY_FALL_DISTANCE, 1.0F) * PumpBlockEntity.PUMP_HEADLIFT)
        );
        boolean wasPumping = this.canPump();
        int oldHeadlift = this.headlift;
        this.remainingPumpTicks = PUMP_DURATION_TICKS;
        this.headlift = nextHeadlift;
        this.impactLocked = true;
        this.pistonPress = 1.0F;
        this.pistonPressing = false;
        this.pistonReleaseDelay = PISTON_RELEASE_DELAY_TICKS;
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
        this.pistonPressing = true;
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
        entity.tickPistonPressAnimation(level, pos);
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

    private void tickPistonPressAnimation(Level level, BlockPos pos) {
        this.pistonPressOld = this.pistonPress;
        if (hasAnvilOnTop(level, pos)) {
            this.pistonPress = 1.0F;
            this.pistonPressing = false;
            this.pistonReleaseDelay = PISTON_RELEASE_DELAY_TICKS;
            this.impactLocked = true;
            return;
        }
        if (!this.pistonPressing) {
            this.tickPistonReleaseAnimation();
            return;
        }
        this.pistonPress = Math.min(1.0F, this.pistonPress + PISTON_PRESS_STEP);
        if (this.pistonPress >= 1.0F) {
            this.pistonPressing = false;
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
        this.pistonPress = Math.max(0.0F, this.pistonPress - PISTON_PRESS_STEP);
        if (this.pistonPress <= 0.0F) {
            this.impactLocked = false;
        }
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RemainingPumpTicks", this.remainingPumpTicks);
        tag.putInt("Headlift", this.headlift);
        tag.putFloat("PistonPress", this.pistonPress);
        tag.putBoolean("PistonPressing", this.pistonPressing);
        tag.putInt("PistonReleaseDelay", this.pistonReleaseDelay);
        tag.putBoolean("ImpactLocked", this.impactLocked);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.remainingPumpTicks = tag.getInt("RemainingPumpTicks");
        this.headlift = tag.getInt("Headlift");
        this.pistonPress = tag.getFloat("PistonPress");
        this.pistonPressOld = this.pistonPress;
        this.pistonPressing = tag.getBoolean("PistonPressing");
        this.pistonReleaseDelay = tag.getInt("PistonReleaseDelay");
        this.impactLocked = tag.getBoolean("ImpactLocked");
    }
}
