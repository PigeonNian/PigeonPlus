package dev.anvilcraft.pigeonplus.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.pigeonplus.block.NozzleBlock;
import dev.anvilcraft.pigeonplus.client.renderer.block.LargeCauldronAttachmentModels;
import dev.anvilcraft.pigeonplus.client.renderer.block.StandaloneBlockModels;
import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import dev.anvilcraft.pigeonplus.util.NozzleExhaustUtil;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LargeCauldronBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.LargeCauldronRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 大炼药锅的两项本模组扩展：
 * 装了喷口后加装顶/底盖板，以及气体铺满锅体、点燃火焰被遮蔽。
 *
 * <h3>26.1 改写</h3>
 * AnvilCraft 的渲染器已改为三段式，且把「画什么」拆成了
 * {@code extractRenderState}（准备数据）与 {@code submit}（提交绘制）。
 * 原先的 {@code drawFluids}/{@code drawFire} 都不存在了，故按新结构重写：
 * <ul>
 *   <li><b>气体铺满</b>：不再自己画流体盒。AnvilCraft 的
 *       {@code submitFluids} 用 {@code amount / TOTAL_CAPACITY} 算液面高度，
 *       所以只要在 {@code extractRenderState} 里把气体那一层的
 *       {@code amount} 抬到总容量，AnvilCraft 自己就会画出满锅气体——
 *       箱壁、坐标、渲染层全部沿用它的实现，我们不碰几何体。</li>
 *   <li><b>遮蔽火焰</b>：火焰现在是渲染状态里的 {@code fire} 字段，
 *       直接在 {@code extractRenderState} 末尾清空即可，
 *       比原先 Redirect {@code drawFire} 更干净。</li>
 *   <li><b>盖板</b>：仍是独立模型，在 {@code submit} 末尾追加绘制。</li>
 * </ul>
 */
@Mixin(LargeCauldronBlockEntityRenderer.class)
public class LargeCauldronBlockEntityRendererMixin {

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
    /**
     * 提取阶段：气体抬满 + 有推进剂时熄火。
     */
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void pigeonplus$adjustLargeCauldronState(
        LargeCauldronBlockEntity cauldron,
        LargeCauldronRenderState state,
        float partialTick,
        Vec3 cameraPosition,
        ModelFeatureRenderer.CrumblingOverlay breakProgress,
        CallbackInfo ci
    ) {
        // 有推进剂时把火焰状态清空，等价于原先「隐藏点燃火焰贴图」
        if (NozzleExhaustUtil.hasAnyPropellant(cauldron)) {
            state.setFire(null);
        }

        List<LargeCauldronRenderState.FluidLayerRenderState> fluids = state.getFluids();
        if (fluids.isEmpty()) {
            return;
        }
        boolean hasGas = false;
        List<LargeCauldronRenderState.FluidLayerRenderState> adjusted = new ArrayList<>(fluids.size());
        for (LargeCauldronRenderState.FluidLayerRenderState layer : fluids) {
            if (layer.resource().getFluid() instanceof GasFluid) {
                hasGas = true;
                // 抬到满容量：AnvilCraft 的 submitFluids 会据此画出满层
                adjusted.add(new LargeCauldronRenderState.FluidLayerRenderState(
                    layer.resource(), LargeCauldronFluidHandler.TOTAL_CAPACITY));
            } else {
                adjusted.add(layer);
            }
        }
        if (hasGas) {
            fluids.clear();
            fluids.addAll(adjusted);
        }
    }

    /**
     * 提交阶段：追加喷口盖板模型。
     */
    @Inject(method = "submit", at = @At("TAIL"))
    private void pigeonplus$submitNozzleCauldronAttachment(
        LargeCauldronRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        CameraRenderState camera,
        CallbackInfo ci
    ) {
        Level level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null || state.blockPos == null) {
            return;
        }
        BlockPos cauldronPos = state.blockPos;
        if (pigeonplus$hasHorizontalNozzle(level, cauldronPos)) {
            StandaloneBlockModels.submit(
                poseStack, collector, LargeCauldronAttachmentModels.TOP, state.lightCoords, OverlayTexture.NO_OVERLAY);
        }
        if (pigeonplus$hasBottomNozzle(level, cauldronPos)) {
            StandaloneBlockModels.submit(
                poseStack, collector, LargeCauldronAttachmentModels.BOTTOM, state.lightCoords, OverlayTexture.NO_OVERLAY);
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
}
