package dev.anvilcraft.pigeonplus.client.selection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.anvilcraft.lib.v2.cube.client.OutlineRenderer;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.anvilcraft.lib.v2.cube.client.model.ModelSelection;
import dev.anvilcraft.lib.v2.cube.geometry.PackedOutline;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import dev.dubhe.anvilcraft.mixin.accessor.ModelBakeryAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID, value = Dist.CLIENT)
public final class AddonModelBlockSelection {
    private static Snapshot snapshot = new Snapshot(Map.of(), Map.of(), Map.of());

    private AddonModelBlockSelection() {
    }

    public static void reload(ModelEvent.BakingCompleted event) {
        ModelBakeryAccessor bakery = (ModelBakeryAccessor) event.getModelBakery();
        snapshot = new AddonModelSelectionBakery(bakery.getUnbakedCache()).bake(bakery.getTopLevelModels());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void highlight(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.options.hideGui) {
            return;
        }
        BlockPos pos = event.getTarget().getBlockPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!AnvilCraftPigeonPlus.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) {
            return;
        }
        if (AddonModelSelectionDenylist.usesOriginalOutline(block)) {
            return;
        }
        float partialTick = frameFraction(event, level);
        boolean multipart = block instanceof AbstractMultiPartBlock<?>;
        List<SelectionPart> outline = snapshot.outlines().get(state);
        List<SelectionPart> extra = rendererParts(level, pos, state, partialTick);
        // 已被 anvillib 画过方块模型描边时，这里只补它画不到的渲染器部件，
        // 避免同一圈描边叠两次。
        boolean drawnByLib = event.isCanceled();
        if (drawnByLib && extra.isEmpty()) {
            return;
        }
        if (!drawnByLib && extra.isEmpty()
            && CubeSelection.target(level, pos, state, partialTick) != null) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        Vec3 offset = multipart ? Vec3.ZERO : state.getOffset(level, pos);
        // 方块局部坐标系下的相机位置：下面 translate 之后，几何与相机就处在同一个坐标系里，
        // PackedOutline#forView 需要的就是这个值。
        Vec3 localCamera = new Vec3(
            camera.x - pos.getX() - offset.x,
            camera.y - pos.getY() - offset.y,
            camera.z - pos.getZ() - offset.z
        );
        pose.pushPose();
        pose.translate(
            pos.getX() - camera.x + offset.x,
            pos.getY() - camera.y + offset.y,
            pos.getZ() - camera.z + offset.z
        );
        if (!drawnByLib && outline != null && !outline.isEmpty()) {
            drawOutline(outline, localCamera, pose, event);
        }
        if (!extra.isEmpty()) {
            drawOutline(extra, localCamera, pose, event);
        }
        pose.popPose();
        event.setCanceled(true);
    }

    private static List<SelectionPart> rendererParts(
        ClientLevel level,
        BlockPos pos,
        BlockState state,
        float partialTick
    ) {
        if (state.getBlock() instanceof AbstractMultiPartBlock<?>) {
            return List.of();
        }
        if (AddonModelSelectionDenylist.excludesBlockEntity(state.getBlock())) {
            return List.of();
        }
        BlockEntity entity = level.getBlockEntity(pos);
        return entity == null ? List.of() : ModelBlockSelection.rendererParts(entity, partialTick);
    }

    private static float frameFraction(RenderHighlightEvent.Block event, ClientLevel level) {
        return event.getDeltaTracker().getGameTimeDeltaPartialTick(
            !level.tickRateManager().isEntityFrozen(event.getCamera().getEntity())
        );
    }

    private static void drawOutline(
        List<SelectionPart> parts,
        Vec3 localCamera,
        PoseStack pose,
        RenderHighlightEvent.Block event
    ) {
        PackedOutline[] outlines = new PackedOutline[parts.size()];
        PackedOutline[] visible = new PackedOutline[parts.size()];
        int segments = 0;
        int visibleSegments = 0;
        for (int i = 0; i < parts.size(); i++) {
            SelectionPart part = parts.get(i);
            outlines[i] = CubeSelection.outlines().get(part.geometry());
            segments += outlines[i].segmentCount();
            // anvillib 的接缝隐藏是按视角算的：相机必须换到该部件的局部坐标系，
            // 与 CubeHighlightEvents 里的做法一致，否则这里画出来的是未优化的原框线。
            visible[i] = outlines[i].forView(part.localPosition(localCamera));
            visibleSegments += visible[i].segmentCount();
        }
        if (segments > SelectionGeometry.MAX_OUTLINE_SEGMENTS) {
            AABB bounds = parts.getFirst().bounds();
            for (SelectionPart part : parts) {
                bounds = bounds.minmax(part.bounds());
            }
            LevelRenderer.renderLineBox(
                pose,
                event.getMultiBufferSource().getBuffer(RenderType.lines()),
                bounds,
                0, 0, 0, 0.4F
            );
            return;
        }
        if (visibleSegments > SelectionGeometry.MAX_OUTLINE_SEGMENTS) {
            System.arraycopy(outlines, 0, visible, 0, outlines.length);
        }
        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.lines());
        for (int i = 0; i < parts.size(); i++) {
            pose.pushPose();
            parts.get(i).apply(pose);
            OutlineRenderer.render(pose, consumer, visible[i], 0, 0, 0, 0.4F);
            pose.popPose();
        }
    }

    record Snapshot(
        Map<BlockState, ModelSelection> states,
        Map<BlockState, List<SelectionPart>> outlines,
        Map<ModelResourceLocation, SelectionPart> standalone
    ) {
    }
}
