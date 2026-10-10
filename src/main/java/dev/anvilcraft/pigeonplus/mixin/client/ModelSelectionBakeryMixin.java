package dev.anvilcraft.pigeonplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "dev.dubhe.anvilcraft.client.selection.ModelSelectionBakery")
public class ModelSelectionBakeryMixin {
    @ModifyExpressionValue(
        method = "bake",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z",
            ordinal = 0
        )
    )
    private boolean pigeonplus$widenFilterToOwnMultipart(boolean original, @Local(ordinal = 0) Block block) {
        if (original) {
            return true;
        }
        return block instanceof AbstractMultiPartBlock<?>
               && AnvilCraftPigeonPlus.MOD_ID.equals(
                   BuiltInRegistries.BLOCK.getKey(block).getNamespace()
               );
    }
}
