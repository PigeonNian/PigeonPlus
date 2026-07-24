package dev.anvilcraft.pigeonplus.mixin.client;

import dev.anvilcraft.pigeonplus.client.renderer.block.StasisBeaconBlockEntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityClientMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$stasisEntityWhiteOutline(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity) (Object) this;
        if (StasisBeaconBlockEntityRenderer.hasStasisEffect(entity)) {
            cir.setReturnValue(0xFFFFFF);
        }
    }
}
