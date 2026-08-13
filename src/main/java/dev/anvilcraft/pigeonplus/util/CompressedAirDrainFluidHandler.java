package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.dubhe.anvilcraft.block.entity.fluid.DrainBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

public class CompressedAirDrainFluidHandler implements IFluidHandler {
    private static final int AIR_CAPACITY = DrainBlockEntity.CAPACITY;

    private final Level level;
    private final BlockPos pos;
    private final IFluidHandler delegate;

    public CompressedAirDrainFluidHandler(Level level, BlockPos pos, IFluidHandler delegate) {
        this.level = level;
        this.pos = pos.immutable();
        this.delegate = delegate;
    }

    public boolean canExtractAir() {
        return this.isInternalTankEmpty() && isDrainAirExposed(this.level, this.pos);
    }

    private boolean isInternalTankEmpty() {
        for (int i = 0; i < this.delegate.getTanks(); i++) {
            if (!this.delegate.getFluidInTank(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getTanks() {
        return this.delegate.getTanks() + 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        if (tank < this.delegate.getTanks()) {
            return this.delegate.getFluidInTank(tank);
        }
        return this.canExtractAir()
            ? new FluidStack(AddonFluids.COMPRESSED_AIR.get(), AIR_CAPACITY)
            : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        if (tank < this.delegate.getTanks()) {
            return this.delegate.getTankCapacity(tank);
        }
        return AIR_CAPACITY;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return tank < this.delegate.getTanks() && this.delegate.isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return this.delegate.fill(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.getFluid().isSame(AddonFluids.COMPRESSED_AIR.get()) && this.canExtractAir()) {
            int amount = Math.min(resource.getAmount(), AIR_CAPACITY);
            this.spawnAirIntakeParticles(amount, action);
            return resource.copyWithAmount(amount);
        }
        return this.delegate.drain(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack drained = this.delegate.drain(maxDrain, action);
        if (!drained.isEmpty()) {
            return drained;
        }
        if (!this.canExtractAir()) {
            return FluidStack.EMPTY;
        }
        int amount = Math.min(maxDrain, AIR_CAPACITY);
        this.spawnAirIntakeParticles(amount, action);
        return new FluidStack(AddonFluids.COMPRESSED_AIR.get(), amount);
    }

    private void spawnAirIntakeParticles(int amount, FluidAction action) {
        if (action != FluidAction.EXECUTE || amount <= 0 || !(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        RandomSource random = serverLevel.getRandom();
        int count = Math.clamp(amount / 80, 2, 8);
        Vec3 center = Vec3.atCenterOf(this.pos);
        for (int i = 0; i < count; i++) {
            double x = this.pos.getX() - 1.0 + random.nextDouble() * 3.0;
            double y = this.pos.getY() - 1.0 + random.nextDouble() * 3.0;
            double z = this.pos.getZ() - 1.0 + random.nextDouble() * 3.0;
            Vec3 particlePos = new Vec3(x, y, z);
            Vec3 velocity = center.subtract(particlePos)
                .normalize()
                .scale(0.08 + random.nextDouble() * 0.08)
                .add(
                    (random.nextDouble() - 0.5) * 0.015,
                    (random.nextDouble() - 0.5) * 0.015,
                    (random.nextDouble() - 0.5) * 0.015
                );
            serverLevel.sendParticles(
                ParticleTypes.CLOUD,
                particlePos.x,
                particlePos.y,
                particlePos.z,
                0,
                velocity.x,
                velocity.y,
                velocity.z,
                1.0
            );
        }
    }

    public static boolean isDrainAirExposed(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos checkPos = pos.relative(direction);
            if (!level.isLoaded(checkPos)) {
                continue;
            }
            BlockState state = level.getBlockState(checkPos);
            if (state.isAir() && state.getFluidState().isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
