package dev.anvilcraft.pigeonplus.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;
import dev.anvilcraft.pigeonplus.client.tooltip.AddonItemTooltipManager;
import net.minecraft.world.item.Item;

import java.util.Map;

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
        provider.add("gui.anvilcraft_pigeon_plus.category.blending", "Blending");
        provider.add("gui.anvilcraft_pigeon_plus.category.gas_liquefaction", "Gas Liquefaction");
        provider.add("gui.anvilcraft_pigeon_plus.gas_liquefaction.fill_then", "After Full");
        provider.add("gui.anvilcraft_pigeon_plus.gas_liquefaction.keep_pumping", "Keep Pumping");
        provider.add("gui.anvilcraft_pigeon_plus.gas_liquefaction.liquefy", "Liquefy");
        provider.add("tooltip.anvilcraft_pigeon_plus.press_key", AddonItemTooltipManager.PRESS_KEY);
        for (Map.Entry<Item, String> entry : AddonItemTooltipManager.getNormalMap().entrySet()) {
            provider.add(AddonItemTooltipManager.getTranslationKey(entry.getKey()), entry.getValue());
        }
        for (Map.Entry<Item, String> entry : AddonItemTooltipManager.getShiftMap().entrySet()) {
            provider.add(AddonItemTooltipManager.getShiftTranslationKey(entry.getKey()), entry.getValue());
        }
    }
}
