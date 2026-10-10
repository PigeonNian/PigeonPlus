package dev.anvilcraft.pigeonplus.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.LargeFireworksRocketBlock;
import dev.anvilcraft.pigeonplus.block.state.LargeFireworksRocketPart;
import dev.anvilcraft.pigeonplus.entity.LargeFireworksRocketEntity;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.client.model.standalone.UnbakedStandaloneModel;

import java.util.List;

public class LargeFireworksRocketRenderer extends EntityRenderer<LargeFireworksRocketEntity, LargeFireworksRocketRenderState> {
    private static final Identifier LIT_BOTTOM_ID = Identifier.fromNamespaceAndPath(
        AnvilCraftPigeonPlus.MOD_ID, "block/lit_large_fireworks_rocket_bottom");

    public static final StandaloneModelKey<BlockStateModel> LIT_BOTTOM =
        new StandaloneModelKey<>((ModelDebugName) () -> "block/lit_large_fireworks_rocket_bottom");

    public static void registerModels(net.neoforged.neoforge.client.event.ModelEvent.RegisterStandalone event) {
        event.register(LIT_BOTTOM, SimpleUnbakedStandaloneModel.blockStateModel(LIT_BOTTOM_ID));
    }

    private static final double PEARL_HEIGHT = LargeFireworksRocketEntity.PEARL_RENDER_HEIGHT;
    private static final float PEARL_SCALE = LargeFireworksRocketEntity.PEARL_RENDER_SCALE;

    private static final int[] NO_TINTS = BlockModelRenderState.EMPTY_TINTS;
    private static final List<BlockStateModelPart> PARTS = new ObjectArrayList<>();

    private static final Segment[] SEGMENTS = {
        new Segment(LargeFireworksRocketPart.Y1_C, 1, true),
        new Segment(LargeFireworksRocketPart.Y4_C, 4, false),
        new Segment(LargeFireworksRocketPart.Y7_C, 7, false),
    };

    private final ItemModelResolver itemModelResolver;

    public LargeFireworksRocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.shadowRadius = 0.0F;
    }

    @Override
    public LargeFireworksRocketRenderState createRenderState() {
        return new LargeFireworksRocketRenderState();
    }

    @Override
    public void extractRenderState(
        LargeFireworksRocketEntity entity,
        LargeFireworksRocketRenderState state,
        float partialTick
    ) {
        super.extractRenderState(entity, state, partialTick);
        state.facing = entity.getFacing();
        state.detonating = entity.isDetonating();
        state.pearls = entity.getPearls();
        this.itemModelResolver.updateForNonLiving(
            state.pearlStack, new ItemStack(Items.ENDER_PEARL), ItemDisplayContext.GROUND, entity);
    }

    @Override
    public void submit(
        LargeFireworksRocketRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        if (state.detonating) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        BlockState base = AddonBlocks.LARGE_FIREWORKS_ROCKET.get().defaultBlockState();
        RandomSource random = RandomSource.create();
        BlockPos entityPos = BlockPos.containing(state.x, state.y, state.z);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yawFor(state.facing)));
        poseStack.translate(-0.5, 0.0, -0.5);
        for (Segment segment : SEGMENTS) {
            BlockState blockState = base
                .setValue(LargeFireworksRocketBlock.PART, segment.part())
                .setValue(LargeFireworksRocketBlock.FACING, state.facing);
            BlockStateModel model = segment.lit()
                ? minecraft.getModelManager().getStandaloneModel(LIT_BOTTOM)
                : minecraft.getModelManager().getBlockStateModelSet().get(blockState);
            if (model == null) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(0.0, segment.offsetY(), 0.0);
            PARTS.clear();
            model.collectParts(random, PARTS);
            if (!PARTS.isEmpty()) {
                collector.submitBlockModel(
                    poseStack,
                    Sheets.cutoutBlockSheet(),
                    new ObjectArrayList<>(PARTS),
                    NO_TINTS,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0
                );
            }
            poseStack.popPose();
        }
        poseStack.popPose();

        if (state.pearls > 0) {
            poseStack.pushPose();
            poseStack.translate(0.0, PEARL_HEIGHT, 0.0);
            poseStack.scale(PEARL_SCALE, PEARL_SCALE, PEARL_SCALE);
            state.pearlStack.submit(
                poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }

        super.submit(state, poseStack, collector, camera);
    }

    @Override
    protected AABB getBoundingBoxForCulling(LargeFireworksRocketEntity entity) {
        return entity.getBoundingBox().expandTowards(0.0, PEARL_HEIGHT + 1.0 - entity.getBbHeight(), 0.0);
    }

    private static float yawFor(Direction facing) {
        return switch (facing) {
            case EAST -> 270.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    private record Segment(LargeFireworksRocketPart part, int offsetY, boolean lit) {
    }
}
