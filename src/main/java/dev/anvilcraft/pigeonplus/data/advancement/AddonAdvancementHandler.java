package dev.anvilcraft.pigeonplus.data.advancement;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumAdvancementProvider;

public class AddonAdvancementHandler {
    public static void init(RegistrumAdvancementProvider provider) {
        provider.accept(AddonAdvancements.ROOT);
        provider.accept(AddonAdvancements.IGNITE_NOZZLE);
        provider.accept(AddonAdvancements.NOZZLE_EXPLOSION);
    }
}
