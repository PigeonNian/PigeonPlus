package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.pigeonplus.block.StasisBeaconBlock;
import dev.anvilcraft.pigeonplus.block.entity.StasisBeaconBlockEntity;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StasisBeaconBlockEntityRenderer implements BlockEntityRenderer<StasisBeaconBlockEntity> {
    private static final float BEAM_BASE_Y = 0.5f;
    private static final float BEAM_INNER_HALF = 0.08f;
    private static final int BEAM_GLOW_LAYERS = 4;
    private static final float BEAM_GLOW_HALF_STEP = 0.06f;
    private static final float BEAM_R = 0.1f;
    private static final float BEAM_G = 0.75f;
    private static final float BEAM_B = 1.0f;
    private static final BlockState CHAIN_STATE = Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, net.minecraft.core.Direction.Axis.Y);
    private static final float CHAIN_SEGMENT_SCALE = 0.36f;
    private static final float CHAIN_SEGMENT_SPACING = 0.36f;
    private static final int CHAIN_ALPHA = 145;
    private static final int CHAIN_END_ALPHA = 2;
    private static final float CHAIN_FADE_START = 0.35f;
    private static final int CHAIN_R = 70;
    private static final int CHAIN_G = 225;
    private static final int CHAIN_B = 255;
    private static final Vec3[] CHAIN_DIRECTIONS = {
        new Vec3(1.0, 0.12, 0.0),
        new Vec3(-1.0, 0.18, 0.25),
        new Vec3(0.25, 0.95, 0.1),
        new Vec3(-0.2, 0.85, -0.45),
        new Vec3(0.1, -0.35, 1.0),
        new Vec3(-0.35, -0.22, -1.0)
    };
    private static final float[] CHAIN_LENGTHS = {4.1f, 3.7f, 3.5f, 4.0f, 3.2f, 3.4f};
    private static final List<BeamRenderData> DEFERRED_BEAMS = new ArrayList<>();
    private static final List<ChainRenderData> DEFERRED_CHAINS = new ArrayList<>();
    private static final Set<Integer> ACTIVE_STASIS_EFFECT_ENTITY_IDS = new HashSet<>();
    private static final Set<Integer> NEXT_STASIS_EFFECT_ENTITY_IDS = new HashSet<>();

    private record BeamRenderData(BlockPos pos, int beamTopY) {
    }

    private record ChainRenderData(Vec3 entityCenter, float entityWidth, float tickTime) {
    }

    public StasisBeaconBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        StasisBeaconBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        BlockState state = level.getBlockState(blockEntity.getBlockPos());
        if (!state.hasProperty(StasisBeaconBlock.LIT) || !state.getValue(StasisBeaconBlock.LIT)) {
            return;
        }

        int beamTopY = blockEntity.getBeamHeight();
        if (beamTopY > blockEntity.getBlockPos().getY() + 1) {
            DEFERRED_BEAMS.add(new BeamRenderData(blockEntity.getBlockPos(), beamTopY));
        }

        if (level instanceof ClientLevel clientLevel && blockEntity.getFrozenEntityClientId() >= 0) {
            Entity entity = clientLevel.getEntity(blockEntity.getFrozenEntityClientId());
            if (entity != null) {
                NEXT_STASIS_EFFECT_ENTITY_IDS.add(entity.getId());
                Vec3 entityCenter = entity.getPosition(partialTick).add(0.0, entity.getBbHeight() * 0.55, 0.0);
                DEFERRED_CHAINS.add(new ChainRenderData(
                    entityCenter,
                    Math.max(entity.getBbWidth(), 0.65f),
                    level.getGameTime() + partialTick
                ));
            }
        }
    }

    public static boolean hasStasisEffect(Entity entity) {
        return ACTIVE_STASIS_EFFECT_ENTITY_IDS.contains(entity.getId());
    }

    public static void publishStasisEffectEntities() {
        ACTIVE_STASIS_EFFECT_ENTITY_IDS.clear();
        ACTIVE_STASIS_EFFECT_ENTITY_IDS.addAll(NEXT_STASIS_EFFECT_ENTITY_IDS);
        NEXT_STASIS_EFFECT_ENTITY_IDS.clear();
    }

    public static void renderDeferredBeams(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camera) {
        if (DEFERRED_BEAMS.isEmpty()) {
            return;
        }

        VertexConsumer vertexConsumer = bufferSource.getBuffer(ModRenderTypes.CORRUPTED_BEACON_BEAM);
        for (BeamRenderData data : DEFERRED_BEAMS) {
            poseStack.pushPose();
            poseStack.translate(
                data.pos.getX() - camera.x,
                data.pos.getY() - camera.y,
                data.pos.getZ() - camera.z
            );

            float beamHeight = (float) (data.beamTopY - data.pos.getY()) - BEAM_BASE_Y;
            if (beamHeight > 0.01f) {
                renderBeam(vertexConsumer, poseStack.last(), 0.5f, BEAM_BASE_Y, 0.5f, beamHeight);
            }

            poseStack.popPose();
        }
        DEFERRED_BEAMS.clear();

        if (!DEFERRED_CHAINS.isEmpty()) {
            for (ChainRenderData data : DEFERRED_CHAINS) {
                renderStasisChains(poseStack, bufferSource, camera, data);
            }
            DEFERRED_CHAINS.clear();
        }
    }

    private static void renderStasisChains(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        Vec3 camera,
        ChainRenderData data
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        for (int i = 0; i < CHAIN_DIRECTIONS.length; i++) {
            Vec3 direction = CHAIN_DIRECTIONS[i].normalize();
            float length = CHAIN_LENGTHS[i] + data.entityWidth * 0.35f;
            Vec3 start = data.entityCenter;
            renderChainModelSegments(poseStack, bufferSource, camera, minecraft, start, direction, length);
        }
    }

    private static void renderChainModelSegments(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        Vec3 camera,
        Minecraft minecraft,
        Vec3 start,
        Vec3 direction,
        float length
    ) {
        Quaternionf rotation = new Quaternionf().rotationTo(
            0.0f,
            1.0f,
            0.0f,
            (float) direction.x,
            (float) direction.y,
            (float) direction.z
        );
        for (float distance = 0.2f; distance < length; distance += CHAIN_SEGMENT_SPACING) {
            int alpha = chainAlpha(distance / length);
            Vec3 point = start.add(direction.scale(distance)).subtract(camera);
            poseStack.pushPose();
            poseStack.translate(point.x, point.y, point.z);
            poseStack.mulPose(rotation);
            poseStack.scale(CHAIN_SEGMENT_SCALE, CHAIN_SEGMENT_SCALE, CHAIN_SEGMENT_SCALE);
            poseStack.translate(-0.5f, -0.5f, -0.5f);
            minecraft.getBlockRenderer().renderSingleBlock(
                CHAIN_STATE,
                poseStack,
                new TintedChainBufferSource(bufferSource, alpha),
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY
            );
            poseStack.popPose();
        }
    }

    private static int chainAlpha(float progress) {
        if (progress <= CHAIN_FADE_START) {
            return CHAIN_ALPHA;
        }
        float fade = (progress - CHAIN_FADE_START) / (1.0f - CHAIN_FADE_START);
        return (int) (CHAIN_ALPHA + (CHAIN_END_ALPHA - CHAIN_ALPHA) * Math.min(fade, 1.0f));
    }

    private record TintedChainBufferSource(MultiBufferSource delegate, int alpha) implements MultiBufferSource {
        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return new TintedChainVertexConsumer(this.delegate.getBuffer(RenderType.translucent()), this.alpha);
        }
    }

    private record TintedChainVertexConsumer(VertexConsumer delegate, int alpha) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return this.delegate.addVertex(x, y, z);
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this.delegate.setColor(CHAIN_R, CHAIN_G, CHAIN_B, this.alpha);
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this.delegate.setUv(u, v);
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this.delegate.setUv1(u, v);
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this.delegate.setUv2(u, v);
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this.delegate.setNormal(x, y, z);
        }
    }

    private static void renderBeam(
        VertexConsumer vertexConsumer,
        PoseStack.Pose pose,
        float centerX,
        float baseY,
        float centerZ,
        float length
    ) {
        float apexY = baseY + length;
        for (int layer = BEAM_GLOW_LAYERS; layer >= 1; layer--) {
            float half = BEAM_INNER_HALF + BEAM_GLOW_HALF_STEP * layer;
            float falloff = 1.0f / (layer + 1);
            falloff *= falloff;
            float alpha = 0.45f * falloff;
            float tipFade = 0.3f * falloff;
            emitBeamPyramid(vertexConsumer, pose, centerX, baseY, centerZ, half, apexY, alpha, tipFade);
        }
        emitBeamPyramid(vertexConsumer, pose, centerX, baseY, centerZ, BEAM_INNER_HALF, apexY, 0.82f, 0.25f);
    }

    private static void emitBeamPyramid(
        VertexConsumer vertexConsumer,
        PoseStack.Pose pose,
        float centerX,
        float baseY,
        float centerZ,
        float halfWidth,
        float apexY,
        float alpha,
        float tipFade
    ) {
        float x0 = centerX - halfWidth;
        float x1 = centerX + halfWidth;
        float z0 = centerZ - halfWidth;
        float z1 = centerZ + halfWidth;
        float[][] corners = {
            {x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}
        };
        float tipAlpha = alpha * tipFade;
        for (int i = 0; i < 4; i++) {
            float[] c0 = corners[i];
            float[] c1 = corners[(i + 1) % 4];
            vertexConsumer.addVertex(pose, c0[0], baseY, c0[1]).setColor(BEAM_R, BEAM_G, BEAM_B, alpha);
            vertexConsumer.addVertex(pose, c1[0], baseY, c1[1]).setColor(BEAM_R, BEAM_G, BEAM_B, alpha);
            vertexConsumer.addVertex(pose, centerX, apexY, centerZ).setColor(BEAM_R, BEAM_G, BEAM_B, tipAlpha);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(StasisBeaconBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRender(StasisBeaconBlockEntity blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos())
            .multiply(1.0, 0.0, 1.0)
            .closerThan(cameraPos.multiply(1.0, 0.0, 1.0), this.getViewDistance());
    }

    @Override
    public AABB getRenderBoundingBox(StasisBeaconBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        int topY = Math.max(blockEntity.getBeamHeight(), pos.getY() + 1);
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, topY, pos.getZ() + 1.0);
    }
}
