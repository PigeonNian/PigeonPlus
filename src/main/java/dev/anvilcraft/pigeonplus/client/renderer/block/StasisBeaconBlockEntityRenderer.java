package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.pigeonplus.block.StasisBeaconBlock;
import dev.anvilcraft.pigeonplus.block.entity.StasisBeaconBlockEntity;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 静滞信标的立柱光束与锁链特效。
 *
 * <h3>26.1 改写要点</h3>
 * <ul>
 *   <li>渲染器改三段式：光束在 {@code submit} 里通过
 *       {@link SubmitNodeCollector#submitCustomGeometry} 提交。</li>
 *   <li>锁链是<b>延迟</b>渲染的（要在世界渲染结束后、带着深度缓冲画到实体周围），
 *       因此它保留了「静态列表 + 渲染事件」的架构，不走 {@code submit}。
 *       它的缓冲来源 {@code Minecraft#renderBuffers().bufferSource()} 在 26.1 仍然可用。</li>
 *   <li>原版不再有 {@code BlockRenderDispatcher#renderSingleBlock}。
 *       改为取出 {@link BlockStateModel}、展开成
 *       {@link BlockStateModelPart}、再逐面用
 *       {@link VertexConsumer#putBakedQuad} 绘制。</li>
 *   <li>锁链的半透明淡出不再需要自己包一层 {@code VertexConsumer}
 *       （那要求实现全部抽象方法，且 26.1 新增了 {@code setLineWidth}）。
 *       现在只需给 {@link QuadInstance} 设颜色，{@code putBakedQuad} 会把它应用到四个顶点。</li>
 * </ul>
 */
public class StasisBeaconBlockEntityRenderer
    implements BlockEntityRenderer<StasisBeaconBlockEntity, StasisBeaconBlockEntityRenderer.State> {

    private static final float BEAM_BASE_Y = 0.5f;
    private static final float BEAM_INNER_HALF = 0.08f;
    private static final int BEAM_GLOW_LAYERS = 4;
    private static final float BEAM_GLOW_HALF_STEP = 0.06f;
    private static final float BEAM_R = 0.1f;
    private static final float BEAM_G = 0.75f;
    private static final float BEAM_B = 1.0f;
    private static final BlockState CHAIN_STATE =
        Blocks.IRON_CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);
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
    private static final List<ChainRenderData> DEFERRED_CHAINS = new ArrayList<>();
    private static final Set<Integer> ACTIVE_STASIS_EFFECT_ENTITY_IDS = new HashSet<>();
    private static final Set<Integer> NEXT_STASIS_EFFECT_ENTITY_IDS = new HashSet<>();

    private record ChainRenderData(Vec3 entityCenter, float entityWidth, float tickTime) {
    }

    public StasisBeaconBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        StasisBeaconBlockEntity blockEntity,
        State state,
        float partialTick,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        state.lit = false;
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        BlockState blockState = level.getBlockState(blockEntity.getBlockPos());
        if (!blockState.hasProperty(StasisBeaconBlock.LIT) || !blockState.getValue(StasisBeaconBlock.LIT)) {
            return;
        }

        int beamTopY = blockEntity.getBeamHeight();
        int posY = blockEntity.getBlockPos().getY();
        if (beamTopY > posY + 1) {
            state.lit = true;
            state.beamHeight = (float) (beamTopY - posY) - BEAM_BASE_Y;
        }

        // 被冻结实体的锁链：这里登记，稍后由渲染事件统一绘制
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

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.lit) {
            return;
        }
        collector.submitCustomGeometry(
            poseStack,
            ModRenderTypes.CORRUPTED_BEACON_BEAM,
            (pose, buffer) -> renderBeam(buffer, pose, 0.5f, BEAM_BASE_Y, 0.5f, state.beamHeight)
        );
    }

    public static boolean hasStasisEffect(Entity entity) {
        return ACTIVE_STASIS_EFFECT_ENTITY_IDS.contains(entity.getId());
    }

    public static void publishStasisEffectEntities() {
        ACTIVE_STASIS_EFFECT_ENTITY_IDS.clear();
        ACTIVE_STASIS_EFFECT_ENTITY_IDS.addAll(NEXT_STASIS_EFFECT_ENTITY_IDS);
        NEXT_STASIS_EFFECT_ENTITY_IDS.clear();
    }

    /**
     * 绘制延迟锁链。
     *
     * <p>由 {@code StasisBeaconRenderEventListener} 在世界渲染结束、深度缓冲可用时调用。
     */
    public static void renderDeferredChains(PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource, Vec3 camera) {
        if (DEFERRED_CHAINS.isEmpty()) {
            return;
        }
        // 锁链模型固定不变，整批只取一次
        List<BlockStateModelPart> parts = pigeonplus$chainParts();
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.translucentMovingBlock());
        for (ChainRenderData data : DEFERRED_CHAINS) {
            renderStasisChains(poseStack, buffer, parts, camera, data);
        }
        DEFERRED_CHAINS.clear();
    }

    private static void renderStasisChains(
        PoseStack poseStack,
        VertexConsumer buffer,
        List<BlockStateModelPart> parts,
        Vec3 camera,
        ChainRenderData data
    ) {
        for (int i = 0; i < CHAIN_DIRECTIONS.length; i++) {
            Vec3 direction = CHAIN_DIRECTIONS[i].normalize();
            float length = CHAIN_LENGTHS[i] + data.entityWidth * 0.35f;
            renderChainModelSegments(poseStack, buffer, parts, camera, data.entityCenter, direction, length);
        }
    }

    private static void renderChainModelSegments(
        PoseStack poseStack,
        VertexConsumer buffer,
        List<BlockStateModelPart> parts,
        Vec3 camera,
        Vec3 start,
        Vec3 direction,
        float length
    ) {
        Quaternionf rotation = new Quaternionf().rotationTo(
            0.0f, 1.0f, 0.0f,
            (float) direction.x, (float) direction.y, (float) direction.z
        );
        for (float distance = 0.2f; distance < length; distance += CHAIN_SEGMENT_SPACING) {
            int alpha = chainAlpha(distance / length);
            Vec3 point = start.add(direction.scale(distance)).subtract(camera);
            poseStack.pushPose();
            poseStack.translate(point.x, point.y, point.z);
            poseStack.mulPose(rotation);
            poseStack.scale(CHAIN_SEGMENT_SCALE, CHAIN_SEGMENT_SCALE, CHAIN_SEGMENT_SCALE);
            poseStack.translate(-0.5f, -0.5f, -0.5f);
            // 每个链节透明度不同，故各自一份 QuadInstance
            QuadInstance instance = new QuadInstance();
            instance.setColor((alpha << 24) | (CHAIN_R << 16) | (CHAIN_G << 8) | CHAIN_B);
            instance.setLightCoords(LightCoordsUtil.FULL_BRIGHT);
            instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

            PoseStack.Pose pose = poseStack.last();
            for (BlockStateModelPart part : parts) {
                for (Direction side : Direction.values()) {
                    pigeonplus$emitQuads(buffer, pose, part.getQuads(side), instance);
                }
                pigeonplus$emitQuads(buffer, pose, part.getQuads(null), instance);
            }
            poseStack.popPose();
        }
    }

    private static void pigeonplus$emitQuads(
        VertexConsumer buffer,
        PoseStack.Pose pose,
        List<BakedQuad> quads,
        QuadInstance instance
    ) {
        for (BakedQuad quad : quads) {
            buffer.putBakedQuad(pose, quad, instance);
        }
    }

    /** 取出锁链方块的模型部件（整批绘制只调用一次）。 */
    private static List<BlockStateModelPart> pigeonplus$chainParts() {
        BlockStateModelSet modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        BlockStateModel model = modelSet.get(CHAIN_STATE);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0L), parts);
        return parts;
    }

    private static int chainAlpha(float progress) {
        if (progress <= CHAIN_FADE_START) {
            return CHAIN_ALPHA;
        }
        float fade = (progress - CHAIN_FADE_START) / (1.0f - CHAIN_FADE_START);
        return (int) (CHAIN_ALPHA + (CHAIN_END_ALPHA - CHAIN_ALPHA) * Math.min(fade, 1.0f));
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
    public boolean shouldRenderOffScreen() {
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

    /** 渲染状态。 */
    public static class State extends BlockEntityRenderState {
        boolean lit;
        float beamHeight;
    }
}
