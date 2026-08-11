package dev.anvilcraft.pigeonplus.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.client.renderer.block.StasisBeaconBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID, value = Dist.CLIENT)
public class StasisBeaconRenderEventListener {
    @SubscribeEvent
    public static void onRenderAfterLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        var levelRenderer = event.getLevelRenderer();
        var mainTarget = Minecraft.getInstance().getMainRenderTarget();
        var weatherTarget = levelRenderer.getWeatherTarget();
        if (weatherTarget != null) {
            mainTarget.copyDepthFrom(weatherTarget);
            mainTarget.bindWrite(false);
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.last().pose().mul(event.getModelViewMatrix());
        StasisBeaconBlockEntityRenderer.renderDeferredChains(poseStack, bufferSource, camera);
        bufferSource.endBatch();
        StasisBeaconBlockEntityRenderer.publishStasisEffectEntities();
        poseStack.popPose();
    }
}
