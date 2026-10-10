package dev.anvilcraft.pigeonplus.integration.jei.recipe;

import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import dev.anvilcraft.pigeonplus.fluid.PotionSoakingRecipes;
import dev.anvilcraft.pigeonplus.fluid.PotionVariants;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PotionBrewingJeiRecipes {
    private PotionBrewingJeiRecipes() {
    }

    public static List<PotionBrewingJeiRecipe> build(int amount) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return List.of();
        }
        PotionBrewing brewing = connection.potionBrewing();
        List<ItemStack> reagents = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (brewing.isIngredient(stack)) {
                reagents.add(stack);
            }
        }

        Map<Item, ItemStack> reagentStacks = new LinkedHashMap<>();
        Map<Item, List<FluidStack>> inputs = new LinkedHashMap<>();
        Map<Item, List<FluidStack>> outputs = new LinkedHashMap<>();
        Map<Item, List<Holder<Potion>>> resultPotions = new LinkedHashMap<>();
        BuiltInRegistries.POTION.listElements().forEach(potion -> {
            ItemStack potionStack = new ItemStack(Items.POTION);
            potionStack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
            for (ItemStack reagent : reagents) {
                if (!brewing.hasMix(potionStack, reagent)) {
                    continue;
                }
                ItemStack mixed = brewing.mix(reagent, potionStack);

                if (!mixed.is(Items.POTION)) {
                    continue;
                }
                PotionContents result = mixed.get(DataComponents.POTION_CONTENTS);
                if (result == null || result.potion().isEmpty()) {
                    continue;
                }

                if (result.potion().get().is(Potions.MUNDANE) || result.potion().get().is(Potions.THICK)) {
                    continue;
                }
                Item key = reagent.getItem();
                reagentStacks.putIfAbsent(key, reagent);
                inputs.computeIfAbsent(key, k -> new ArrayList<>()).add(inputFluid(potion, amount));
                outputs.computeIfAbsent(key, k -> new ArrayList<>()).add(toFluid(result, amount));
                resultPotions.computeIfAbsent(key, k -> new ArrayList<>()).add(result.potion().get());
            }
        });
        List<PotionBrewingJeiRecipe> basic = new ArrayList<>();
        List<PotionBrewingJeiRecipe> corruption = new ArrayList<>();
        List<PotionBrewingJeiRecipe> enhancement = new ArrayList<>();
        reagentStacks.forEach((key, stack) -> {
            List<FluidStack> ins = inputs.get(key);
            List<FluidStack> outs = outputs.get(key);

            if (hasDuplicateResults(resultPotions.get(key))) {
                for (int i = 0; i < ins.size(); i++) {
                    corruption.add(new PotionBrewingJeiRecipe(stack, List.of(ins.get(i)), List.of(outs.get(i))));
                }
                return;
            }
            PotionBrewingJeiRecipe recipe = new PotionBrewingJeiRecipe(stack, ins, outs);

            if (isEnhancementReagent(resultPotions.get(key), ins)) {
                enhancement.add(recipe);
            } else {
                basic.add(recipe);
            }
        });

        List<PotionBrewingJeiRecipe> recipes = new ArrayList<>(basic);
        recipes.addAll(corruption);
        recipes.addAll(enhancement);
        return recipes;
    }

    private static boolean isEnhancementReagent(List<Holder<Potion>> results, List<FluidStack> inputs) {
        if (results.isEmpty()) {
            return false;
        }
        for (int i = 0; i < results.size(); i++) {
            Holder<Potion> result = results.get(i);
            if (!PotionVariants.isEnhanced(result)) {
                return false;
            }
            if (!PotionVariants.basePath(result).equals(potionPathOf(inputs.get(i)))) {
                return false;
            }
        }
        return true;
    }

    private static String potionPathOf(FluidStack fluid) {
        PotionContents contents = PotionFluidUtil.getContents(fluid);
        if (contents == null || contents.potion().isEmpty()) {
            return "";
        }
        return PotionVariants.basePath(contents.potion().get());
    }

    private static boolean hasDuplicateResults(List<Holder<Potion>> outputs) {
        Set<String> seen = new HashSet<>();
        for (Holder<Potion> output : outputs) {
            if (!seen.add(PotionVariants.path(output))) {
                return true;
            }
        }
        return false;
    }

    private static FluidStack toFluid(PotionContents contents, int amount) {
        return PotionFluidUtil.withContents(new FluidStack(AddonFluids.POTION.get(), amount), contents);
    }

    private static FluidStack inputFluid(Holder<Potion> potion, int amount) {
        if (potion.is(Potions.WATER)) {
            return new FluidStack(Fluids.WATER, amount);
        }
        return toFluid(new PotionContents(potion), amount);
    }

    public static List<ItemStack> potionItemsOf(List<FluidStack> fluids) {
        List<ItemStack> items = new ArrayList<>();
        for (FluidStack fluid : fluids) {
            items.add(potionItemOf(fluid));
        }
        return items;
    }

    private static ItemStack potionItemOf(FluidStack fluid) {
        if (fluid.is(Fluids.WATER)) {

            return potionItem(new PotionContents(Potions.WATER));
        }
        return potionItem(PotionFluidUtil.getContents(fluid));
    }

    private static ItemStack potionItem(PotionContents contents) {
        ItemStack item = new ItemStack(Items.POTION);
        item.set(DataComponents.POTION_CONTENTS, contents);
        return item;
    }

    public static List<PotionSoakingJeiRecipe> buildSoaking(int amount) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return List.of();
        }
        PotionBrewing brewing = connection.potionBrewing();
        List<ItemStack> reagents = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (brewing.isIngredient(stack)) {
                reagents.add(stack);
            }
        }

        Map<String, PotionContents> reachable = new LinkedHashMap<>();
        BuiltInRegistries.POTION.listElements().forEach(potion -> {
            ItemStack potionStack = new ItemStack(Items.POTION);
            potionStack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
            for (ItemStack reagent : reagents) {
                if (!brewing.hasMix(potionStack, reagent)) {
                    continue;
                }
                ItemStack mixed = brewing.mix(reagent, potionStack);

                if (!mixed.is(Items.POTION)) {
                    continue;
                }
                PotionContents result = mixed.get(DataComponents.POTION_CONTENTS);
                if (result == null || result.potion().isEmpty()) {
                    continue;
                }
                if (result.potion().get().is(Potions.MUNDANE) || result.potion().get().is(Potions.THICK)) {
                    continue;
                }
                reachable.putIfAbsent(PotionVariants.path(result.potion().get()), result);
            }
        });

        reachable.putIfAbsent("water", new PotionContents(Potions.WATER));
        List<PotionSoakingJeiRecipe> recipes = new ArrayList<>();

        for (PotionSoakingRecipes.Soakable soakable : PotionSoakingRecipes.all()) {
            List<FluidStack> potions = new ArrayList<>();
            List<ItemStack> results = new ArrayList<>();
            reachable.forEach((path, contents) -> {
                potions.add(inputFluidOf(contents, amount));
                results.add(soakable.soakedWith(contents));
            });
            recipes.add(new PotionSoakingJeiRecipe(soakable.blank(), potions, results));
        }

        for (PotionSoakingRecipes.Transformation transformation : PotionSoakingRecipes.transformations()) {
            List<FluidStack> potions = new ArrayList<>();
            reachable.forEach((path, contents) -> {
                if (transformation.acceptsPotion(contents)) {
                    potions.add(inputFluidOf(contents, amount));
                }
            });
            if (!potions.isEmpty()) {
                recipes.add(new PotionSoakingJeiRecipe(
                    transformation.inputStack(),
                    potions,
                    List.of(transformation.outputStack())
                ));
            }
        }
        return recipes;
    }

    private static FluidStack inputFluidOf(PotionContents contents, int amount) {
        if (contents.potion().isPresent() && contents.potion().get().is(Potions.WATER)) {
            return new FluidStack(Fluids.WATER, amount);
        }
        return toFluid(contents, amount);
    }
}
