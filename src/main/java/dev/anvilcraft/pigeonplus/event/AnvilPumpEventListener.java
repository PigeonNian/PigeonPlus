package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.entity.AnvilPumpBlockEntity;
import dev.anvilcraft.pigeonplus.block.entity.FeedSpreaderBlockEntity;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class AnvilPumpEventListener {
    @SubscribeEvent
    public static void onAnvilLand(AnvilEvent.OnLand event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        BlockPos pumpPos = event.getPos().below();
        if (level.getBlockEntity(pumpPos) instanceof AnvilPumpBlockEntity pump) {
            pump.activate(event.getFallDistance());
        }
        if (level.getBlockEntity(pumpPos) instanceof FeedSpreaderBlockEntity feedSpreader) {
            feedSpreader.activate(event.getFallDistance());
        }
    }
}
