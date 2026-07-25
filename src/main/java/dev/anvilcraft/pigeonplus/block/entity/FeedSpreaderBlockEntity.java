package dev.anvilcraft.pigeonplus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FeedSpreaderBlockEntity extends BlockEntity {
    private static final float PISTON_PRESS_STEP = 0.18F;
    private static final float PISTON_RELEASE_STEP = 0.25F;
    private static final float BUCKET_ROTATION_STEP = 1.0F / 24.0F;
    private static final int PISTON_RELEASE_DELAY_TICKS = 20;

    private float pistonPress;
    private float pistonPressOld;
    private boolean pistonPressing;
    private boolean pistonHolding;
    private int pistonReleaseDelay;
    private float bucketRotation;
    private float bucketRotationOld;
    private boolean bucketRotating;

    public FeedSpreaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FEED_SPREADER.get(), pos, state);
    }

    public float getPistonPress(float partialTick) {
        float press = this.pistonPressOld + (this.pistonPress - this.pistonPressOld) * partialTick;
        return Math.max(0.0F, Math.min(press, 1.0F));
    }

    public float getBucketRotation(float partialTick) {
        float rotation = this.bucketRotationOld + (this.bucketRotation - this.bucketRotationOld) * partialTick;
        return Math.max(0.0F, Math.min(rotation, 1.0F));
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

    public static void tick(Level level, BlockPos pos, BlockState state, FeedSpreaderBlockEntity entity) {
        if (level.isClientSide()) {
            entity.tickPistonPressAnimation(level, pos);
            entity.tickBucketRotationAnimation();
        }
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
            this.startBucketRotationAnimation();
        }
    }

    private void startBucketRotationAnimation() {
        this.bucketRotation = 0.0F;
        this.bucketRotationOld = 0.0F;
        this.bucketRotating = true;
    }

    private void tickBucketRotationAnimation() {
        this.bucketRotationOld = this.bucketRotation;
        if (!this.bucketRotating) {
            return;
        }
        this.bucketRotation = Math.min(1.0F, this.bucketRotation + BUCKET_ROTATION_STEP);
        if (this.bucketRotation >= 1.0F) {
            this.bucketRotation = 0.0F;
            this.bucketRotationOld = 0.0F;
            this.bucketRotating = false;
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

    private static boolean hasAnvilOnTop(Level level, BlockPos pos) {
        return level.getBlockState(pos.above()).getBlock() instanceof AnvilBlock;
    }
}
