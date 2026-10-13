package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.anvilcraft.pigeonplus.block.ElectricBlenderBlock;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonRecipeTypes;
import dev.anvilcraft.pigeonplus.recipe.FermentationRecipe;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.entity.BurningHeaterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class FermentationEventListener {
    private static Level lastLevel;
    private static BlockPos lastPos;
    private static long lastTick = Long.MIN_VALUE;

    @SubscribeEvent
    public static void onAnvilLand(AnvilEvent.OnLand event) {
        tryFerment(event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public static void onGiantAnvilLand(AnvilEvent.GiantOnLand event) {
        tryFerment(event.getLevel(), event.getPos());
    }

    private static void tryFerment(Level level, BlockPos anvilPos) {
        if (level.isClientSide()) {
            return;
        }
        LargeCauldronBlockEntity main = findCauldron(level, anvilPos);
        if (main == null) {
            return;
        }
        if (isDuplicate(level, main.getBlockPos())) {
            return;
        }
        if (!hasBlenderAndHeatBelow(level, main.getBlockPos())) {
            return;
        }
        for (RecipeHolder<FermentationRecipe> holder
            : level.getRecipeManager().getAllRecipesFor(AddonRecipeTypes.FERMENTATION_TYPE.get())) {
            if (ferment(main, holder.value())) {
                consumeFuel(level, main.getBlockPos());
                return;
            }
        }
    }

    private static boolean ferment(LargeCauldronBlockEntity main, FermentationRecipe recipe) {
        LargeCauldronFluidHandler fluids = main.getFluids();
        int batches = maxBatches(fluids, recipe);
        if (batches <= 0) {
            return false;
        }
        List<FluidStack> remaining = consume(
            fluids.copyFluids(), recipe.input(), recipe.input().getAmount() * batches);
        if (remaining == null) {
            return false;
        }
        fluids.setFluids(remaining);
        fluids.fill(
            recipe.output().copyWithAmount(recipe.output().getAmount() * batches),
            IFluidHandler.FluidAction.EXECUTE
        );
        return true;
    }

    private static int maxBatches(LargeCauldronFluidHandler fluids, FermentationRecipe recipe) {
        int available = 0;
        for (FluidStack stored : fluids.copyFluids()) {
            if (!stored.isEmpty() && stored.is(recipe.input().getFluid())) {
                available += stored.getAmount();
            }
        }
        int high = available / recipe.input().getAmount();
        if (high <= 0) {
            return 0;
        }
        int low = 0;
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            if (canFit(fluids, recipe, middle)) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    private static boolean canFit(LargeCauldronFluidHandler fluids, FermentationRecipe recipe, int batches) {
        List<FluidStack> remaining = consume(
            fluids.copyFluids(), recipe.input(), recipe.input().getAmount() * batches);
        if (remaining == null) {
            return false;
        }
        LargeCauldronFluidHandler simulated = new LargeCauldronFluidHandler(() -> {
        });
        simulated.setFluids(remaining);
        int filled = simulated.fill(
            recipe.output().copyWithAmount(recipe.output().getAmount() * batches),
            IFluidHandler.FluidAction.EXECUTE
        );
        return filled == recipe.output().getAmount() * batches;
    }

    private static List<FluidStack> consume(List<FluidStack> current, FluidStack input, int amount) {
        List<FluidStack> result = new ArrayList<>(current.size());
        int removed = 0;
        for (FluidStack stored : current) {
            if (stored.isEmpty()) {
                continue;
            }
            if (removed < amount && stored.is(input.getFluid())) {
                int taken = Math.min(stored.getAmount(), amount - removed);
                removed += taken;
                int left = stored.getAmount() - taken;
                if (left > 0) {
                    result.add(stored.copyWithAmount(left));
                }
            } else {
                result.add(stored.copy());
            }
        }
        return removed >= amount ? result : null;
    }

    private static boolean hasBlenderAndHeatBelow(Level level, BlockPos mainPos) {
        BlockPos center = mainPos.below(2);
        boolean hasBlender = false;
        boolean hasHeat = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            BlockState state = level.getBlockState(pos);
            hasBlender |= isWorkingBlender(state);
            hasHeat |= isHeatSource(state);
            if (hasBlender && hasHeat) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWorkingBlender(BlockState state) {
        boolean blender = state.is(AddonBlocks.BLENDER.get()) || state.is(AddonBlocks.ELECTRIC_BLENDER.get());
        return blender && state.getValue(BlenderBlock.WORKING);
    }

    private static boolean isHeatSource(BlockState state) {
        if (isActiveHeater(state)) {
            return true;
        }
        return state.is(AddonBlocks.ELECTRIC_BLENDER.get()) && state.getValue(ElectricBlenderBlock.HEATING);
    }

    private static boolean isActiveHeater(BlockState state) {
        if (state.is(ModBlocks.HEATER)) {
            return !state.getValue(HeaterBlock.OVERLOAD);
        }
        return state.is(ModBlocks.BURNING_HEATER) && state.getValue(BurningHeaterBlock.LEVEL) == 2;
    }

    private static void consumeFuel(Level level, BlockPos mainPos) {
        BlockPos center = mainPos.below(2);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            BlockState state = level.getBlockState(pos);
            boolean lit = state.is(ModBlocks.BURNING_HEATER)
                && state.getValue(BurningHeaterBlock.LEVEL) == 2;
            if (lit && level.getBlockEntity(pos) instanceof BurningHeaterBlockEntity heater) {
                heater.consumeBurnTime(SuperHeatingRecipe.ConsumeFuel.FUEL_COST_TICKS);
                return;
            }
        }
    }

    private static boolean isDuplicate(Level level, BlockPos pos) {
        long tick = level.getGameTime();
        if (level == lastLevel && pos.equals(lastPos) && tick == lastTick) {
            return true;
        }
        lastLevel = level;
        lastPos = pos;
        lastTick = tick;
        return false;
    }

    private static LargeCauldronBlockEntity findCauldron(Level level, BlockPos anvilPos) {
        for (BlockPos pos : BlockPos.betweenClosed(
            anvilPos.offset(-1, -1, -1), anvilPos.offset(1, -1, 1))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.LARGE_CAULDRON)) {
                continue;
            }
            LargeCauldronBlockEntity main = LargeCauldronBlockEntity.getMain(level, pos, state);
            if (main != null && main.isMainPart()) {
                return main;
            }
        }
        return null;
    }
}
