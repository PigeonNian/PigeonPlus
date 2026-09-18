package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.effect.StunMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AddonMobEffects {
    private static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, AnvilCraftPigeonPlus.MOD_ID);

    /**
     * 眩晕：火箭重拳撞墙效果的标记。
     */
    public static final DeferredHolder<MobEffect, StunMobEffect> STUN =
        EFFECTS.register("stun", StunMobEffect::new);

    private AddonMobEffects() {
    }

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}
