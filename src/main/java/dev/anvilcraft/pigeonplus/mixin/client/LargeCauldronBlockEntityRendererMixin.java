package dev.anvilcraft.pigeonplus.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.NozzleBlock;
import dev.anvilcraft.pigeonplus.util.NozzleExhaustUtil;
import dev.anvilcraft.pigeonplus.client.renderer.GasContainerRenderUtil;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LargeCauldronBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(LargeCauldronBlockEntityRenderer.class)
    public class LargeCauldronBlockEntityRendererMixin {
    @Unique
    private static final ModelResourceLocation PIGEONPLUS_LARGE_CAULDRON_TOP = new ModelResourceLocation(
        ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/large_cauldron_top"),
        "standalone"
    );
    @Unique
    private static final ModelResourceLocation PIGEONPLUS_LARGE_CAULDRON_BOTTOM = new ModelResourceLocation(
        ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/large_cauldron_bottom"),
        "standalone"
    );
    @Unique
    private static final float PIGEONPLUS_WALL = 0.25F + 0.001F;
    @Unique
    private static final float PIGEONPLUS_MIN_XZ = -1.0F + PIGEONPLUS_WALL;
    @Unique
    private static final float PIGEONPLUS_MAX_XZ = 2.0F - PIGEONPLUS_WALL;
    @Unique
    private static final float PIGEONPLUS_MIN_Y = -0.5F + 0.001F;
    @Unique
    private static final float PIGEONPLUS_MAX_Y = 1.75F - 0.001F;

    @Inject(method = "render", at = @At("TAIL"))
    private void pigeonplus$renderNozzleCauldronAttachment(
        LargeCauldronBlockEntity cauldron,
        float partialTick,
        PoseStack pose,
        MultiBufferSource buffers,
        int light,
        int overlay,
        CallbackInfo ci
    ) {
        Level level = cauldron.getLevel();
        if (level == null || !cauldron.isMainPart()) {
            return;
        }

        if (pigeonplus$hasHorizontalNozzle(level, cauldron.getBlockPos())) {
            pigeonplus$renderStandaloneModel(PIGEONPLUS_LARGE_CAULDRON_TOP, cauldron, pose, buffers, light, overlay);
        }
        if (pigeonplus$hasBottomNozzle(level, cauldron.getBlockPos())) {
            pigeonplus$renderStandaloneModel(PIGEONPLUS_LARGE_CAULDRON_BOTTOM, cauldron, pose, buffers, light, overlay);
        }
    }

    @Unique
    private static void pigeonplus$renderStandaloneModel(
        ModelResourceLocation modelLocation,
        LargeCauldronBlockEntity cauldron,
        PoseStack pose,
        MultiBufferSource buffers,
        int light,
        int overlay
    ) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelLocation);
        BlockState state = cauldron.getBlockState();
        RandomSource random = RandomSource.create();
        ModelData modelData = ModelData.EMPTY;
        for (RenderType renderType : model.getRenderTypes(state, random, modelData)) {
            VertexConsumer consumer = buffers.getBuffer(renderType);
            dispatcher.getModelRenderer().renderModel(
                pose.last(),
                consumer,
                state,
                model,
                1.0F,
                1.0F,
                1.0F,
                light,
                overlay,
                modelData,
                renderType
            );
        }
    }

    @Unique
    private static boolean pigeonplus$hasBottomNozzle(Level level, BlockPos cauldronPos) {
        BlockPos nozzlePos = cauldronPos.below(NozzleExhaustUtil.NOZZLE_MAIN_OFFSET_Y);
        BlockState state = level.getBlockState(nozzlePos);
        return state.getBlock() instanceof NozzleBlock nozzle
            && nozzle.isMainPart(state)
            && state.getValue(NozzleBlock.FACING) == Direction.DOWN;
    }

    @Unique
    private static boolean pigeonplus$hasHorizontalNozzle(Level level, BlockPos cauldronPos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos nozzlePos = cauldronPos.relative(direction, NozzleExhaustUtil.NOZZLE_MAIN_OFFSET_Y);
            BlockState state = level.getBlockState(nozzlePos);
            if (state.getBlock() instanceof NozzleBlock nozzle
                && nozzle.isMainPart(state)
                && state.getValue(NozzleBlock.FACING) == direction) {
                return true;
            }
        }
        return false;
    }

    @Inject(method = "drawFluids", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$renderGasAboveLiquid(
        LargeCauldronFluidHandler handler,
        PoseStack pose,
        MultiBufferSource buffers,
        int light,
        CallbackInfo ci
    ) {
        List<FluidStack> layers = new ArrayList<>();
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluid = handler.getFluidInTank(tank);
            if (!fluid.isEmpty()) {
                layers.add(fluid);
            }
        }
        if (!GasContainerRenderUtil.hasGas(layers)) {
            return;
        }
        GasContainerRenderUtil.renderLayeredFluidBox(
            layers,
            (double) LargeCauldronFluidHandler.TANK_COUNT * LargeCauldronFluidHandler.TANK_CAPACITY,
            PIGEONPLUS_MIN_XZ,
            PIGEONPLUS_MIN_Y,
            PIGEONPLUS_MIN_XZ,
            PIGEONPLUS_MAX_XZ,
            PIGEONPLUS_MAX_Y,
            PIGEONPLUS_MAX_XZ,
            buffers,
            pose,
            light
        );
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
        ci.cancel();
    }

    @Shadow
    private void drawFire(PoseStack pose, MultiBufferSource buffers, int overlay, float surfaceY) {
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Ldev/dubhe/anvilcraft/client/renderer/blockentity/LargeCauldronBlockEntityRenderer;drawFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V"
        )
    )
    private void pigeonplus$hideIgnitedFireTexture(
        LargeCauldronBlockEntityRenderer instance,
        PoseStack pose,
        MultiBufferSource buffers,
        int overlay,
        float surfaceY,
        LargeCauldronBlockEntity cauldron,
        float partialTick,
        PoseStack renderPose,
        MultiBufferSource renderBuffers,
        int light,
        int renderOverlay
    ) {
        if (NozzleExhaustUtil.hasAnyPropellant(cauldron)) {
            return;
        }
        this.drawFire(pose, buffers, overlay, surfaceY);
    }
}
