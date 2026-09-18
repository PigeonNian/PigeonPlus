package dev.anvilcraft.pigeonplus.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 眩晕：被火箭重拳击飞撞墙的敌人短暂失去行动能力。
 *
 * <p>实际的“停止行动”由 {@code StunManager} 通过 {@code Mob#setNoAi} 实现，
 * 本效果负责持续时间与图标显示，并在生效期间持续压制水平速度。
 */
public class StunMobEffect extends MobEffect {

    public StunMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x4A4A4A);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Vec3 movement = entity.getDeltaMovement();
        entity.setDeltaMovement(movement.x * 0.1, movement.y, movement.z * 0.1);
        entity.hurtMarked = true;
        return true;
    }
}
