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

    /**
     * 泡在混合生物质里会持续反胃。
     *
     * <p>26.1 的 {@code entityInside} 新增了两个参数：
     * {@code InsideBlockEffectApplier}（用于派发「方块内效果」的替换机制）
     * 与一个 boolean。本方法自己直接施加药水效果，不依赖该机制，
     * 故只需忽略这两个新参数。
     */
    @Override
    protected void entityInside(
        BlockState state,
        Level level,
        BlockPos pos,
        Entity entity,
        net.minecraft.world.entity.InsideBlockEffectApplier effectApplier,
        boolean isInside
    ) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) {
            return;
        }

        MobEffectInstance current = living.getEffect(MobEffects.NAUSEA);
        if (current == null || current.getDuration() < NAUSEA_DURATION_TICKS / 2) {
            living.addEffect(new MobEffectInstance(MobEffects.NAUSEA, NAUSEA_DURATION_TICKS));
        }
    }
}
