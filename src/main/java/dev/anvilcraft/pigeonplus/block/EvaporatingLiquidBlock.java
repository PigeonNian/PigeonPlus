package dev.anvilcraft.pigeonplus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.joml.Vector3f;

public class EvaporatingLiquidBlock extends LiquidBlock {
    private static final int MIN_EVAPORATION_DELAY_TICKS = 20;
    private static final int RANDOM_EVAPORATION_DELAY_TICKS = 60;
    private final Vector3f vaporColor;

    public EvaporatingLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties, Vector3f vaporColor) {
        super(fluid, properties);
        this.vaporColor = vaporColor;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        this.evaporate(level, pos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        this.evaporate(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        scheduleEvaporation(level, pos);
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        net.minecraft.world.level.block.Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        scheduleEvaporation(level, pos);
    }

    private static void scheduleEvaporation(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        level.scheduleTick(
            pos,
            level.getBlockState(pos).getBlock(),
            MIN_EVAPORATION_DELAY_TICKS + level.random.nextInt(RANDOM_EVAPORATION_DELAY_TICKS)
        );
    }

    private void evaporate(ServerLevel level, BlockPos pos) {
        level.sendParticles(
            new DustParticleOptions(this.vaporColor, 1.15f),
            pos.getX() + 0.5,
            pos.getY() + 0.2,
            pos.getZ() + 0.5,
            12,
            0.38,
            0.16,
            0.38,
            0.035
        );
        level.sendParticles(
            ParticleTypes.POOF,
            pos.getX() + 0.5,
            pos.getY() + 0.2,
            pos.getZ() + 0.5,
            3,
            0.25,
            0.08,
            0.25,
            0.01
        );
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }
}
