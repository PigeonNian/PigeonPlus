package dev.anvilcraft.pigeonplus.data.provider;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonSounds;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

public class AddonSoundDefinitionsProvider extends SoundDefinitionsProvider {
    public AddonSoundDefinitionsProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, AnvilCraftPigeonPlus.MOD_ID, fileHelper);
    }

    @Override
    public void registerSounds() {
        add(AddonSounds.ENGINE_ON, definition()
            .subtitle("subtitles.anvilcraft_pigeon_plus.engine_on")
            .with(sound(AnvilCraftPigeonPlus.of("engine_on"))
                .attenuationDistance((int) AddonSounds.ENGINE_ON_RANGE)));

        add(AddonSounds.ENGINE_FIRE, definition()
            .subtitle("subtitles.anvilcraft_pigeon_plus.engine_fire")
            .with(sound(AnvilCraftPigeonPlus.of("engine_fire"))
                .attenuationDistance((int) AddonSounds.ENGINE_FIRE_RANGE)));
    }
}
