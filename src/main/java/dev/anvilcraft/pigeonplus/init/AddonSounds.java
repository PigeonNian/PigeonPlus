package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class AddonSounds {
    private static final DeferredRegister<SoundEvent> REGISTER =
        DeferredRegister.create(Registries.SOUND_EVENT, AnvilCraftPigeonPlus.MOD_ID);
    public static final float ENGINE_ON_RANGE = 48.0F;
    public static final float ENGINE_FIRE_RANGE = 64.0F;

    public static final Supplier<SoundEvent> ENGINE_ON = REGISTER.register(
        "engine_on",
        () -> SoundEvent.createFixedRangeEvent(AnvilCraftPigeonPlus.of("engine_on"), ENGINE_ON_RANGE)
    );

    public static final Supplier<SoundEvent> ENGINE_FIRE = REGISTER.register(
        "engine_fire",
        () -> SoundEvent.createFixedRangeEvent(AnvilCraftPigeonPlus.of("engine_fire"), ENGINE_FIRE_RANGE)
    );

    private AddonSounds() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
