package dev.anvilcraft.pigeonplus.data.lang;

import dev.anvilcraft.pigeonplus.AddonConfig;
import dev.anvilcraft.lib.v2.config.ConfigData;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class AddonLangHandler {

    /**
     * 语言文件初始化
     *
     * @param provider 提供器
     */
    public static void init(RegistrumLangProvider provider) {
        ConfigData.readConfigClass(provider, AddonConfig.class);
    }
}
