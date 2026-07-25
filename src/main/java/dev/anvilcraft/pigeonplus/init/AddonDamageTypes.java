package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public final class AddonDamageTypes {
    public static final ResourceKey<DamageType> NOZZLE_EXHAUST = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        AnvilCraftPigeonPlus.of("nozzle_exhaust")
    );

    private AddonDamageTypes() {
    }

    public static DamageSource nozzleExhaust(Level level) {
        return level.damageSources().source(NOZZLE_EXHAUST);
    }
}
