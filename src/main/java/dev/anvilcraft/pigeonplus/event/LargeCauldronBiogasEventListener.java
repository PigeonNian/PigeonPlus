package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class LargeCauldronBiogasEventListener {
    private static final int BIOMASS_PER_REACTION = FluidType.BUCKET_VOLUME;
    private static final int BIOGAS_PER_REACTION = 2 * FluidType.BUCKET_VOLUME;
    private static int lastImpactEntityId = Integer.MIN_VALUE;
    private static long lastImpactGameTime = Long.MIN_VALUE;
    private static BlockPos lastImpactCauldronPos = BlockPos.ZERO;

    @SubscribeEvent
    public static void onGiantAnvilLand(AnvilEvent.OnLand event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !(event.getEntity() instanceof FallingGiantAnvilEntity)) {
            return;
        }

        BlockPos hitBlockPos = event.getPos().below();
        BlockState hitBlockState = level.getBlockState(hitBlockPos);
        if (!(hitBlockState.getBlock() instanceof LargeCauldronBlock)) {
            return;
        }
        LargeCauldronBlockEntity cauldron = LargeCauldronBlockEntity.getMain(level, hitBlockPos, hitBlockState);
        if (cauldron == null || !isLandedGiantAnvilAboveCauldron(level, event.getPos(), cauldron.getBlockPos())) {
            return;
        }
        if (!hasActiveHeaterBelow(level, cauldron.getBlockPos())) {
            return;
        }
        if (isDuplicateImpact(event, cauldron.getBlockPos())) {
            return;
        }
        rememberImpact(event, cauldron.getBlockPos());
        convertMixedBiomass(cauldron);
    }

    private static boolean isLandedGiantAnvilAboveCauldron(Level level, BlockPos landedPos, BlockPos cauldronMainPos) {
        BlockState landedState = level.getBlockState(landedPos);
        return landedState.getBlock() instanceof GiantAnvilBlock giantAnvil
            && giantAnvil.getMainPartPos(landedPos, landedState).equals(cauldronMainPos.above(3));
    }

    private static boolean hasActiveHeaterBelow(Level level, BlockPos cauldronMainPos) {
        BlockPos center = cauldronMainPos.below(2);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.HEATER) && state.getBlock() instanceof HeaterBlock heater && heater.isActive(state)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDuplicateImpact(AnvilEvent.OnLand event, BlockPos cauldronMainPos) {
        return lastImpactEntityId == event.getEntity().getId()
            && lastImpactGameTime == event.getLevel().getGameTime()
            && lastImpactCauldronPos.equals(cauldronMainPos);
    }

    private static void rememberImpact(AnvilEvent.OnLand event, BlockPos cauldronMainPos) {
        lastImpactEntityId = event.getEntity().getId();
        lastImpactGameTime = event.getLevel().getGameTime();
        lastImpactCauldronPos = cauldronMainPos.immutable();
    }

    private static void convertMixedBiomass(LargeCauldronBlockEntity cauldron) {
        LargeCauldronFluidHandler handler = cauldron.getFluids();
        List<FluidStack> fluids = handler.copyFluids();
        int mixedBiomassIndex = -1;
        int biogasIndex = -1;
        int activeLayers = 0;
        for (int i = 0; i < fluids.size(); i++) {
            FluidStack stack = fluids.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            activeLayers++;
            if (stack.getFluid().isSame(AddonFluids.MIXED_BIOMASS.get())) {
                mixedBiomassIndex = i;
            } else if (stack.getFluid().isSame(AddonFluids.GASEOUS_BIOGAS.get())) {
                biogasIndex = i;
            }
        }
        if (mixedBiomassIndex < 0) {
            return;
        }

        FluidStack mixedBiomass = fluids.get(mixedBiomassIndex);
        int biogasRoom = biogasIndex >= 0
            ? LargeCauldronFluidHandler.TANK_CAPACITY - fluids.get(biogasIndex).getAmount()
            : LargeCauldronFluidHandler.TANK_CAPACITY;
        int reactions = Math.min(mixedBiomass.getAmount() / BIOMASS_PER_REACTION, biogasRoom / BIOGAS_PER_REACTION);
        if (reactions <= 0) {
            return;
        }
        int inputAmount = reactions * BIOMASS_PER_REACTION;
        if (biogasIndex < 0
            && activeLayers >= LargeCauldronFluidHandler.TANK_COUNT
            && inputAmount < mixedBiomass.getAmount()) {
            return;
        }

        int outputAmount = reactions * BIOGAS_PER_REACTION;
        List<FluidStack> result = new ArrayList<>(LargeCauldronFluidHandler.TANK_COUNT);
        boolean addedBiogas = false;
        for (int i = 0; i < fluids.size(); i++) {
            FluidStack stack = fluids.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (i == mixedBiomassIndex) {
                int remaining = stack.getAmount() - inputAmount;
                if (remaining > 0) {
                    result.add(stack.copyWithAmount(remaining));
                }
            } else if (i == biogasIndex) {
                result.add(stack.copyWithAmount(stack.getAmount() + outputAmount));
                addedBiogas = true;
            } else {
                result.add(stack.copy());
            }
        }
        if (!addedBiogas) {
            result.add(new FluidStack(AddonFluids.GASEOUS_BIOGAS.get(), outputAmount));
        }
        handler.setFluids(result);
    }
}
