package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class AddonParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
        DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, AnvilCraftPigeonPlus.MOD_ID);

    public static final Supplier<SimpleParticleType> ROLLING_PLASMA =
        PARTICLES.register("rolling_plasma", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> ROLLING_METHANE_PLASMA =
        PARTICLES.register("rolling_methane_plasma", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> ROLLING_HYDROGEN_PLASMA =
        PARTICLES.register("rolling_hydrogen_plasma", () -> new SimpleParticleType(false));

    private AddonParticles() {
    }

    public static void register(IEventBus modEventBus) {
        PARTICLES.register(modEventBus);
    }
}
