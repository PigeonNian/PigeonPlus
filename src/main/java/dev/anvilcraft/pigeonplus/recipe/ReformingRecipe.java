package dev.anvilcraft.pigeonplus.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.pigeonplus.init.AddonRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/**
 * 蒸汽重整配方：多种流体在大型炼药锅里被催化重整成另一种流体。
 *
 * <p>与发酵的单流体进出不同，重整天然是「甲烷 + 水 → 氢气」这种多输入反应，
 * 因此输入是一张流体表，每项都自带单批次消耗量。
 */
public record ReformingRecipe(List<FluidStack> inputs, FluidStack output)
    implements Recipe<ReformingRecipe.Input> {
    public static final MapCodec<ReformingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        FluidStack.CODEC.listOf().fieldOf("inputs").forGetter(ReformingRecipe::inputs),
        FluidStack.CODEC.fieldOf("output").forGetter(ReformingRecipe::output)
    ).apply(instance, ReformingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReformingRecipe> STREAM_CODEC = StreamCodec.composite(
        FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
        ReformingRecipe::inputs,
        FluidStack.STREAM_CODEC,
        ReformingRecipe::output,
        ReformingRecipe::new
    );

    public ReformingRecipe {
        inputs = inputs.stream()
            .filter(stack -> !stack.isEmpty())
            .map(FluidStack::copy)
            .toList();
        output = output.copy();
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("Reforming recipe needs at least one fluid input");
        }
    }

    @Override
    public boolean matches(Input input, net.minecraft.world.level.Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AddonRecipeTypes.REFORMING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return AddonRecipeTypes.REFORMING_TYPE.get();
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

    public static class Serializer implements RecipeSerializer<ReformingRecipe> {
        @Override
        public MapCodec<ReformingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ReformingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
