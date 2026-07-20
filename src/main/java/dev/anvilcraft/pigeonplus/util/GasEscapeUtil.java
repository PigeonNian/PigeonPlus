package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class GasEscapeUtil {
    private static final int GAS_ESCAPE_INTERVAL_TICKS = 20;
    private static final int GAS_ESCAPE_AMOUNT = FluidType.BUCKET_VOLUME / 10;

    private GasEscapeUtil() {
    }

    public static void escapeFishTankGas(Level level, BlockPos pos, IFluidHandler handler) {
        if (!canEscapeThisTick(level) || isCoveredByFullCollisionBlock(level, pos.above())) {
            return;
        }
        drainGas(handler);
    }

    public static void escapeLargeCauldronGas(Level level, BlockPos mainPos, LargeCauldronFluidHandler handler) {
        if (!canEscapeThisTick(level) || isLargeCauldronCovered(level, mainPos)) {
            return;
        }
        for (FluidStack stack : handler.copyFluids()) {
            if (stack.getFluid() instanceof GasFluid) {
                handler.drainStoredFluid(
                    stack.copyWithAmount(Math.min(GAS_ESCAPE_AMOUNT, stack.getAmount())),
                    IFluidHandler.FluidAction.EXECUTE
                );
            }
        }
    }

    private static boolean canEscapeThisTick(Level level) {
        return !level.isClientSide() && level.getGameTime() % GAS_ESCAPE_INTERVAL_TICKS == 0;
    }

    private static void drainGas(IFluidHandler handler) {
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack stack = handler.getFluidInTank(tank);
            if (!(stack.getFluid() instanceof GasFluid)) {
                continue;
            }
            handler.drain(
                stack.copyWithAmount(Math.min(GAS_ESCAPE_AMOUNT, stack.getAmount())),
                IFluidHandler.FluidAction.EXECUTE
            );
        }
    }

    private static boolean isLargeCauldronCovered(Level level, BlockPos mainPos) {
        if (isCoveredByGiantAnvil(level, mainPos)) {
            return true;
        }
        BlockPos center = mainPos.above(2);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            if (!isCoveredByFullCollisionBlock(level, pos)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCoveredByGiantAnvil(Level level, BlockPos mainPos) {
        BlockPos expectedGiantAnvilMainPos = mainPos.above(3);
        BlockPos bottomCenter = mainPos.above(2);
        for (BlockPos pos : BlockPos.betweenClosed(bottomCenter.offset(-1, 0, -1), bottomCenter.offset(1, 0, 1))) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof GiantAnvilBlock giantAnvil)) {
                return false;
            }
            if (!giantAnvil.getMainPartPos(pos, state).equals(expectedGiantAnvilMainPos)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCoveredByFullCollisionBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return Block.isShapeFullBlock(state.getCollisionShape(level, pos));
    }
}
