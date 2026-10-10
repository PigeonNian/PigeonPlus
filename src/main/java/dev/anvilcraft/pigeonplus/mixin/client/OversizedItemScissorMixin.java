package dev.anvilcraft.pigeonplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiGraphicsExtractor.class)
public abstract class OversizedItemScissorMixin {
    @WrapOperation(
        method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/item/ItemStack;III)V",
        at = @At(value = "NEW", target = "net/minecraft/client/renderer/state/gui/GuiItemRenderState")
    )
    private GuiItemRenderState pigeonplus$unclipOversizedIcon(
        Matrix3x2f pose,
        TrackingItemStackRenderState state,
        int x,
        int y,
        @Nullable ScreenRectangle scissor,
        Operation<GuiItemRenderState> original,
        @Local(argsOnly = true) ItemStack stack
    ) {
        if (scissor == null || !pigeonplus$isOversizedInGui(stack)) {
            return original.call(pose, state, x, y, scissor);
        }
        return original.call(pose, state, x, y, (ScreenRectangle) null);
    }

    private static boolean pigeonplus$isOversizedInGui(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!AnvilCraftPigeonPlus.MOD_ID.equals(
            BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) {
            return false;
        }
        Identifier model = stack.get(DataComponents.ITEM_MODEL);
        if (model == null) {
            return false;
        }
        return Minecraft.getInstance()
            .getModelManager()
            .getItemProperties(model)
            .oversizedInGui();
    }
}