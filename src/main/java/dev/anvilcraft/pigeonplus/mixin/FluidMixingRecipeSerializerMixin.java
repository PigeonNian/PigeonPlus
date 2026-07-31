package dev.anvilcraft.pigeonplus.mixin;

import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(FluidMixingRecipe.Serializer.class)
public class FluidMixingRecipeSerializerMixin {
    @ModifyConstant(method = "lambda$static$0", constant = @Constant(intValue = 2), require = 1)
    private static int pigeonplus$allowSingleFluidIngredient(int minIngredients) {
        return 1;
    }
}
