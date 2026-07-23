package dev.anvilcraft.pigeonplus.client.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.client.support.NozzleScreenShakeManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID, value = Dist.CLIENT)
public final class NozzleScreenShakeEventListener {
    private NozzleScreenShakeEventListener() {
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        NozzleScreenShakeManager manager = NozzleScreenShakeManager.getInstance();
        if (!manager.isActive()) {
            return;
        }
        float[] offsets = manager.computeAngleOffsets();
        if (offsets == null) {
            return;
        }
        event.setYaw(event.getYaw() + offsets[0]);
        event.setPitch(event.getPitch() + offsets[1]);
        event.setRoll(event.getRoll() + offsets[2]);
    }
}
