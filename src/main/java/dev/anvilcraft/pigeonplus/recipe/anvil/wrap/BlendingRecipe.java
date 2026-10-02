package dev.anvilcraft.pigeonplus.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BlendingRecipe extends AbstractProcessRecipe<BlendingRecipe> {
    public BlendingRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results,
        HasCauldronSimple hasCauldron
    ) {
        super(new Property()
            .setItemInputOffset(new Vec3(0.0, -0.375, 0.0))
            .setItemInputRange(new Vec3(0.75, 0.75, 0.75))
            .setInputItems(itemIngredients)
            .setItemOutputOffset(new Vec3(0.0, -0.75, 0.0))
            .setResultItems(results)
            .setCauldronOffset(new Vec3i(0, -1, 0))
            .setHasCauldron(hasCauldron)
            .setBlockInputOffset(new Vec3i(0, -2, 0))
            .setInputBlocks(BlockStatePredicate.builder()
                .of(AddonBlocks.BLENDER.get())
                .with(BlenderBlock.WORKING, true)
                .build()));
    }

    @Override
    public RecipeSerializer<BlendingRecipe> getSerializer() {
        return AddonRecipeTypes.BLENDING_SERIALIZER.get();
    }

    @Override
    public RecipeType<BlendingRecipe> getType() {
        return AddonRecipeTypes.BLENDING_TYPE.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isConsumeFluid() {
        HasCauldronSimple hasCauldron = this.getHasCauldron();
        return hasCauldron.hasFluid() && hasCauldron.consume() > 0;
    }

    public boolean isProduceFluid() {
        HasCauldronSimple hasCauldron = this.getHasCauldron();
        return !hasCauldron.transforms().isEmpty() && hasCauldron.produce() > 0;
    }

    public static final MapCodec<BlendingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemIngredientPredicate.CODEC.listOf().optionalFieldOf("ingredients", List.of())
            .forGetter(BlendingRecipe::getInputItems),
        ChanceItemStack.CODEC.listOf().optionalFieldOf("results", List.of())
            .forGetter(BlendingRecipe::getResultItems),
        HasCauldronSimple.CODEC.forGetter(BlendingRecipe::getHasCauldron)
    ).apply(instance, BlendingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlendingRecipe> STREAM_CODEC =
        StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            BlendingRecipe::getInputItems,
            ChanceItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            BlendingRecipe::getResultItems,
            HasCauldronSimple.STREAM_CODEC,
            BlendingRecipe::getHasCauldron,
            BlendingRecipe::new
        );

    /**
     * 序列化器。
     *
     * <p>26.1 起 {@code RecipeSerializer} 从接口变成了 record，
     * 直接 {@code new RecipeSerializer<>(codec, streamCodec)} 即可，
     * 不再需要自己实现 {@code codec()}/{@code streamCodec()} 的包装类。
     */
    public static final RecipeSerializer<BlendingRecipe> SERIALIZER =
        new RecipeSerializer<>(CODEC, STREAM_CODEC);

    public static class Builder extends SimpleAbstractBuilder<BlendingRecipe, Builder> {
        private final HasCauldronSimple.Builder hasCauldron = HasCauldronSimple.empty();

        public Builder fluid(Identifier fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        public Builder fluid(Block cauldron) {
            this.hasCauldron.fluid(WrapUtils.cauldron2Fluid(cauldron));
            return this;
        }

        public Builder transform(Identifier transform) {
            return this.transform(transform, 1000);
        }

        public Builder transform(Identifier transform, int produce) {
            // AnvilCraft 2386 的 Builder 把 transform 与 produce 拆成两次调用
            this.hasCauldron.transform(transform).produce(produce);
            return this;
        }

        public Builder transform(Block cauldron) {
            return this.transform(cauldron, 1000);
        }

        public Builder transform(Block cauldron, int produce) {
            return this.transform(WrapUtils.cauldron2Fluid(cauldron), produce);
        }

        public Builder consume(int consume) {
            this.hasCauldron.consume(consume);
            return this;
        }

        public Builder chance(float chance) {
            this.hasCauldron.chance(chance);
            return this;
        }

        public Builder ignite() {
            this.hasCauldron.ignite();
            return this;
        }


        @Override
        protected BlendingRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new BlendingRecipe(itemIngredients, results, this.hasCauldron.build());
        }

        @Override
        public String getType() {
            return "blending";
        }

        @Override
        public void validate(Identifier id) {
            if (this.itemIngredients.isEmpty()) {
                throw new IllegalArgumentException("Recipe ingredients must not be empty, RecipeId: " + id);
            }
            if (this.results.isEmpty() && this.hasCauldron.build().transforms().isEmpty()) {
                throw new IllegalArgumentException("Recipe must have results or a fluid transform, RecipeId: " + id);
            }
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
