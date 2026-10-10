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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

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
        if (isDuplicate(level, tankPos)) {
            return;
        }
        ResourceHandler<FluidResource> fluids = tank.getFluidHandler();
        FluidStack stored = fluids.getResource(0).toStack(fluids.getAmountAsInt(0));
        ResourceHandler<ItemResource> input = tank.getInput();
        PotionContents contents = PotionBrewingHelper.resolveContents(stored);
        if (contents != null && AmbientHeater.hasActiveHeaterBelow(level, tankPos)) {
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
        ResourceHandler<FluidResource> fluids,
        ResourceHandler<ItemResource> input,
        FluidStack stored,
        PotionBrewingHelper.Selection selection
    ) {
        int amount = stored.getAmount();
        FluidResource storedResource = FluidResource.of(stored);
        FluidResource productResource = FluidResource.of(
            PotionFluidUtil.withContents(new FluidStack(AddonFluids.POTION.get(), amount), selection.result()));
        try (Transaction transaction = Transaction.openRoot()) {
            int drained = fluids.extract(storedResource, amount, transaction);
            if (drained <= 0) {
                return false;
            }
            int filled = fluids.insert(productResource, drained, transaction);
            if (filled < drained) {
                return false;
            }
            int extracted = input.extract(selection.slot(), input.getResource(selection.slot()), 1, transaction);
            if (extracted < 1) {
                return false;
            }
            transaction.commit();
        }
        level.playSound(null, tankPos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
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

    private static FishTankBlockEntity findTank(Level level, BlockPos anvilPos) {
        if (level.getBlockEntity(anvilPos.below()) instanceof FishTankBlockEntity direct) {
            return direct;
        }
        for (BlockPos pos : BlockPos.betweenClosed(
            anvilPos.offset(-1, -1, -1), anvilPos.offset(1, -1, 1))) {
            if (level.getBlockEntity(pos) instanceof FishTankBlockEntity tank) {
                return tank;
            }
        }
        return null;
    }
}
