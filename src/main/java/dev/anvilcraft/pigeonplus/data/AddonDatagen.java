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
    /**
     * 注册客户端侧的数据生成器（音效定义）。
     *
     * <p>26.1 的 {@code GatherDataEvent} 变成了<b>抽象类</b>，
     * 只监听它会在加载时报
     * 「Cannot register listeners for abstract class GatherDataEvent」。
     * 必须监听具体子类 {@link GatherDataEvent.Client} 或
     * {@code GatherDataEvent.Server}。音效定义属于客户端资源，故用 Client。
     */
    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
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
