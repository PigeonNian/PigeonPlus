package dev.anvilcraft.pigeonplus.mixin.client;

import dev.anvilcraft.pigeonplus.client.renderer.block.StasisBeaconBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$stasisEntityAppearsGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (StasisBeaconBlockEntityRenderer.hasStasisEffect(entity)) {
            cir.setReturnValue(true);
        }
    }
}
