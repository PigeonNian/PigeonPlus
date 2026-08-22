package dev.anvilcraft.pigeonplus.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
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

    public int ratio() {
        return this.output.getAmount() > 0 ? this.input.getAmount() / this.output.getAmount() : 0;
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
        return dev.anvilcraft.pigeonplus.init.AddonRecipeTypes.GAS_LIQUEFACTION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return dev.anvilcraft.pigeonplus.init.AddonRecipeTypes.GAS_LIQUEFACTION_TYPE.get();
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

    public static class Serializer implements RecipeSerializer<GasLiquefactionRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        public Serializer() {
        }

        @Override
        public MapCodec<GasLiquefactionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GasLiquefactionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
