package dev.anvilcraft.pigeonplus.data.lang;

import dev.anvilcraft.lib.v2.config.ConfigData;
import dev.anvilcraft.pigeonplus.AddonClientConfig;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;
import dev.anvilcraft.pigeonplus.client.tooltip.AddonItemTooltipManager;
import net.minecraft.world.item.Item;

import java.util.Map;

public class AddonLangHandler {

    /**
     * 鐠囶叀鈻堥弬鍥︽閸掓繂顫愰崠?
     *
     * @param provider 閹绘劒绶甸崳?
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
        provider.add("advancements.anvilcraft_pigeon_plus.paper_block.title", "A Stack of Paper");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.paper_block.description",
            "Compress paper into a block"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.compressed_bottle.title", "Bottle That Bursts");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.compressed_bottle.description",
            "Compress a glass bottle into a splash or lingering bottle"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.liquid_chocolate.title", "Liquid Happiness");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.liquid_chocolate.description",
            "Use a blender to melt chocolate into a fluid"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.chocolate_cookie_pillar.title", "Filled Cookie Pillar");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.chocolate_cookie_pillar.description",
            "Pour a bucket of liquid chocolate into a cookie pillar"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.large_creamy_bread_roll.title", "Giant Creamy Bread Roll");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.large_creamy_bread_roll.description",
            "Assemble a 3x3 creamy bread roll"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.eat_bread_roll.title", "Shovel It In");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.eat_bread_roll.description",
            "Eat a whole bread roll using a shovel as a spoon"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.feed_spreader.title", "Feed the World");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.feed_spreader.description",
            "Place a feed spreader and press it with an anvil"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.mixed_biomass.title", "Mixed Biomass");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.mixed_biomass.description",
            "Blend crops with biomass into a mixed biomass fluid"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.gaseous_biogas.title", "First Whiff of Biogas");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.gaseous_biogas.description",
            "Ferment mixed biomass into gaseous biogas"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.liquefied_biogas.title", "Squeezed to Liquid");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.liquefied_biogas.description",
            "Liquefy gaseous biogas"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.anvil_pump.title", "Anvil Driven");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.anvil_pump.description",
            "Place an anvil pump"
        );

        provider.add("advancements.anvilcraft_pigeon_plus.compressed_air.title", "Put the Air In");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.compressed_air.description",
            "Extract compressed air through a drain driven by a pump"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.liquid_oxygen.title", "Liquid Oxygen");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.liquid_oxygen.description",
            "Liquefy compressed air into liquid oxygen"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.liquid_hydrogen.title", "Liquid Hydrogen");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.liquid_hydrogen.description",
            "Liquefy compressed air into liquid hydrogen"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.blender.title", "Pneumatic Blender");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.blender.description",
            "Place a pneumatic blender"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.electric_blender.title", "Powered Blender");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.electric_blender.description",
            "Place an electric blender"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.stasis_beacon.title", "Time, Paused");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.stasis_beacon.description",
            "Place a stasis beacon"
        );

        provider.add("advancements.anvilcraft_pigeon_plus.large_fireworks_rocket.title", "Faster Than NASA");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.large_fireworks_rocket.description",
            "Build a large fireworks rocket"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.rocket_rider_killed.title", "Escape Tower Failure");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.rocket_rider_killed.description",
            "Ride a large fireworks rocket and go down with it"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.rocket_moon_arrival.title", "One Giant Leap");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.rocket_moon_arrival.description",
            "Ride a large fireworks rocket all the way to the moon"
        );
        provider.add("advancements.anvilcraft_pigeon_plus.rocket_launch.title", "Liftoff");
        provider.add(
            "advancements.anvilcraft_pigeon_plus.rocket_launch.description",
            "Ignite a large fireworks rocket and send it flying"
        );
        provider.add("block.anvilcraft_pigeon_plus.gaseous_biogas", "Gaseous Biogas");
        provider.add("block.anvilcraft_pigeon_plus.liquefied_biogas", "Liquefied Biogas");
        provider.add("block.anvilcraft_pigeon_plus.compressed_air", "Compressed Air");
        provider.add("block.anvilcraft_pigeon_plus.mixed_biomass", "Mixed Biomass");
        provider.add("block.anvilcraft_pigeon_plus.liquid_oxygen", "Liquid Oxygen");
        provider.add("block.anvilcraft_pigeon_plus.liquid_hydrogen", "Liquid Hydrogen");
        provider.add("block.anvilcraft_pigeon_plus.liquid_chocolate", "Liquid Chocolate");
        provider.add("block.anvilcraft_pigeon_plus.liquid_chocolate_dark", "Liquid Dark Chocolate");
        provider.add("block.anvilcraft_pigeon_plus.liquid_chocolate_white", "Liquid White Chocolate");
        provider.add("block.anvilcraft_pigeon_plus.potion", "Potion");
        provider.add("fluid.anvilcraft_pigeon_plus.potion.of", "%s%s");
        provider.add("fluid.anvilcraft_pigeon_plus.potion.long", "Long ");
        provider.add("fluid.anvilcraft_pigeon_plus.potion.strong", "Strong ");
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
        provider.add(
            "death.attack.escapeTowerFailure",
            "%1$s's escape tower failed to fire"
        );
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
        provider.add(
            "gui.anvilcraft_pigeon_plus.category.blending.need_heat",
            "Requires heat: a blaze head on the air blender, or powered electric blender"
        );
        provider.add("gui.anvilcraft_pigeon_plus.category.fermentation", "Fermentation");
        provider.add(
            "gui.anvilcraft_pigeon_plus.category.fermentation.requirement",
            "Requires an air blender and a heat source\nAn electric blender can serve as both"
        );
        provider.add("gui.anvilcraft_pigeon_plus.category.reforming", "Steam Reforming");
        provider.add(
            "gui.anvilcraft_pigeon_plus.category.reforming.requirement",
            "Requires a giant anvil to strike the cauldron\nAlso needs a working blender, a heat source and a magnet block\nAn electric blender can serve as both blender and heat"
        );
        provider.add("gui.anvilcraft_pigeon_plus.category.potion_brewing", "Potion Brewing");
        provider.add(
            "gui.anvilcraft_pigeon_plus.category.potion_brewing.whole_tank",
            "Converts all potion in the container"
        );
        provider.add(
            "gui.anvilcraft_pigeon_plus.category.potion_brewing.need_blender",
            "Requires an air blender with a blaze head"
        );
        provider.add("gui.anvilcraft_pigeon_plus.category.potion_bulk_brewing", "Bulk Potion Brewing");
        provider.add(
            "gui.anvilcraft_pigeon_plus.category.potion_bulk_brewing.bulk",
            "Brews 8B at once, using only one reagent"
        );
        provider.add(
            "gui.anvilcraft_pigeon_plus.category.potion_bulk_brewing.need_blender",
            "Requires a running electric blender with a blaze head"
        );
        provider.add("gui.anvilcraft_pigeon_plus.category.potion_soaking", "Potion Soaking");
        provider.add(
            "message.anvilcraft_pigeon_plus.large_fireworks_rocket.installed",
            "Loaded firework star %s/%s"
        );
        provider.add(
            "message.anvilcraft_pigeon_plus.large_fireworks_rocket.full",
            "This large firework is already full (%s stars)"
        );
        provider.add(
            "message.anvilcraft_pigeon_plus.large_fireworks_rocket.pearl_installed",
            "Loaded ender pearl %s/%s"
        );
        provider.add(
            "message.anvilcraft_pigeon_plus.large_fireworks_rocket.pearl_full",
            "This large firework already holds the maximum %s ender pearls"
        );
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
