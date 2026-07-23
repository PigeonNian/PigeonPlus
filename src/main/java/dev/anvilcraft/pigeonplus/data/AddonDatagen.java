package dev.anvilcraft.pigeonplus.data;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.data.lang.AddonLangHandler;
import dev.anvilcraft.pigeonplus.data.provider.AddonSoundDefinitionsProvider;
import dev.anvilcraft.pigeonplus.data.recipe.AddonRecipeHandler;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import static dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus.REGISTRUM;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class AddonDatagen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
            event.includeClient(),
            new AddonSoundDefinitionsProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper())
        );
    }

    /**
     * 初始化生成器
     */
    public static void init() {
        REGISTRUM.addDataGenerator(ProviderType.LANG, AddonLangHandler::init);
        REGISTRUM.addDataGenerator(ProviderType.RECIPE, AddonRecipeHandler::init);
    }
}
