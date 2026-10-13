package dev.anvilcraft.pigeonplus.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.pigeonplus.init.AddonRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;

public record FermentationRecipe(FluidStack input, FluidStack output) implements Recipe<FermentationRecipe.Input> {
    public static final MapCodec<FermentationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        FluidStack.CODEC.fieldOf("input").forGetter(FermentationRecipe::input),
        FluidStack.CODEC.fieldOf("output").forGetter(FermentationRecipe::output)
    ).apply(instance, FermentationRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FermentationRecipe> STREAM_CODEC = StreamCodec.composite(
        FluidStack.STREAM_CODEC,
        FermentationRecipe::input,
        FluidStack.STREAM_CODEC,
        FermentationRecipe::output,
        FermentationRecipe::new
    );

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
        return AddonRecipeTypes.FERMENTATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return AddonRecipeTypes.FERMENTATION_TYPE.get();
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

    public static class Serializer implements RecipeSerializer<FermentationRecipe> {
        @Override
        public MapCodec<FermentationRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FermentationRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
