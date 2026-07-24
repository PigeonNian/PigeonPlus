package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.util.StasisTimeFreezeManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class StasisTimeFreezeEventListener {
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (StasisTimeFreezeManager.captureDamage(entity, event.getSource(), event.getOriginalDamage())) {
            event.setNewDamage(0.0f);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (StasisTimeFreezeManager.freezeTick(entity)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (StasisTimeFreezeManager.captureKnockback(
            entity,
            event.getStrength(),
            event.getRatioX(),
            event.getRatioZ()
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosionKnockback(ExplosionKnockbackEvent event) {
        Entity entity = event.getAffectedEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (StasisTimeFreezeManager.captureMomentum(entity, event.getKnockbackVelocity())) {
            event.setKnockbackVelocity(net.minecraft.world.phys.Vec3.ZERO);
        }
    }
}
