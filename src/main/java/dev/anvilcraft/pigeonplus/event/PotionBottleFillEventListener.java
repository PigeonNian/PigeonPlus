package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.fluid.PotionBottleTypes;
import dev.anvilcraft.pigeonplus.fluid.PotionFluid;
import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class PotionBottleFillEventListener {
    private static final int FLUID_PER_BOTTLE = 250;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        ItemStack held = event.getItemStack();
        if (PotionBottleTypes.isPotion(held)
            && level.getBlockState(pos).is(ModBlocks.CREATIVE_FLUID_TANK)) {
            return;
        }
        ResourceHandler<FluidResource> handler =
            level.getCapability(Capabilities.Fluid.BLOCK, pos, event.getFace());
        if (handler == null) {
            return;
        }
        if (!withdrawPotion(event, player, level, pos, handler, held)) {
            depositPotion(event, player, level, pos, handler, held);
        }
    }

    private static boolean withdrawPotion(
        PlayerInteractEvent.RightClickBlock event,
        Player player,
        Level level,
        BlockPos pos,
        ResourceHandler<FluidResource> handler,
        ItemStack bottle
    ) {
        Item filledItem = PotionBottleTypes.filledFor(bottle);
        if (filledItem == null || handler.size() == 0) {
            return false;
        }
        FluidStack stored = handler.getResource(0).toStack(handler.getAmountAsInt(0));
        if (!(stored.getFluid() instanceof PotionFluid) || stored.getAmount() < FLUID_PER_BOTTLE) {
            return false;
        }
        PotionContents contents = PotionFluidUtil.getContents(stored);
        if (contents == null) {
            return false;
        }
        if (!level.isClientSide()) {
            try (Transaction transaction = Transaction.openRoot()) {
                int drained = handler.extract(FluidResource.of(stored), FLUID_PER_BOTTLE, transaction);
                if (drained < FLUID_PER_BOTTLE) {
                    return false;
                }
                transaction.commit();
            }
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        ItemStack filled = new ItemStack(filledItem);
        filled.set(DataComponents.POTION_CONTENTS, contents);
        player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(bottle, player, filled));
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        return true;
    }

    private static void depositPotion(
        PlayerInteractEvent.RightClickBlock event,
        Player player,
        Level level,
        BlockPos pos,
        ResourceHandler<FluidResource> handler,
        ItemStack potion
    ) {
        if (!PotionBottleTypes.isPotion(potion)) {
            return;
        }
        PotionContents contents = potion.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return;
        }
        FluidStack toFill =
            PotionFluidUtil.withContents(new FluidStack(AddonFluids.POTION.get(), FLUID_PER_BOTTLE), contents);
        FluidResource resource = FluidResource.of(toFill);
        if (!level.isClientSide()) {
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = handler.insert(resource, FLUID_PER_BOTTLE, transaction);
                if (inserted < FLUID_PER_BOTTLE) {
                    return;
                }
                transaction.commit();
            }
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        player.setItemInHand(
            event.getHand(),
            ItemUtils.createFilledResult(potion, player, PotionBottleTypes.emptyFor(potion))
        );
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
