package dev.anvilcraft.pigeonplus.data;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.data.advancement.AddonAdvancementHandler;
import dev.anvilcraft.pigeonplus.data.lang.AddonLangHandler;
import dev.anvilcraft.pigeonplus.data.provider.AddonSoundDefinitionsProvider;
import dev.anvilcraft.pigeonplus.data.recipe.AddonRecipeHandler;
import dev.anvilcraft.pigeonplus.init.AddonEnchantments;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import static dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus.REGISTRUM;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class AddonDatagen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        // 26.1 的 GatherDataEvent 移除了 includeClient() 开关；
        // 且 DataGenerator#addProvider 需要 (boolean, provider) 两个参数，
        // 故改用事件自身的 addProvider(T)，由它代为处理这些细节。
        event.addProvider(new AddonSoundDefinitionsProvider(event.getGenerator().getPackOutput()));
    }

    /**
     * 初始化生成器
     */
    public static void init() {
        REGISTRUM.getDataGenInitializer().add(Registries.ENCHANTMENT, AddonEnchantments::bootstrap);
        REGISTRUM.addDataGenerator(ProviderType.LANG, AddonLangHandler::init);
        REGISTRUM.addDataGenerator(ProviderType.RECIPE, AddonRecipeHandler::init);
        REGISTRUM.addDataGenerator(ProviderType.ADVANCEMENT, AddonAdvancementHandler::init);
    }
}
