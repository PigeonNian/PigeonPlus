package dev.anvilcraft.pigeonplus.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.pigeonplus.init.AddonRecipeTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 气体液化配方：把一种流体按比例转成另一种。
 *
 * <p>本配方不参与原版合成台逻辑（没有物品形态的输入输出），
 * 而是由气体液化装置直接读取数据。26.1 的 {@link Recipe} 接口新增了
 * {@code placementInfo} 与 {@code recipeBookCategory}，
 * 并移除了 {@code assemble(input, registries)} 的 registries 参数、
 * {@code canCraftInDimensions} 与 {@code getResultItem}。
 */
public record GasLiquefactionRecipe(FluidStack input, FluidStack output) implements Recipe<GasLiquefactionRecipe.Input> {
    public static final MapCodec<GasLiquefactionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        FluidStack.CODEC.fieldOf("input").forGetter(GasLiquefactionRecipe::input),
        FluidStack.CODEC.fieldOf("output").forGetter(GasLiquefactionRecipe::output)
    ).apply(instance, GasLiquefactionRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GasLiquefactionRecipe> STREAM_CODEC = StreamCodec.composite(
        FluidStack.STREAM_CODEC,
        GasLiquefactionRecipe::input,
        FluidStack.STREAM_CODEC,
        GasLiquefactionRecipe::output,
        GasLiquefactionRecipe::new
    );

    /** 序列化器（26.1 起 RecipeSerializer 是 record，直接构造即可）。 */
    public static final RecipeSerializer<GasLiquefactionRecipe> SERIALIZER =
        new RecipeSerializer<>(CODEC, STREAM_CODEC);

    public int ratio() {
        return this.output.getAmount() > 0 ? this.input.getAmount() / this.output.getAmount() : 0;
    }

    @Override
    public boolean matches(Input input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Input input) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "gas_liquefaction";
    }

    /** 不通过合成台摆放，故标记为不可放置。 */
    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<GasLiquefactionRecipe> getSerializer() {
        return AddonRecipeTypes.GAS_LIQUEFACTION_SERIALIZER.get();
    }

    @Override
    public RecipeType<GasLiquefactionRecipe> getType() {
        return AddonRecipeTypes.GAS_LIQUEFACTION_TYPE.get();
    }

    public record Input() implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }
    }
}
