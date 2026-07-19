package dev.anvilcraft.pigeonplus.client;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = AnvilCraftPigeonPlus.MOD_ID, dist = Dist.CLIENT)
public class AnvilCraftPigeonPlusClient {
    public AnvilCraftPigeonPlusClient(IEventBus modBus, ModContainer container) {
    }
}
