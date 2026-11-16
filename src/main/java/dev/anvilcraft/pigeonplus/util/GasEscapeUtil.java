package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.joml.Vector3f;

public class GasEscapeUtil {
    private static final int DRAIN_GAS_ESCAPE_INTERVAL_TICKS = 5;
    private static final int DRAIN_GAS_ESCAPE_AMOUNT = FluidType.BUCKET_VOLUME / 4;

    private GasEscapeUtil() {
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
}
