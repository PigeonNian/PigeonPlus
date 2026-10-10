package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import dev.dubhe.anvilcraft.api.fluid.FluidStackResourceHandler;

import java.util.List;

public final class PotionParticleUtil {
    private static final int INTERVAL_TICKS = 4;
    private static final int PARTICLES_PER_EMIT = 3;

    private PotionParticleUtil() {
    }

    public static void emitLargeCauldronParticles(
        Level level,
        BlockPos mainPos,
        LargeCauldronFluidHandler fluids
    ) {
        if (!(level instanceof ServerLevel serverLevel) || !shouldEmitThisTick(serverLevel)) {
            return;
        }
        emitAll(
            serverLevel,
            PotionFluidUtil.allPotions(fluids),
            mainPos.getX() + PotionContainerGeometry.CAULDRON_MIN_XZ,
            mainPos.getZ() + PotionContainerGeometry.CAULDRON_MIN_XZ,
            PotionContainerGeometry.CAULDRON_MAX_XZ - PotionContainerGeometry.CAULDRON_MIN_XZ,
            PotionContainerGeometry.cauldronSurfaceY(mainPos, fluids)
        );
    }

    public static void emitFishTankParticles(Level level, BlockPos pos, FluidStackResourceHandler tank) {
        if (!(level instanceof ServerLevel serverLevel) || !shouldEmitThisTick(serverLevel)) {
            return;
        }
        emitAll(
            serverLevel,
            PotionFluidUtil.allPotions(tank),
            pos.getX() + PotionContainerGeometry.TANK_MIN_XZ,
            pos.getZ() + PotionContainerGeometry.TANK_MIN_XZ,
            PotionContainerGeometry.TANK_WIDTH,
            PotionContainerGeometry.fishTankSurfaceY(pos, tank)
        );
    }

    private static void emitAll(
        ServerLevel level,
        List<FluidStack> potions,
        double minX,
        double minZ,
        double width,
        double y
    ) {
        for (FluidStack potion : potions) {
            PotionContents contents = PotionFluidUtil.getContents(potion);
            if (contents != null) {
                emit(level, contents.getColor(), minX, minZ, width, y);
            }
        }
    }

    private static void emit(
        ServerLevel level,
        int color,
        double minX,
        double minZ,
        double width,
        double y
    ) {
        RandomSource random = level.getRandom();
        ColorParticleOption particle = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color);
        for (int i = 0; i < PARTICLES_PER_EMIT; i++) {
            level.sendParticles(
                particle,
                minX + random.nextDouble() * width,
                y,
                minZ + random.nextDouble() * width,
                1,
                0.0,
                0.0,
                0.0,
                0.0
            );
        }
    }

    private static boolean shouldEmitThisTick(ServerLevel level) {
        return level.getGameTime() % INTERVAL_TICKS == 0;
    }
}
