package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.fluid.PotionBrewingHelper;
import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.api.itemhandler.LargeCauldronInputHandler;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class LargeCauldronBrewingEventListener {
    // 一次酿造 8B，对应大型炼药锅的 8 个储罐各 1 桶。
    private static final int BREW_AMOUNT = 8000;

    private static Level lastLevel;
    private static BlockPos lastPos;
    private static long lastTick = Long.MIN_VALUE;

    @SubscribeEvent
    public static void onAnvilLand(AnvilEvent.OnLand event) {
        tryBrew(event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public static void onGiantAnvilLand(AnvilEvent.GiantOnLand event) {
        tryBrew(event.getLevel(), event.getPos());
    }

    private static void tryBrew(Level level, BlockPos anvilPos) {
        if (level.isClientSide()) {
            return;
        }
        LargeCauldronBlockEntity main = findCauldron(level, anvilPos);
        if (main == null) {
            return;
        }
        // 按结构主部件去重：3x3 的锅会对每个顶块各触发一次 OnLand（共 9 次），
        // 按事件位置去重拦不住，会让一次砸落酿造 9 次。
        if (isDuplicate(level, main.getBlockPos())) {
            return;
        }
        LargeCauldronFluidHandler fluids = main.getFluids();
        // 必须用 getInputHandler()：getInput() 在非处理状态下返回 EMPTY_HANDLER，恒为空。
        LargeCauldronInputHandler input = main.getInputHandler();
        if (BrewingBlender.hasBulkBrewingBlenderArrayBelow(
            level, main.getBlockPos(), level.getBlockState(main.getBlockPos()))) {
            if (brewAny(level, main, fluids, input)) {
                return;
            }
        }
        // 锅内可能同时存在多种药水，逐个尝试，取第一个有可用配方的。
        // 只看第一种会让「迅捷排在粗制之前」时永远酿不动粗制。
        if (PotionBrewingHelper.tryMakeSoaked(fluids, input, main.getOutputHandler())) {
            level.playSound(null, main.getBlockPos(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private static boolean brewAny(
        Level level,
        LargeCauldronBlockEntity main,
        LargeCauldronFluidHandler fluids,
        LargeCauldronInputHandler input
    ) {
        PotionBrewing brewing = level.potionBrewing();
        for (FluidStack stored : fluids.copyFluids()) {
            if (stored.isEmpty()) {
                continue;
            }
            PotionContents contents = PotionBrewingHelper.resolveContents(stored);
            if (contents == null) {
                continue;
            }
            PotionBrewingHelper.Selection selection = PotionBrewingHelper.select(brewing, contents, input);
            if (selection == null) {
                continue;
            }
            if (brew(level, main, fluids, input, stored, selection)) {
                return true;
            }
        }
        return false;
    }

    private static boolean brew(
        Level level,
        LargeCauldronBlockEntity main,
        LargeCauldronFluidHandler fluids,
        LargeCauldronInputHandler input,
        FluidStack stored,
        PotionBrewingHelper.Selection selection
    ) {
        int amount = Math.min(stored.getAmount(), BREW_AMOUNT);
        // 匹配含组件（isSameFluidSameComponents），所以要先带上原有组件再抽。
        FluidStack drained =
            fluids.drainStoredFluid(stored.copyWithAmount(amount), IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return false;
        }
        FluidStack product = PotionFluidUtil.withContents(
            new FluidStack(AddonFluids.POTION.get(), drained.getAmount()), selection.result());
        int filled = fluids.fill(product, IFluidHandler.FluidAction.EXECUTE);
        if (filled < drained.getAmount()) {
            // 产物放不回去（该药水的罐已满且无空罐）：收回已放入的、还原原流体，并且不消耗材料。
            if (filled > 0) {
                fluids.drainStoredFluid(product.copyWithAmount(filled), IFluidHandler.FluidAction.EXECUTE);
            }
            fluids.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            return false;
        }
        input.extractItem(selection.slot(), 1, false);
        level.playSound(null, main.getBlockPos(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
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
