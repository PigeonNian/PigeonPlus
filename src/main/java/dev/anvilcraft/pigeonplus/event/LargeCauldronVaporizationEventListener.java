package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonVaporizationSources;
import dev.anvilcraft.lib.v2.yukkuri.api.event.LargeCauldronProcessEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public final class LargeCauldronVaporizationEventListener {
    private LargeCauldronVaporizationEventListener() {
    }

    @SubscribeEvent
    public static void onLargeCauldronProcess(LargeCauldronProcessEvent event) {
        if (event.phase() != LargeCauldronProcessEvent.Phase.BEFORE_VAPORIZATION) {
            return;
        }
        if (AddonVaporizationSources.tryProcessMethaneVaporization(event.context())) {
            return;
        }
        AddonVaporizationSources.tryProcessMixedVaporization(event.context());
    }
}
