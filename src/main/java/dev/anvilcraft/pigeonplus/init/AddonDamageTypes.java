package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class AddonDamageTypes {
    public static final ResourceKey<DamageType> NOZZLE_EXHAUST = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        AnvilCraftPigeonPlus.of("nozzle_exhaust")
    );

    /**
     * 火箭重拳直接命中造成的伤害。
     */
    public static final ResourceKey<DamageType> ROCKET_PUNCH = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        AnvilCraftPigeonPlus.of("rocket_punch")
    );

    /**
     * 被火箭重拳击飞的敌人撞上墙壁时的额外伤害。
     */
    public static final ResourceKey<DamageType> ROCKET_PUNCH_WALL_SLAM = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        AnvilCraftPigeonPlus.of("rocket_punch_wall_slam")
    );

    /**
     * 上勾拳造成的伤害。
     */
    public static final ResourceKey<DamageType> UPPERCUT = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        AnvilCraftPigeonPlus.of("uppercut")
    );

    private AddonDamageTypes() {
    }

    public static DamageSource nozzleExhaust(Level level) {
        return level.damageSources().source(NOZZLE_EXHAUST);
    }

    public static DamageSource rocketPunch(Level level, @Nullable Entity attacker) {
        return level.damageSources().source(ROCKET_PUNCH, attacker);
    }

    public static DamageSource rocketPunchWallSlam(Level level, @Nullable Entity attacker) {
        return level.damageSources().source(ROCKET_PUNCH_WALL_SLAM, attacker);
    }

    public static DamageSource uppercut(Level level, @Nullable Entity attacker) {
        return level.damageSources().source(UPPERCUT, attacker);
    }
}
