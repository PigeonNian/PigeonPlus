package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.anvilcraft.pigeonplus.util.GasLiquefactionTracker;
import dev.dubhe.anvilcraft.block.LargeFluidTankBlock;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LargeFluidTankBlockEntity.class)
public abstract class LargeFluidTankBlockEntityMixin {
    @Unique
    private static final int PIGEONPLUS_GAS_INPUT_PER_TICK = 2000;

    @Inject(method = "tick", at = @At("HEAD"))
    private void pigeonplus$liquefyGasInTank(CallbackInfo ci) {
        LargeFluidTankBlockEntity self = (LargeFluidTankBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        if (!self.getBlockState().getValue(LargeFluidTankBlock.HALF).equals(Cube3x3PartHalf.MID_CENTER)) {
            return;
        }
        IFluidHandler handler = self.getFluidHandler();
        BlockPos pos = self.getBlockPos();

        Fluid gas = pigeonplus$findLiquefiableGas(handler);
        if (gas == null) {
            pigeonplus$clearProgress(level, pos);
            return;
        }
        int totalCapacity = pigeonplus$totalCapacity(handler);
        int totalAmount = pigeonplus$totalAmount(handler);
        int gasAmount = pigeonplus$gasAmount(handler, gas);
        if (totalCapacity <= 0 || gasAmount <= 0 || totalAmount < totalCapacity) {
            GasLiquefactionTracker.clear(level, pos, gas);
            return;
        }
        int inputAmount = Math.min(PIGEONPLUS_GAS_INPUT_PER_TICK, gasAmount);
        FluidStack drained = handler.drain(new FluidStack(gas, inputAmount), IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return;
        }
        int liquidAmount = GasLiquefactionTracker.addGasInput(
            level,
            pos,
            gas,
            drained.getAmount(),
            pigeonplus$liquefactionRatio(gas)
        );
        if (liquidAmount > 0) {
            pigeonplus$replaceGasWithLiquid(handler, gas, liquidAmount);
        }
    }

    @Unique
    private static void pigeonplus$clearProgress(Level level, BlockPos pos) {
        GasLiquefactionTracker.clear(level, pos, AddonFluids.COMPRESSED_AIR.get());
        GasLiquefactionTracker.clear(level, pos, AddonFluids.GASEOUS_BIOGAS.get());
    }

    @Unique
    private static Fluid pigeonplus$findLiquefiableGas(IFluidHandler handler) {
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack stack = handler.getFluidInTank(i);
            Fluid fluid = stack.getFluid();
            if (stack.getAmount() > 0
                && (fluid.isSame(AddonFluids.COMPRESSED_AIR.get()) || fluid.isSame(AddonFluids.GASEOUS_BIOGAS.get()))) {
                return fluid;
            }
        }
        return null;
    }

    @Unique
    private static int pigeonplus$gasAmount(IFluidHandler handler, Fluid gas) {
        int amount = 0;
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack stack = handler.getFluidInTank(i);
            if (stack.getFluid().isSame(gas)) {
                amount += stack.getAmount();
            }
        }
        return amount;
    }

    @Unique
    private static int pigeonplus$totalAmount(IFluidHandler handler) {
        int total = 0;
        for (int i = 0; i < handler.getTanks(); i++) {
            total += handler.getFluidInTank(i).getAmount();
        }
        return total;
    }

    @Unique
    private static int pigeonplus$totalCapacity(IFluidHandler handler) {
        int total = 0;
        for (int i = 0; i < handler.getTanks(); i++) {
            total += handler.getTankCapacity(i);
        }
        return total;
    }

    @Unique
    private static int pigeonplus$liquefactionRatio(Fluid gas) {
        if (gas.isSame(AddonFluids.COMPRESSED_AIR.get())) {
            return GasLiquefactionTracker.COMPRESSED_AIR_TO_LIQUID_OXYGEN_RATIO;
        }
        if (gas.isSame(AddonFluids.GASEOUS_BIOGAS.get())) {
            return GasLiquefactionTracker.BIOGAS_TO_LIQUEFIED_BIOGAS_RATIO;
        }
        return 0;
    }

    @Unique
    private static void pigeonplus$replaceGasWithLiquid(IFluidHandler handler, Fluid gas, int liquidAmount) {
        Fluid liquefied = pigeonplus$liquefiedFluid(gas);
        if (liquefied == null || liquidAmount <= 0) {
            return;
        }
        FluidStack drainedGas = handler.drain(new FluidStack(gas, liquidAmount), IFluidHandler.FluidAction.EXECUTE);
        int actualAmount = drainedGas.getAmount();
        if (actualAmount <= 0) {
            return;
        }
        int filled = handler.fill(new FluidStack(liquefied, actualAmount), IFluidHandler.FluidAction.EXECUTE);
        if (filled < actualAmount) {
            handler.fill(drainedGas.copyWithAmount(actualAmount - filled), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Unique
    private static Fluid pigeonplus$liquefiedFluid(Fluid gas) {
        if (gas.isSame(AddonFluids.COMPRESSED_AIR.get())) {
            return AddonFluids.LIQUID_OXYGEN.get();
        }
        if (gas.isSame(AddonFluids.GASEOUS_BIOGAS.get())) {
            return AddonFluids.LIQUEFIED_BIOGAS.get();
        }
        return null;
    }
}