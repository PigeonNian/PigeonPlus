package dev.anvilcraft.pigeonplus;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.pigeonplus.block.entity.ModBlockEntities;
import dev.anvilcraft.pigeonplus.data.AddonDatagen;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.anvilcraft.pigeonplus.init.AddonInteractionMap;
import dev.anvilcraft.pigeonplus.init.AddonItemGroups;
import dev.anvilcraft.pigeonplus.init.AddonItems;
import dev.anvilcraft.pigeonplus.init.AddonParticles;
import dev.anvilcraft.pigeonplus.init.AddonVaporizationSources;
import dev.anvilcraft.pigeonplus.init.AddonRecipeTypes;
import dev.anvilcraft.pigeonplus.init.AddonSounds;
import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.lib.v2.registrum.Registrum;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;

@Mod(AnvilCraftPigeonPlus.MOD_ID)
public class AnvilCraftPigeonPlus {
    public static final String MOD_ID = "anvilcraft_pigeon_plus";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final AddonConfig CONFIG = ConfigManager.register(AnvilCraftPigeonPlus.MOD_ID, AddonConfig::new);
    public static final Registrum REGISTRUM = Registrum.create(MOD_ID);

    public AnvilCraftPigeonPlus(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(ModBlockEntities::registerCapabilities);
        modEventBus.addListener(AnvilCraftPigeonPlus::loadComplete);
        AddonItemGroups.register(modEventBus);
        AddonFluids.register(modEventBus);
        AddonParticles.register(modEventBus);
        AddonSounds.register(modEventBus);
        AddonRecipeTypes.register(modEventBus);
        AddonVaporizationSources.register();
        AddonBlocks.register();
        AddonItems.register();
        AddonDatagen.init();
        ModBlockEntities.register(modEventBus);
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void loadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(AddonInteractionMap::init);
    }
}
