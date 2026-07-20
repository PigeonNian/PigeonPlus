package dev.anvilcraft.pigeonplus.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.pigeonplus.client.renderer.GasTankRenderContext;
import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FishTankBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishTankBlockEntityRenderer.class)
public class FishTankBlockEntityRendererMixin {
    @Unique
    private static final float TANK_W = 1 / 16F + 0.001F;
    @Unique
    private static final ThreadLocal<Boolean> PIGEONPLUS_RENDERING_GAS = ThreadLocal.withInitial(() -> false);

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
