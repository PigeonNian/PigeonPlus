package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.util.StasisTimeFreezeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 冻结中的实体不再享有受击无敌时间，使每次攻击都能结算伤害。
 *
 * <h3>26.1 的注入目标变化</h3>
 * 旧版的 {@code Entity#hurt(DamageSource, float)} 被拆成两个方法：
 * <ul>
 *   <li>{@code hurtServer(ServerLevel, DamageSource, float)}——服务端结算，
 *       返回是否真正造成了伤害；</li>
 *   <li>{@code hurtClient(DamageSource)}——客户端预测。</li>
 * </ul>
 * 伤害与无敌帧都由服务端裁决，因此改注入 {@code hurtServer}。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void pigeonplus$clearStasisInvulnerableTimeBeforeHurt(
        ServerLevel level,
        DamageSource source,
        float amount,
        CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (StasisTimeFreezeManager.isFrozen(entity)) {
            entity.invulnerableTime = 0;
        }
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void pigeonplus$clearStasisInvulnerableTimeAfterHurt(
        ServerLevel level,
        DamageSource source,
        float amount,
        CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (StasisTimeFreezeManager.isFrozen(entity)) {
            entity.invulnerableTime = 0;
        }
    }
}
