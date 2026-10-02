package dev.anvilcraft.pigeonplus.client.event;

import com.mojang.blaze3d.pipeline.RenderTarget;
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

/**
 * 在世界渲染结束后绘制静滞信标的延迟锁链。
 *
 * <p>26.1 起 {@code RenderLevelStageEvent} 由「一个事件 + {@code Stage} 枚举」
 * 改为「每个阶段一个事件子类」，因此不再需要 {@code getStage()} 判断，
 * 直接把参数声明成 {@link RenderLevelStageEvent.AfterLevel} 即可。
 *
 * <p>相机位置也换了来源：原 {@code getCamera()} 已移除，
 * 改从 {@code getLevelRenderState().cameraRenderState.pos} 取。
 */
@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID, value = Dist.CLIENT)
public class StasisBeaconRenderEventListener {
    @SubscribeEvent
    public static void onRenderAfterLevel(RenderLevelStageEvent.AfterLevel event) {
        // 26.1 移除了 RenderTarget#bindWrite —— 渲染改走 RenderPass 模型，
        // 此处只需把天气目标的深度拷进主目标即可（与 AnvilCraft 的做法一致）。
        RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        RenderTarget weatherTarget = event.getLevelRenderer().getWeatherTarget();
        if (weatherTarget != null) {
            mainTarget.copyDepthFrom(weatherTarget);
        }

        PoseStack poseStack = event.getPoseStack();
        // 不能用 event.getLevelRenderer().renderBuffers（该字段为 private），
        // 走 Minecraft 的公开访问器
        MultiBufferSource.BufferSource bufferSource =
            Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        poseStack.pushPose();
        poseStack.last().pose().mul(event.getModelViewMatrix());
        StasisBeaconBlockEntityRenderer.renderDeferredChains(poseStack, bufferSource, camera);
        poseStack.popPose();
        StasisBeaconBlockEntityRenderer.publishStasisEffectEntities();
    }
}
