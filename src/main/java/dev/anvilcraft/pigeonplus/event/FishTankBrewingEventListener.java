package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.fluid.PotionBrewingHelper;
import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class FishTankBrewingEventListener {
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
        FishTankBlockEntity tank = findTank(level, anvilPos);
        if (tank == null) {
            return;
        }
        BlockPos tankPos = tank.getBlockPos();
        // 按鱼缸位置去重：巨型铁砧会对下方每个方块各触发一次，按事件位置去重拦不住。
        if (isDuplicate(level, tankPos)) {
            return;
        }
        IFluidHandler fluids = tank.getFluidHandler();
        FluidStack stored = fluids.getFluidInTank(0);
        IItemHandler input = tank.getInput();
        PotionContents contents = PotionBrewingHelper.resolveContents(stored);
        if (contents != null && BrewingBlender.hasBrewingBlenderBelow(level, tankPos)) {
            PotionBrewingHelper.Selection selection =
                PotionBrewingHelper.select(level.potionBrewing(), contents, input);
            if (selection != null && brew(level, tankPos, fluids, input, stored, selection)) {
                return;
            }
        }
        if (PotionBrewingHelper.tryMakeSoaked(fluids, input, tank.getOutputHandler())) {
            level.playSound(null, tankPos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private static boolean brew(
        Level level,
        BlockPos tankPos,
        IFluidHandler fluids,
        IItemHandler input,
        FluidStack stored,
        PotionBrewingHelper.Selection selection
    ) {
        int amount = stored.getAmount();
        fluids.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        FluidStack product = PotionFluidUtil.withContents(
            new FluidStack(AddonFluids.POTION.get(), amount), selection.result());
        int filled = fluids.fill(product, IFluidHandler.FluidAction.EXECUTE);
        if (filled < amount) {
            // 产物放不回去：收回已放入的、还原原流体，并且不消耗材料。
            if (filled > 0) {
                fluids.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            }
            fluids.fill(stored, IFluidHandler.FluidAction.EXECUTE);
            return false;
        }
        input.extractItem(selection.slot(), 1, false);
        level.playSound(null, tankPos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private static boolean isDuplicate(Level level, BlockPos pos) {        long tick = level.getGameTime();
        if (level == lastLevel && pos.equals(lastPos) && tick == lastTick) {
            return true;
        }
        lastLevel = level;
        lastPos = pos;
        lastTick = tick;
        return false;
    }

    private static FishTankBlockEntity findTank(Level level, BlockPos anvilPos) {
        if (level.getBlockEntity(anvilPos.below()) instanceof FishTankBlockEntity direct) {
            return direct;
        }
        // 巨型铁砧是 3x3，getPos 未必落在鱼缸正上方，退化为扫下方一层 3x3。
        for (BlockPos pos : BlockPos.betweenClosed(
            anvilPos.offset(-1, -1, -1), anvilPos.offset(1, -1, 1))) {
            if (level.getBlockEntity(pos) instanceof FishTankBlockEntity tank) {
                return tank;
            }
        }
        return null;
    }
}
