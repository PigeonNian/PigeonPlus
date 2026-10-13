package dev.anvilcraft.pigeonplus.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.pigeonplus.client.renderer.GasTankRenderContext;
import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import dev.anvilcraft.pigeonplus.util.BlenderStirring;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FishTankBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(FishTankBlockEntityRenderer.class)
public class FishTankBlockEntityRendererMixin {
    @Unique
    private static final float TANK_W = 1 / 16F + 0.001F;
    @Unique
    private static final ThreadLocal<Boolean> PIGEONPLUS_RENDERING_GAS = ThreadLocal.withInitial(() -> false);
    @Unique
    private static final ThreadLocal<Float> PIGEONPLUS_ITEM_ROTATION = ThreadLocal.withInitial(() -> 0.0F);
    @Unique
    private static final ThreadLocal<Boolean> PIGEONPLUS_ITEM_ROTATED = ThreadLocal.withInitial(() -> false);

    @Inject(method = "render", at = @At("HEAD"))
    private void pigeonplus$beginRenderGasItemPlacement(
        FishTankBlockEntity tank,
        float partialTick,
        PoseStack pose,
        MultiBufferSource source,
        int light,
        int overlay,
        CallbackInfo ci
    ) {
        PIGEONPLUS_RENDERING_GAS.set(tank.getFluidHandler().getFluid().getFluid() instanceof GasFluid);
        PIGEONPLUS_ITEM_ROTATION.set(pigeonplus$itemRotation(tank, partialTick));
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void pigeonplus$endRenderGasItemPlacement(
        FishTankBlockEntity tank,
        float partialTick,
        PoseStack pose,
        MultiBufferSource source,
        int light,
        int overlay,
        CallbackInfo ci
    ) {
        PIGEONPLUS_RENDERING_GAS.remove();
        PIGEONPLUS_ITEM_ROTATION.remove();
    }

    @Unique
    private static float pigeonplus$itemRotation(FishTankBlockEntity tank, float partialTick) {
        Level level = tank.getLevel();
        if (level == null || !BlenderStirring.isStirring(level, tank.getBlockPos().below())) {
            return 0.0F;
        }
        return BlenderStirring.angle(level, partialTick);
    }

    @Inject(method = "drawItemsInTank", at = @At("HEAD"))
    private static void pigeonplus$beginRotateItemsInTank(
        Level level,
        List<ItemStack> items,
        float fill,
        ItemRenderer renderer,
        PoseStack pose,
        MultiBufferSource source,
        RandomSource random,
        int light,
        int overlay,
        CallbackInfo ci
    ) {
        float angle = PIGEONPLUS_ITEM_ROTATION.get();
        if (items.isEmpty() || angle == 0.0F) {
            PIGEONPLUS_ITEM_ROTATED.set(false);
            return;
        }
        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-0.5F, 0.0F, -0.5F);
        PIGEONPLUS_ITEM_ROTATED.set(true);
    }

    @Inject(method = "drawItemsInTank", at = @At("RETURN"))
    private static void pigeonplus$endRotateItemsInTank(
        Level level,
        List<ItemStack> items,
        float fill,
        ItemRenderer renderer,
        PoseStack pose,
        MultiBufferSource source,
        RandomSource random,
        int light,
        int overlay,
        CallbackInfo ci
    ) {
        if (PIGEONPLUS_ITEM_ROTATED.get()) {
            pose.popPose();
            PIGEONPLUS_ITEM_ROTATED.set(false);
        }
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Ldev/dubhe/anvilcraft/client/renderer/blockentity/FishTankBlockEntityRenderer;drawItemsInTank(Lnet/minecraft/world/level/Level;Ljava/util/List;FLnet/minecraft/client/renderer/entity/ItemRenderer;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/util/RandomSource;II)V"
        ),
        index = 2
    )
    private float pigeonplus$keepItemsAtBottomInGas(float fill) {
        return PIGEONPLUS_RENDERING_GAS.get() ? 0.0F : fill;
    }

    @Inject(method = "drawFluidInTank", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$renderGasWithAlpha(
        PoseStack pose,
        MultiBufferSource source,
        int light,
        FluidTank fluid,
        float minY,
        float maxY,
        CallbackInfo ci
    ) {
        FluidStack stack = fluid.getFluid();
        if (!(stack.getFluid() instanceof GasFluid)) {
            return;
        }
        float fill = fluid.getCapacity() <= 0
            ? 0.0F
            : Math.min((float) fluid.getFluidAmount() / fluid.getCapacity(), 1.0F);
        GasTankRenderContext.renderWithFill(fill, () -> FluidRenderHelper.INSTANCE.renderFluidBox(
            stack,
            TANK_W,
            TANK_W,
            TANK_W,
            1 - TANK_W,
            1 - TANK_W,
            1 - TANK_W,
            source,
            pose,
            light,
            true,
            false
        ));
        if (source instanceof MultiBufferSource.BufferSource buffer) {
            buffer.endBatch();
        }
        ci.cancel();
    }
}
