package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.workstation.GiantAnvilBlock;
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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.minecraft.util.ARGB;
import org.joml.Vector3f;

public class GasEscapeUtil {
    private static final int GAS_ESCAPE_INTERVAL_TICKS = 20;
    private static final int GAS_ESCAPE_AMOUNT = FluidType.BUCKET_VOLUME / 10;
    private static final int DRAIN_GAS_ESCAPE_INTERVAL_TICKS = 5;
    private static final int DRAIN_GAS_ESCAPE_AMOUNT = FluidType.BUCKET_VOLUME / 4;

    private GasEscapeUtil() {
    }

    public static void escapeFishTankGas(Level level, BlockPos pos, ResourceHandler<FluidResource> handler) {
        if (!canEscapeThisTick(level) || isCoveredByFullCollisionBlock(level, pos.above())) {
            return;
        }
        drainGas(handler);
    }

    public static void escapeLargeCauldronGas(Level level, BlockPos mainPos, LargeCauldronFluidHandler handler) {
        if (!canEscapeThisTick(level) || isLargeCauldronCovered(level, mainPos)) {
            return;
        }
        drainGas(handler);
    }

    public static void escapeDrainGas(Level level, BlockPos pos, ResourceHandler<FluidResource> handler) {
        if (level.isClientSide()
            || level.getGameTime() % DRAIN_GAS_ESCAPE_INTERVAL_TICKS != 0
            || !CompressedAirDrainFluidHandler.isDrainAirExposed(level, pos)) {
            return;
        }
        for (int tank = 0; tank < handler.size(); tank++) {
            FluidResource resource = handler.getResource(tank);
            if (resource.isEmpty() || !(resource.getFluid() instanceof GasFluid)) {
                continue;
            }
            long available = handler.getAmountAsLong(tank);
            int amount = (int) Math.min(DRAIN_GAS_ESCAPE_AMOUNT, available);
            if (amount <= 0) {
                continue;
            }
            int drained = pigeonplus$extract(handler, tank, resource, amount);
            if (drained > 0) {
                // 用资源与数量重建 FluidStack，只为取粒子颜色与数量
                spawnDrainGasParticles((ServerLevel) level, pos, resource.toStack(drained));
            }
        }
    }

    /**
     * 在独立事务里抽取流体。
     *
     * <p>26.1 的流体能力换成了 {@code ResourceHandler} + {@code Transaction}：
     * 抽取必须发生在事务内，且只有 {@code commit()} 之后才真正生效。
     * 因此这里统一封装——抽取成功才提交，失败则自动回滚（try-with-resources 关闭）。
     */
    private static int pigeonplus$extract(
        ResourceHandler<FluidResource> handler, int tank, FluidResource resource, int amount
    ) {
        return FluidTransactions.extract(handler, tank, resource, amount);
    }

    public static boolean hasStoredBiogas(ResourceHandler<FluidResource> handler) {
        for (int tank = 0; tank < handler.size(); tank++) {
            FluidResource resource = handler.getResource(tank);
            if (resource.isEmpty() || handler.getAmountAsLong(tank) <= 0) {
                continue;
            }
            if (resource.getFluid().isSame(AddonFluids.GASEOUS_BIOGAS.get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean canEscapeThisTick(Level level) {
        return !level.isClientSide() && level.getGameTime() % GAS_ESCAPE_INTERVAL_TICKS == 0;
    }

    /** 把所有气体储罐各抽走一小口（鱼缸与大炼药锅共用）。 */
    private static void drainGas(ResourceHandler<FluidResource> handler) {
        for (int tank = 0; tank < handler.size(); tank++) {
            FluidResource resource = handler.getResource(tank);
            if (resource.isEmpty() || !(resource.getFluid() instanceof GasFluid)) {
                continue;
            }
            long available = handler.getAmountAsLong(tank);
            int amount = (int) Math.min(GAS_ESCAPE_AMOUNT, available);
            if (amount > 0) {
                pigeonplus$extract(handler, tank, resource, amount);
            }
        }
    }

    private static void spawnDrainGasParticles(ServerLevel level, BlockPos pos, FluidStack gas) {
        RandomSource random = level.getRandom();
        DustParticleOptions particle = new DustParticleOptions(gasParticleColor(gas.getFluid()), 0.9f);
        Vec3 center = Vec3.atCenterOf(pos);
        int count = Math.clamp(gas.getAmount() / 40, 6, 18);
        for (int i = 0; i < count; i++) {
            Direction direction = Direction.getRandom(random);
            Vec3 normal = Vec3.atLowerCornerOf(direction.getUnitVec3i());
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

    /**
     * 气体对应的粒子颜色。
     *
     * <p>26.1 的 {@code DustParticleOptions} 收打包好的 ARGB int，
     * 不再是 {@code Vector3f}，故这里直接返回 int。
     */
    private static int gasParticleColor(Fluid fluid) {
        if (fluid.isSame(AddonFluids.GASEOUS_BIOGAS.get())) {
            return ARGB.colorFromFloat(1.0f, 0.42f, 0.56f, 0.24f);
        }
        if (fluid.isSame(AddonFluids.COMPRESSED_AIR.get())) {
            return ARGB.colorFromFloat(1.0f, 0.85f, 0.95f, 1.0f);
        }
        return ARGB.colorFromFloat(1.0f, 0.8f, 0.85f, 0.9f);
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
