package dev.anvilcraft.pigeonplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 允许流体混合配方只写 1 种流体。
 *
 * <p>AnvilCraft 的 {@code FluidMixingRecipe} 默认要求至少 2 种流体
 * （{@code validateSize(ingredients, 2, MAX, ...)}）。本模组的
 * 「混合生物质 → 气态生物质」只需单一流体，因此把该下界改成 1。
 *
 * <h3>26.1 的注意点</h3>
 * AnvilCraft 把内部的 {@code Serializer} 改成了 <b>private</b> 静态嵌套类，
 * 于是不能再写 {@code @Mixin(FluidMixingRecipe.Serializer.class)}——
 * 那会因跨包访问私有类而编译失败。
 * 改用 {@code @Mixin(targets = "...$Serializer")} 以字符串指定目标，
 * 绕开编译期的访问检查（Mixin 本身在字节码层面工作，不受 private 限制）。
 *
 * <p>被改的常量位于 {@code lambda$static$0}：它是 {@code Serializer} 里
 * 第一个静态 lambda，即 {@code INGREDIENTS_CODEC} 的校验器
 * （按声明顺序，{@code $0}=配料、{@code $1}=物品产物、{@code $2}=流体产物），
 * 其中的 {@code 2} 正是配料数量下界。
 */
@Mixin(targets = "dev.dubhe.anvilcraft.recipe.FluidMixingRecipe$Serializer")
public class FluidMixingRecipeSerializerMixin {
    @ModifyConstant(method = "lambda$static$0", constant = @Constant(intValue = 2), require = 1)
    private static int pigeonplus$allowSingleFluidIngredient(int minIngredients) {
        return 1;
    }
}
