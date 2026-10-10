package dev.anvilcraft.pigeonplus.fluid;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class PotionSoakingRecipes {

    public static final int FLUID_COST = 250;

    private static final int PILL_COUNT = 2;
    private static final int ARROW_COUNT = 8;

    private static final Identifier WEAK_RESIN_BLOCK =
        Identifier.fromNamespaceAndPath("anvilcraft_pigsplus", "weak_resin_block");
    private static final String WEAKNESS = "weakness";
    private static final int RESIN_COUNT = 1;

    private PotionSoakingRecipes() {
    }

    public static List<Soakable> all() {
        return SOAKABLE;
    }

    private static final List<Soakable> SOAKABLE = List.of(
        new Soakable(
            () -> new ItemStack(ModFoodItems.PILL.get(), PILL_COUNT),
            PotionSoakingRecipes::pillWith
        ),
        new Soakable(
            () -> new ItemStack(Items.ARROW, ARROW_COUNT),
            PotionSoakingRecipes::tippedArrowWith
        )
    );

    public static List<Transformation> transformations() {
        List<Transformation> list = new ArrayList<>();
        BuiltInRegistries.ITEM.getOptional(WEAK_RESIN_BLOCK).ifPresent(weakResinBlock ->
            list.add(new Transformation(
                new ItemStack(ModBlocks.RESIN_BLOCK.get(), RESIN_COUNT),
                WEAKNESS,
                new ItemStack(weakResinBlock, RESIN_COUNT)
            ))
        );
        return List.copyOf(list);
    }

    public record Soakable(
        Supplier<ItemStack> blankSupplier,
        Function<PotionContents, ItemStack> soaker
    ) {
        public ItemStack blank() {
            return this.blankSupplier.get();
        }

        public ItemStack soakedWith(PotionContents contents) {
            return this.soaker.apply(contents);
        }

        public int count() {
            return this.blank().getCount();
        }

        public boolean matches(ItemStack stack) {
            return stack.is(this.blank().getItem());
        }
    }

    public record Transformation(ItemStack input, String potionPath, ItemStack output) {
        public ItemStack inputStack() {
            return this.input.copy();
        }

        public ItemStack outputStack() {
            return this.output.copy();
        }

        public int count() {
            return this.input.getCount();
        }

        public boolean matches(ItemStack stack) {
            return stack.is(this.input.getItem());
        }

        public boolean acceptsPotion(PotionContents contents) {
            return contents.potion().isPresent()
                && PotionVariants.basePath(contents.potion().get()).equals(this.potionPath);
        }
    }

    private static ItemStack pillWith(PotionContents contents) {
        ItemStack pills = new ItemStack(ModFoodItems.PILL.get(), PILL_COUNT);
        pills.set(DataComponents.POTION_CONTENTS, contents);
        return pills;
    }

    private static ItemStack tippedArrowWith(PotionContents contents) {
        ItemStack arrows = new ItemStack(Items.TIPPED_ARROW, ARROW_COUNT);
        arrows.set(DataComponents.POTION_CONTENTS, contents);
        return arrows;
    }
}
