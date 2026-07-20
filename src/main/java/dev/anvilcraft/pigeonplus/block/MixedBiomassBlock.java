package dev.anvilcraft.pigeonplus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

public class MixedBiomassBlock extends LiquidBlock {
    private static final int NAUSEA_DURATION_TICKS = 10 * 20;

    public MixedBiomassBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }

        MobEffectInstance current = living.getEffect(MobEffects.CONFUSION);
        if (current == null || current.getDuration() < NAUSEA_DURATION_TICKS / 2) {
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_DURATION_TICKS));
        }
    }
}
