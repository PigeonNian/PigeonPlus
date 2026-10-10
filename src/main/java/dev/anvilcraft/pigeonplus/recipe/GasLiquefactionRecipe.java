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
import net.neoforged.neoforge.fluids.FluidStackTemplate;

public record GasLiquefactionRecipe(FluidStackTemplate inputTemplate, FluidStackTemplate outputTemplate)
    implements Recipe<GasLiquefactionRecipe.Input> {

    public static final MapCodec<GasLiquefactionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        FluidStackTemplate.CODEC.fieldOf("input").forGetter(GasLiquefactionRecipe::inputTemplate),
        FluidStackTemplate.CODEC.fieldOf("output").forGetter(GasLiquefactionRecipe::outputTemplate)
    ).apply(instance, GasLiquefactionRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GasLiquefactionRecipe> STREAM_CODEC = StreamCodec.composite(
        FluidStackTemplate.STREAM_CODEC,
        GasLiquefactionRecipe::inputTemplate,
        FluidStackTemplate.STREAM_CODEC,
        GasLiquefactionRecipe::outputTemplate,
        GasLiquefactionRecipe::new
    );

    /** 序列化器（26.1 起 RecipeSerializer 是 record，直接构造即可）。 */
    public static final RecipeSerializer<GasLiquefactionRecipe> SERIALIZER =
        new RecipeSerializer<>(CODEC, STREAM_CODEC);

    public FluidStack input() {
        return this.inputTemplate.create();
    }

    public FluidStack output() {
        return this.outputTemplate.create();
    }

    public int ratio() {
        return this.outputTemplate.amount() > 0 ? this.inputTemplate.amount() / this.outputTemplate.amount() : 0;
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
