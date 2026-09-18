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

    // ------------------------------------------------------------------ 铁拳技能
    //
    // 这些技能原本直接播放原版音效（如 SoundEvents.MACE_SMASH_GROUND_HEAVY）。
    // 改为独立声音事件后，资源包可以只覆盖某一条而不用整体改动；
    // 默认行为不变——sounds.json 里用 type:"event" 转发回原来那个原版事件，
    // 因此无需附带 .ogg 文件，听感与改动前完全一致。

    /** 火箭重拳释放（冲刺起步）。 */
    public static final Supplier<SoundEvent> ROCKET_PUNCH_CAST = REGISTER.register(
        "rocket_punch_cast",
        () -> SoundEvent.createVariableRangeEvent(AnvilCraftPigeonPlus.of("rocket_punch_cast"))
    );

    /** 火箭重拳开始蓄力。 */
    public static final Supplier<SoundEvent> ROCKET_PUNCH_CHARGE = REGISTER.register(
        "rocket_punch_charge",
        () -> SoundEvent.createVariableRangeEvent(AnvilCraftPigeonPlus.of("rocket_punch_charge"))
    );

    /** 火箭重拳命中敌人。 */
    public static final Supplier<SoundEvent> ROCKET_PUNCH_HIT = REGISTER.register(
        "rocket_punch_hit",
        () -> SoundEvent.createVariableRangeEvent(AnvilCraftPigeonPlus.of("rocket_punch_hit"))
    );

    /** 被火箭重拳击飞的敌人撞上墙壁。 */
    public static final Supplier<SoundEvent> ROCKET_PUNCH_WALL_SLAM = REGISTER.register(
        "rocket_punch_wall_slam",
        () -> SoundEvent.createVariableRangeEvent(AnvilCraftPigeonPlus.of("rocket_punch_wall_slam"))
    );

    /** 上勾拳释放。 */
    public static final Supplier<SoundEvent> UPPERCUT_CAST = REGISTER.register(
        "uppercut_cast",
        () -> SoundEvent.createVariableRangeEvent(AnvilCraftPigeonPlus.of("uppercut_cast"))
    );

    private AddonSounds() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
