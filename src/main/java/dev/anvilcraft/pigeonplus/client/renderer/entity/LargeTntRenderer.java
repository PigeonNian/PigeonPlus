package dev.anvilcraft.pigeonplus.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.pigeonplus.block.LargeTntBlock;
import dev.anvilcraft.pigeonplus.entity.LargeTntEntity;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jspecify.annotations.NonNull;

public class LargeTntRenderer extends EntityRenderer<LargeTntEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public LargeTntRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 1.5F;
    }

    @Override
    public @NonNull ResourceLocation getTextureLocation(@NonNull LargeTntEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(
        @NonNull LargeTntEntity entity,
        float entityYaw,
        float partialTick,
        @NonNull PoseStack poseStack,
        @NonNull MultiBufferSource buffer,
        int packedLight
    ) {
        BlockState state = AddonBlocks.LARGE_TNT.get().defaultBlockState()
            .setValue(LargeTntBlock.PART, Cube3x3PartHalf.MID_CENTER);

        poseStack.pushPose();
        // 引信最后 10 tick 逐级膨胀，与原版 TNT 的蓄势感一致。
        float pulse = 1.0F;
        float remaining = (float) entity.getFuse() - partialTick + 1.0F;
        if (remaining < 10.0F) {
            float swell = Mth.clamp(1.0F - remaining / 10.0F, 0.0F, 1.0F);
            swell *= swell;
            swell *= swell;
            pulse = 1.0F + swell * 0.3F;
        }
        // 碰撞箱虽然缩到 2.9，模型仍按 3 格绘制：外观与方块结构完全一致，不做缩放。
        poseStack.scale(pulse, pulse, pulse);
        poseStack.translate(-0.5, 1.0, -0.5);

        int overlay = entity.getFuse() / 5 % 2 == 0
            ? OverlayTexture.pack(OverlayTexture.u(1.0F), 10)
            : OverlayTexture.NO_OVERLAY;
        this.blockRenderer.renderSingleBlock(state, poseStack, buffer, packedLight, overlay, ModelData.EMPTY, null);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}
