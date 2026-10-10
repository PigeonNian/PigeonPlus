package dev.anvilcraft.pigeonplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelBlockSelection.class)
public class ModelBlockSelectionMixin {
    @ModifyExpressionValue(
        method = "highlight(Lnet/neoforged/neoforge/client/event/ExtractBlockOutlineRenderStateEvent;Lnet/minecraft/world/level/block/state/BlockState;)Z",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"
        )
    )
    private static boolean pigeonplus$widenHighlightToOwnMultipart(
        boolean original,
        @Local(ordinal = 0) Block block
    ) {
        if (original) {
            return true;
        }
        return block instanceof AbstractMultiPartBlock<?>
               && AnvilCraftPigeonPlus.MOD_ID.equals(
                   BuiltInRegistries.BLOCK.getKey(block).getNamespace()
               );
    }
}
