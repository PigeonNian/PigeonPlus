package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.anvilcraft.pigeonplus.block.ElectricBlenderBlock;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonRecipeTypes;
import dev.anvilcraft.pigeonplus.recipe.ReformingRecipe;
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

/**
 * 蒸汽重整：巨型铁砧砸在大型炼药锅上，把锅里的甲烷（液化沼气）与水重整成氢气。
 *
 * <p>触发只认巨型铁砧：普通铁砧及各种变体砸锅不产生重整反应，
 * 这与 JEI 分类里只登记巨型铁砧作为催化剂一致。
 *
 * <p>与发酵共用同一套「搅拌机 + 热源」判定，额外要求锅下 3×3 内有一块磁铁块当催化剂。
 * 催化剂不消耗：它代表重整炉里的镍基催化剂床，只提供反应条件，不进入产物。
 */
@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class ReformingEventListener {
    private static Level lastLevel;
    private static BlockPos lastPos;
    private static long lastTick = Long.MIN_VALUE;

    /**
     * 只订阅巨型铁砧落地：大型炼药锅的重整必须由巨型铁砧砸下才成立，
     * 普通铁砧（以及皇家/余烬/浮霜等变体）不参与。JEI 的催化剂列表与此保持一致。
     */
    @SubscribeEvent
    public static void onGiantAnvilLand(AnvilEvent.GiantOnLand event) {
        tryReform(event.getLevel(), event.getPos());
    }

    private static void tryReform(Level level, BlockPos anvilPos) {
        if (level.isClientSide()) {
            return;
        }
        LargeCauldronBlockEntity main = findCauldron(level, anvilPos);
        if (main == null || isDuplicate(level, main.getBlockPos())) {
            return;
        }
        if (!hasReformingSetup(level, main.getBlockPos())) {
            return;
        }
        for (RecipeHolder<ReformingRecipe> holder
            : level.getRecipeManager().getAllRecipesFor(AddonRecipeTypes.REFORMING_TYPE.get())) {
            if (reform(main, holder.value())) {
                consumeFuel(level, main.getBlockPos());
                return;
            }
        }
    }

    private static boolean reform(LargeCauldronBlockEntity main, ReformingRecipe recipe) {
        LargeCauldronFluidHandler fluids = main.getFluids();
        List<FluidStack> current = fluids.copyFluids();
        int batches = maxBatches(current, recipe);
        if (batches <= 0) {
            return false;
        }
        List<FluidStack> remaining = current;
        for (FluidStack input : recipe.inputs()) {
            remaining = consume(remaining, input, input.getAmount() * batches);
            if (remaining == null) {
                return false;
            }
        }
        fluids.setFluids(remaining);
        fluids.fill(
            recipe.output().copyWithAmount(recipe.output().getAmount() * batches),
            IFluidHandler.FluidAction.EXECUTE
        );
        return true;
    }

    /**
     * 在「输入够用」与「产物放得下」两个约束下求最大批次。
     *
     * <p>输入上限直接取各原料能支持的最小批次；产物一侧随批次线性增长，用二分找最大可行值。
     */
    private static int maxBatches(List<FluidStack> current, ReformingRecipe recipe) {
        int high = Integer.MAX_VALUE;
        for (FluidStack input : recipe.inputs()) {
            int available = 0;
            for (FluidStack stored : current) {
                if (!stored.isEmpty() && FluidStack.isSameFluidSameComponents(stored, input)) {
                    available += stored.getAmount();
                }
            }
            high = Math.min(high, available / input.getAmount());
        }
        // 产物体积也可能先到上限，先按 int 安全范围封顶
        high = Math.min(high, Integer.MAX_VALUE / Math.max(1, recipe.output().getAmount()));
        if (high <= 0) {
            return 0;
        }
        int low = 0;
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            if (canFit(current, recipe, middle)) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    private static boolean canFit(List<FluidStack> current, ReformingRecipe recipe, int batches) {
        List<FluidStack> remaining = current;
        for (FluidStack input : recipe.inputs()) {
            remaining = consume(remaining, input, input.getAmount() * batches);
            if (remaining == null) {
                return false;
            }
        }
        LargeCauldronFluidHandler simulated = new LargeCauldronFluidHandler(() -> {
        });
        simulated.setFluids(remaining);
        int amount = recipe.output().getAmount() * batches;
        return simulated.fill(recipe.output().copyWithAmount(amount), IFluidHandler.FluidAction.EXECUTE) == amount;
    }

    /**
     * 从流体表里扣掉指定量，返回扣完后的新表；存量不足时返回 {@code null}。
     */
    private static List<FluidStack> consume(List<FluidStack> current, FluidStack input, int amount) {
        List<FluidStack> result = new ArrayList<>(current.size());
        int removed = 0;
        for (FluidStack stored : current) {
            if (stored.isEmpty()) {
                continue;
            }
            if (removed < amount && FluidStack.isSameFluidSameComponents(stored, input)) {
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

    /**
     * 锅下 3×3 内必须同时具备：运行中的搅拌机、热源、磁铁块（催化剂）。
     */
    private static boolean hasReformingSetup(Level level, BlockPos mainPos) {
        BlockPos center = mainPos.below(2);
        boolean hasBlender = false;
        boolean hasHeat = false;
        boolean hasCatalyst = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            BlockState state = level.getBlockState(pos);
            hasBlender |= isWorkingBlender(state);
            hasHeat |= isHeatSource(state);
            hasCatalyst |= state.is(ModBlocks.MAGNET_BLOCK.get());
            if (hasBlender && hasHeat && hasCatalyst) {
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
