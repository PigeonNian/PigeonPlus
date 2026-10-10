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
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class LargeCauldronBrewingEventListener {

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

        if (isDuplicate(level, main.getBlockPos())) {
            return;
        }
        LargeCauldronFluidHandler fluids = main.getFluids();

        LargeCauldronInputHandler input = main.getInputHandler();

        if (AmbientHeater.hasActiveHeaterArrayBelow(
            level, main.getBlockPos(), level.getBlockState(main.getBlockPos()))) {
            if (brewAny(level, main, fluids, input)) {
                return;
            }
        }

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
        FluidResource storedResource = FluidResource.of(stored);
        FluidResource productResource = FluidResource.of(PotionFluidUtil.withContents(
            new FluidStack(AddonFluids.POTION.get(), amount), selection.result()));
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
