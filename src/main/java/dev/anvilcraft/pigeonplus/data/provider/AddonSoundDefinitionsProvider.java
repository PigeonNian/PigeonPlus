package dev.anvilcraft.pigeonplus.data.provider;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonSounds;
import net.minecraft.data.PackOutput;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

public class AddonSoundDefinitionsProvider extends SoundDefinitionsProvider {
    /**
     * 26.1 起 {@code ExistingFileHelper} 已被移除，
     * 父类构造器只收 {@code PackOutput} 与 mod id。
     */
    public AddonSoundDefinitionsProvider(PackOutput output) {
        super(output, AnvilCraftPigeonPlus.MOD_ID);
    }

    @Override
    public void registerSounds() {
        add(AddonSounds.ENGINE_ON.get(), definition()
            .subtitle("subtitles.anvilcraft_pigeon_plus.engine_on")
            .with(sound(AnvilCraftPigeonPlus.of("engine_on"))
                .attenuationDistance((int) AddonSounds.ENGINE_ON_RANGE)));

        add(AddonSounds.ENGINE_FIRE.get(), definition()
            .subtitle("subtitles.anvilcraft_pigeon_plus.engine_fire")
            .with(sound(AnvilCraftPigeonPlus.of("engine_fire"))
                .attenuationDistance((int) AddonSounds.ENGINE_FIRE_RANGE)));

        // 铁拳技能音效：用 type:"event" 转发回原版事件。
        // 这样不需要额外的 .ogg 文件，听感与直接播放原版音效一致，
        // 而资源包可以按 anvilcraft_pigeon_plus:rocket_punch_cast 这样的名字单独替换。
        add(AddonSounds.ROCKET_PUNCH_CAST.get(), definition()
            .subtitle("subtitles.anvilcraft_pigeon_plus.rocket_punch_cast")
            .with(sound(SoundEvents.MACE_SMASH_GROUND_HEAVY.location(), SoundDefinition.SoundType.EVENT)));

        add(AddonSounds.ROCKET_PUNCH_CHARGE.get(), definition()
            .subtitle("subtitles.anvilcraft_pigeon_plus.rocket_punch_charge")
            .with(sound(SoundEvents.RESPAWN_ANCHOR_CHARGE.location(), SoundDefinition.SoundType.EVENT)));

        add(AddonSounds.ROCKET_PUNCH_HIT.get(), definition()
            .subtitle("subtitles.anvilcraft_pigeon_plus.rocket_punch_hit")
            .with(sound(SoundEvents.MACE_SMASH_GROUND.location(), SoundDefinition.SoundType.EVENT)));

        add(AddonSounds.ROCKET_PUNCH_WALL_SLAM.get(), definition()
            .subtitle("subtitles.anvilcraft_pigeon_plus.rocket_punch_wall_slam")
            .with(sound(SoundEvents.ANVIL_LAND.location(), SoundDefinition.SoundType.EVENT)));

        add(AddonSounds.UPPERCUT_CAST.get(), definition()
            .subtitle("subtitles.anvilcraft_pigeon_plus.uppercut_cast")
            .with(sound(SoundEvents.MACE_SMASH_GROUND.location(), SoundDefinition.SoundType.EVENT)));

        add(AddonSounds.SEISMIC_SLAM_LEAP.get(), definition()
            .subtitle("subtitles.anvilcraft_pigeon_plus.seismic_slam_leap")
            .with(sound(SoundEvents.BREEZE_JUMP.location(), SoundDefinition.SoundType.EVENT)));

        add(AddonSounds.SEISMIC_SLAM_IMPACT.get(), definition()
            .subtitle("subtitles.anvilcraft_pigeon_plus.seismic_slam_impact")
            .with(sound(SoundEvents.MACE_SMASH_GROUND_HEAVY.location(), SoundDefinition.SoundType.EVENT)));
    }
}
