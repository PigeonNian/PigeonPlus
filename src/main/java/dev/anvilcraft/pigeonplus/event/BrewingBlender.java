package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.anvilcraft.pigeonplus.block.ElectricBlenderBlock;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.entity.BurningHeaterBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 酿造对搅拌机与热源的要求。
 *
 * <p>鱼缸：正下方紧贴一台**装了烈焰搅拌头、正在运转**的搅拌机（气动、电动都行）即可。
 *
 * <p>大型炼药锅：锅底下方 3×3 里要**同时**满足两件事——一台装了烈焰搅拌头、正在运转的搅拌机，
 * 以及一个热源（电加热器、点燃的燃烧加热器，或正在加热的电搅拌机）。烈焰搅拌头只负责搅拌，
 * 不能顶替热源。
 */
public final class BrewingBlender {
    private static final int MAX_STRUCTURE_HEIGHT = 16;

    private BrewingBlender() {
    }

    public static boolean hasBrewingBlenderBelow(Level level, BlockPos vesselPos) {
        return isRunningBlazeBlender(level.getBlockState(vesselPos.below()));
    }

    /**
     * 大锅批量酿造：锅底下方 3×3 里「装烈焰搅拌头的运转中搅拌机」和「热源」两样都要有。
     */
    public static boolean hasBulkBrewingBlenderArrayBelow(
        Level level,
        BlockPos mainPos,
        BlockState structureBlock
    ) {
        BlockPos center = findBrewingBase(level, mainPos, structureBlock);
        boolean stirring = false;
        boolean heat = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            BlockState state = level.getBlockState(pos);
            stirring |= isRunningBlazeBlender(state);
            heat |= isHeatSource(state);
            if (stirring && heat) {
                return true;
            }
        }
        return false;
    }

    /**
     * 一次批量酿造之后扣掉燃烧加热器的燃烧时间。
     *
     * <p>只有热量确实来自燃烧加热器时才扣：3×3 里如果有正在加热的电搅拌机，说明热是电来的，不烧燃料。
     */
    public static void consumeBulkBrewingFuel(Level level, BlockPos mainPos, BlockState structureBlock) {
        BlockPos center = findBrewingBase(level, mainPos, structureBlock);
        BlockPos burningHeaterPos = null;
        boolean electricHeat = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            BlockState state = level.getBlockState(pos);
            if (isHeatingElectricBlender(state)) {
                electricHeat = true;
            } else if (burningHeaterPos == null && isLitBurningHeater(state)) {
                burningHeaterPos = pos;
            }
        }
        if (electricHeat || burningHeaterPos == null) {
            return;
        }
        if (level.getBlockEntity(burningHeaterPos) instanceof BurningHeaterBlockEntity heater) {
            heater.consumeBurnTime(SuperHeatingRecipe.ConsumeFuel.FUEL_COST_TICKS);
        }
    }

    /**
     * 从结构主部件往下走，返回锅底正下方那一层 3×3 的中心位置。
     */
    private static BlockPos findBrewingBase(Level level, BlockPos mainPos, BlockState structureBlock) {
        BlockPos bottom = mainPos;
        for (int guard = 0; guard < MAX_STRUCTURE_HEIGHT; guard++) {
            BlockState below = level.getBlockState(bottom.below());
            if (!below.is(structureBlock.getBlock())) {
                break;
            }
            bottom = bottom.below();
        }
        return bottom.below();
    }

    private static boolean isRunningBlazeBlender(BlockState state) {
        boolean blender = state.is(AddonBlocks.BLENDER.get()) || state.is(AddonBlocks.ELECTRIC_BLENDER.get());
        return blender && isBlazeHead(state) && isWorking(state);
    }

    private static boolean isHeatSource(BlockState state) {
        return isHeatingElectricBlender(state)
               || (state.is(ModBlocks.HEATER) && !state.getValue(HeaterBlock.OVERLOAD))
               || isLitBurningHeater(state);
    }

    private static boolean isHeatingElectricBlender(BlockState state) {
        return state.is(AddonBlocks.ELECTRIC_BLENDER.get())
               && state.hasProperty(ElectricBlenderBlock.HEATING)
               && state.getValue(ElectricBlenderBlock.HEATING);
    }

    private static boolean isLitBurningHeater(BlockState state) {
        return state.is(ModBlocks.BURNING_HEATER) && state.getValue(BurningHeaterBlock.LEVEL) == 2;
    }

    private static boolean isBlazeHead(BlockState state) {
        return state.hasProperty(BlenderBlock.HEAD)
               && state.getValue(BlenderBlock.HEAD) == BlenderBlock.BlenderHead.BLAZE;
    }

    private static boolean isWorking(BlockState state) {
        return state.hasProperty(BlenderBlock.WORKING) && state.getValue(BlenderBlock.WORKING);
    }
}
