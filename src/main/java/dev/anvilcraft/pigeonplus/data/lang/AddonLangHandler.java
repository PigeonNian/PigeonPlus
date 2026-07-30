package dev.anvilcraft.pigeonplus.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class AddonLangHandler {

    /**
     * 语言文件初始化
     *
     * @param provider 提供器
     */
    public static void init(RegistrumLangProvider provider) {
        provider.add("block.anvilcraft_pigeon_plus.gaseous_biogas", "Gaseous Biogas");
        provider.add("block.anvilcraft_pigeon_plus.liquefied_biogas", "Liquefied Biogas");
        provider.add("block.anvilcraft_pigeon_plus.compressed_air", "Compressed Air");
        provider.add("block.anvilcraft_pigeon_plus.mixed_biomass", "Mixed Biomass");
        provider.add("block.anvilcraft_pigeon_plus.liquid_oxygen", "Liquid Oxygen");
        provider.add("subtitles.anvilcraft_pigeon_plus.engine_on", "Nozzle engine starts");
        provider.add("subtitles.anvilcraft_pigeon_plus.engine_fire", "Nozzle engine roars");
        provider.add("death.attack.nozzleExhaust", "%1$s sat in the duct seat");
    }
}
