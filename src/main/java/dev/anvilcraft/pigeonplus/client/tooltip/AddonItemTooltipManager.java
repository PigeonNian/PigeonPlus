package dev.anvilcraft.pigeonplus.client.tooltip;

import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AddonItemTooltipManager {
    public static final String PRESS_KEY = "Hold %s for details";

    private static final Map<Item, String> NORMAL = new HashMap<>();
    private static final Map<Item, String> SHIFT = new HashMap<>();

    static {
        normal(AddonBlocks.NOZZLE.asItem(), "Forms a directional nozzle exhaust from propellant in a large cauldron");
        normal(AddonBlocks.STASIS_BEACON.asItem(), "Freezes one non-player entity or falling block inside its beam");
        normal(
            AddonBlocks.ANVIL_PUMP.asItem(),
            """
                Pumps fluids and gases with anvil-driven pressure
                The flow rate and head are determined by the falling height of the anvil"""
        );
        normal(AddonBlocks.FEED_SPREADER.asItem(), "Spreads feed or bone meal when pressed by an anvil");
        normal(AddonBlocks.BLENDER.asItem(), "Blends fluid ingredients under a cauldron");
        normal(
            AddonItems.GASEOUS_BIOGAS_BUCKET.asItem(),
            """
                Gas, can be liquefied by continued pumping
                """
        );
        normal(AddonItems.LIQUEFIED_BIOGAS_BUCKET.asItem(), "Liquid methane-rich fuel for nozzle exhaust");
        normal(
            AddonItems.COMPRESSED_AIR_BUCKET.asItem(),
                """
                Compressed gas, can be liquefied into liquid oxygen
                Can be obtained by pumping from the drain port exposed to air"""
        );
        normal(
            AddonItems.MIXED_BIOMASS_BUCKET.asItem(), """
                Very fresh, very delicious
                Can be converted into gaseous biogas in a heated cauldron"""
        );
        normal(AddonItems.LIQUID_OXYGEN_BUCKET.asItem(), "Oxidizer for nozzle propellant reactions");
        shift(
            AddonBlocks.FEED_SPREADER.asItem(),
            """
                Anvil height controls the square working range, up to 9x9
                When ripening crops, each crop has a 30% chance to consume bone meal"""
        );

        shift(
            AddonBlocks.STASIS_BEACON.asItem(),
            """
                Requires a Frost Metal Block beacon base
                Stores incoming damage and momentum while time is stopped
                Releases after 30 seconds or 5 hearts of accumulated damage"""
        );

        shift(
            AddonBlocks.NOZZLE.asItem(),
            """
                Requires a matching large cauldron propellant reaction
                Redstone signal disables the exhaust
                The center exhaust can heat blocks, damage entities, and apply momentum"""
        );


    }

    private AddonItemTooltipManager() {
    }

    public static void addTooltip(ItemStack stack, List<Component> tooltip) {
        Item item = stack.getItem();
        if (SHIFT.containsKey(item)) {
            if (Screen.hasShiftDown()) {
                addTranslatedTooltip(tooltip, getShiftTranslationKey(item));
            } else {
                if (NORMAL.containsKey(item)) {
                    addTranslatedTooltip(tooltip, getTranslationKey(item));
                }
                addLine(tooltip, Component.translatable(
                    "tooltip.anvilcraft_pigeon_plus.press_key",
                    Component.literal("[Shift]").withStyle(ChatFormatting.WHITE)
                ).withStyle(ChatFormatting.DARK_GRAY));
            }
            return;
        }
        if (NORMAL.containsKey(item)) {
            addTranslatedTooltip(tooltip, getTranslationKey(item));
        }
    }

    public static Map<Item, String> getNormalMap() {
        return Collections.unmodifiableMap(NORMAL);
    }

    public static Map<Item, String> getShiftMap() {
        return Collections.unmodifiableMap(SHIFT);
    }

    public static String getTranslationKey(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return "tooltip.%s.item.%s".formatted(key.getNamespace(), key.getPath());
    }

    public static String getShiftTranslationKey(Item item) {
        return getTranslationKey(item) + ".shift";
    }

    private static void normal(Item item, String tooltip) {
        NORMAL.put(item, tooltip);
    }

    private static void shift(Item item, String tooltip) {
        SHIFT.put(item, tooltip);
    }

    private static void addTranslatedTooltip(List<Component> tooltip, String key) {
        String text = I18n.get(key);
        if (text.equals(key)) {
            return;
        }
        String[] lines = text.split("\n");
        for (int index = lines.length - 1; index >= 0; index--) {
            addLine(tooltip, Component.literal(lines[index]).withStyle(ChatFormatting.GRAY));
        }
    }

    private static void addLine(List<Component> tooltip, Component component) {
        tooltip.add(Math.min(1, tooltip.size()), component);
    }
}
