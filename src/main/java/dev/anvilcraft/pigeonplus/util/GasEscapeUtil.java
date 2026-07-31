package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.joml.Vector3f;

public class GasEscapeUtil {
    private static final int GAS_ESCAPE_INTERVAL_TICKS = 20;
    private static final int GAS_ESCAPE_AMOUNT = FluidType.BUCKET_VOLUME / 10;
    private static final int DRAIN_GAS_ESCAPE_INTERVAL_TICKS = 5;
    private static final int DRAIN_GAS_ESCAPE_AMOUNT = FluidType.BUCKET_VOLUME / 4;

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

    public static void escapeDrainGas(Level level, BlockPos pos, IFluidHandler handler) {
        if (level.isClientSide()
            || level.getGameTime() % DRAIN_GAS_ESCAPE_INTERVAL_TICKS != 0
            || !CompressedAirDrainFluidHandler.isDrainAirExposed(level, pos)) {
            return;
        }
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack stack = handler.getFluidInTank(tank);
            if (!(stack.getFluid() instanceof GasFluid)) {
                continue;
            }
            int amount = Math.min(DRAIN_GAS_ESCAPE_AMOUNT, stack.getAmount());
            FluidStack drained = handler.drain(stack.copyWithAmount(amount), IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty()) {
                spawnDrainGasParticles((ServerLevel) level, pos, drained);
            }
        }
    }

    public static boolean hasStoredBiogas(IFluidHandler handler) {
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack stack = handler.getFluidInTank(tank);
            if (!stack.isEmpty() && stack.getFluid().isSame(AddonFluids.GASEOUS_BIOGAS.get())) {
                return true;
            }
        }
        return false;
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

    private static void spawnDrainGasParticles(ServerLevel level, BlockPos pos, FluidStack gas) {
        RandomSource random = level.getRandom();
        DustParticleOptions particle = new DustParticleOptions(gasParticleColor(gas.getFluid()), 0.9f);
        Vec3 center = Vec3.atCenterOf(pos);
        int count = Math.clamp(gas.getAmount() / 40, 6, 18);
        for (int i = 0; i < count; i++) {
            Direction direction = Direction.getRandom(random);
            Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
            Vec3 particlePos = center.add(normal.scale(0.48)).add(
                (random.nextDouble() - 0.5) * 0.35,
                (random.nextDouble() - 0.5) * 0.35,
                (random.nextDouble() - 0.5) * 0.35
            );
            Vec3 velocity = normal.scale(0.035 + random.nextDouble() * 0.045).add(
                (random.nextDouble() - 0.5) * 0.025,
                0.035 + random.nextDouble() * 0.035,
                (random.nextDouble() - 0.5) * 0.025
            );
            level.sendParticles(
                particle,
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

    private static Vector3f gasParticleColor(Fluid fluid) {
        if (fluid.isSame(AddonFluids.GASEOUS_BIOGAS.get())) {
            return new Vector3f(0.42f, 0.56f, 0.24f);
        }
        if (fluid.isSame(AddonFluids.COMPRESSED_AIR.get())) {
            return new Vector3f(0.85f, 0.95f, 1.0f);
        }
        return new Vector3f(0.8f, 0.85f, 0.9f);
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
