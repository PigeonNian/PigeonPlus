package dev.anvilcraft.pigeonplus.data.lang;

import dev.anvilcraft.lib.v2.config.ConfigData;
import dev.anvilcraft.pigeonplus.AddonClientConfig;
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
        ConfigData.readConfigClass(provider, AddonClientConfig.class);
        provider.add("advancements.anvilcraft_pigeon_plus.root.title", "AnvilCraft: Pigeon Plus");
        provider.add("advancements.anvilcraft_pigeon_plus.root.description", "Welcome to the Pigeon Plus addon");
        provider.add("advancements.anvilcraft_pigeon_plus.nozzle_ignition.title", "The test pad is launch pad!");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.nozzle_ignition.description",
            "Activate a nozzle to convert mixed combustion in the large cauldron into a directed gas jet"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.nozzle_explosion.title", "Rich Engine Combustion");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.nozzle_explosion.description",
            "Seal a firing nozzle completely and let it blow itself up"
        );
        provider.add("block.anvilcraft_pigeon_plus.gaseous_biogas", "Gaseous Biogas");
        provider.add("block.anvilcraft_pigeon_plus.liquefied_biogas", "Liquefied Biogas");
        provider.add("block.anvilcraft_pigeon_plus.compressed_air", "Compressed Air");
        provider.add("block.anvilcraft_pigeon_plus.mixed_biomass", "Mixed Biomass");
        provider.add("block.anvilcraft_pigeon_plus.liquid_oxygen", "Liquid Oxygen");
        provider.add("block.anvilcraft_pigeon_plus.liquid_hydrogen", "Liquid Hydrogen");
        provider.add("subtitles.anvilcraft_pigeon_plus.engine_on", "Nozzle engine starts");
        provider.add("subtitles.anvilcraft_pigeon_plus.engine_fire", "Nozzle engine roars");
        provider.add("subtitles.anvilcraft_pigeon_plus.rocket_punch_cast", "Rocket Punch");
        provider.add("subtitles.anvilcraft_pigeon_plus.rocket_punch_charge", "Rocket Punch charges");
        provider.add("subtitles.anvilcraft_pigeon_plus.rocket_punch_hit", "Rocket Punch hits");
        provider.add("subtitles.anvilcraft_pigeon_plus.rocket_punch_wall_slam", "Slammed into a wall");
        provider.add("subtitles.anvilcraft_pigeon_plus.uppercut_cast", "Uppercut");
        provider.add("death.attack.nozzleExhaust", "%1$s sat in the duct seat");
        provider.add("death.attack.rocketPunch", "%1$s was punched into next week by %2$s");
        provider.add("death.attack.rocketPunchWallSlam", "%1$s was slammed into a wall by %2$s");
        provider.add("death.attack.uppercut", "%1$s was launched skyward by %2$s");
        provider.add("death.attack.seismicSlam", "%1$s was flattened by %2$s");
        provider.add("subtitles.anvilcraft_pigeon_plus.seismic_slam_leap", "Seismic Slam leaps");
        provider.add("subtitles.anvilcraft_pigeon_plus.seismic_slam_impact", "Seismic Slam impacts");
        provider.add("skill.anvilcraft_pigeon_plus.seismic_slam", "Seismic Slam");
        provider.add("skill.anvilcraft_pigeon_plus.rocket_punch", "Rocket Punch");
        provider.add("skill.anvilcraft_pigeon_plus.uppercut", "Uppercut");
        provider.add("effect.anvilcraft_pigeon_plus.stun", "Stun");
        provider.add("enchantment.anvilcraft_pigeon_plus.doomfist", "Doomfist");
        provider.add(
            "enchantment.anvilcraft_pigeon_plus.doomfist.desc",
            "Turns the Anvil Hammer into a pure weapon: it can no longer rotate or break blocks, open a portable anvil, rocket jump, or act as goggles."
        );
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
