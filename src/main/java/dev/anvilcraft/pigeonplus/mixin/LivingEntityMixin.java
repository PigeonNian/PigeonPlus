package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.util.StasisTimeFreezeManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "hurt", at = @At("HEAD"))
    private void pigeonplus$clearStasisInvulnerableTimeBeforeHurt(
        DamageSource source,
        float amount,
        CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (StasisTimeFreezeManager.isFrozen(entity)) {
            entity.invulnerableTime = 0;
        }
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void pigeonplus$clearStasisInvulnerableTimeAfterHurt(
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
