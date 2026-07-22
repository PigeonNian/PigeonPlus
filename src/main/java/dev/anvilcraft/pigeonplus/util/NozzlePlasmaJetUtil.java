package dev.anvilcraft.pigeonplus.util;

import com.mojang.datafixers.util.Pair;
import dev.anvilcraft.pigeonplus.block.NozzleBlock;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import java.util.HashSet;
import java.util.Set;

public final class NozzlePlasmaJetUtil {
    public static final int JET_TUBE_HEIGHT = 4;
    public static final int JET_RANGE_RADIUS = 1;
    public static final int JET_RANGE_HEIGHT = 9;
    public static final int JET_OUTLET_OFFSET_Y = 5;
    public static final int NOZZLE_MAIN_OFFSET_Y = 3;
    public static final int PLASMA_CONSUME_AMOUNT = FluidType.BUCKET_VOLUME;
    public static final int PLASMA_CONSUME_INTERVAL = 20;

    private NozzlePlasmaJetUtil() {
    }

    public static BlockPos getJetOutletPos(BlockPos cauldronMainPos) {
        return cauldronMainPos.above(JET_OUTLET_OFFSET_Y);
    }

    public static boolean trySpawn(Level level, LargeCauldronBlockEntity cauldron) {
        if (!canSustainJet(level, cauldron)) {
            return false;
        }
        BlockPos outletPos = getJetOutletPos(cauldron.getBlockPos());
        BlockState outletState = level.getBlockState(outletPos);
        if (outletState.is(ModBlocks.PLASMA_JETS)) {
            return true;
        }
        if (!outletState.isAir()) {
            return false;
        }
        return level.setBlock(outletPos, ModBlocks.PLASMA_JETS.getDefaultState(), 3);
    }

    public static boolean canSustainJet(Level level, LargeCauldronBlockEntity cauldron) {
        return hasUpwardNozzle(level, cauldron.getBlockPos())
            && cauldron.isIgnited()
            && isTopFluidOil(cauldron)
            && hasActiveHeaterBelow(level, cauldron.getBlockPos());
    }

    public static LargeCauldronBlockEntity getStructuralCauldron(Level level, BlockPos jetPos) {
        BlockPos cauldronMainPos = jetPos.below(JET_OUTLET_OFFSET_Y);
        if (!(level.getBlockEntity(cauldronMainPos) instanceof LargeCauldronBlockEntity cauldron) || !cauldron.isMainPart()) {
            return null;
        }
        return hasUpwardNozzle(level, cauldronMainPos) && getJetOutletPos(cauldronMainPos).equals(jetPos)
            ? cauldron
            : null;
    }

    public static void seedTubeWalls(Set<PlasmaJetsBlockEntity.TubeWallLayer> tubeWalls, BlockPos jetPos) {
        if (!tubeWalls.isEmpty()) {
            return;
        }
        for (int i = 1; i <= JET_TUBE_HEIGHT; i++) {
            tubeWalls.add(PlasmaJetsBlockEntity.TubeWallLayer.of(jetPos.below(i)));
        }
    }

    public static NozzleRingTargets collectRingTargets(Level level, BlockPos jetPos) {
        Set<BlockPos> noMagnetHeatablePoses = new HashSet<>();
        Set<BlockPos> magnetHeatablePoses = new HashSet<>();
        Set<BlockPos> magnetPoses = new HashSet<>();
        for (int i = 0; i < JET_RANGE_HEIGHT; i++) {
            BlockPos layerCenter = jetPos.above(i);
            BlockPos[] northFace = new BlockPos[] {
                layerCenter.north(2).west(),
                layerCenter.north(2),
                layerCenter.north(2).east()
            };
            BlockPos[] southFace = new BlockPos[] {
                layerCenter.south(2).west(),
                layerCenter.south(2),
                layerCenter.south(2).east()
            };
            BlockPos[] eastFace = new BlockPos[] {
                layerCenter.east(2).north(),
                layerCenter.east(2),
                layerCenter.east(2).south()
            };
            BlockPos[] westFace = new BlockPos[] {
                layerCenter.west(2).north(),
                layerCenter.west(2),
                layerCenter.west(2).south()
            };

            if (matchesHeatablePair(level, northFace, southFace) && matchesMagnetPair(level, eastFace, westFace)) {
                addFace(magnetHeatablePoses, northFace);
                addFace(magnetHeatablePoses, southFace);
                addFace(magnetPoses, eastFace);
                addFace(magnetPoses, westFace);
                continue;
            }

            if (matchesHeatablePair(level, eastFace, westFace) && matchesMagnetPair(level, northFace, southFace)) {
                addFace(magnetHeatablePoses, eastFace);
                addFace(magnetHeatablePoses, westFace);
                addFace(magnetPoses, northFace);
                addFace(magnetPoses, southFace);
                continue;
            }

            if (matchesHeatablePair(level, northFace, southFace) && matchesHeatablePair(level, eastFace, westFace)) {
                addFace(noMagnetHeatablePoses, northFace);
                addFace(noMagnetHeatablePoses, southFace);
                addFace(noMagnetHeatablePoses, eastFace);
                addFace(noMagnetHeatablePoses, westFace);
            }
        }
        return new NozzleRingTargets(noMagnetHeatablePoses, magnetHeatablePoses, magnetPoses);
    }

    private static boolean matchesHeatablePair(Level level, BlockPos[] firstFace, BlockPos[] secondFace) {
        return matchesFacePair(level, firstFace, secondFace, ModBlockTags.HEATABLE_BLOCKS);
    }

    private static boolean matchesMagnetPair(Level level, BlockPos[] firstFace, BlockPos[] secondFace) {
        return matchesFacePair(level, firstFace, secondFace, ModBlockTags.MAGNET);
    }

    private static boolean matchesFacePair(Level level, BlockPos[] firstFace, BlockPos[] secondFace, net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tag) {
        if (firstFace.length != secondFace.length || firstFace.length == 0) {
            return false;
        }
        BlockState firstState = level.getBlockState(firstFace[0]);
        BlockState secondState = level.getBlockState(secondFace[0]);
        if (!firstState.is(tag) || !secondState.is(tag) || firstState.getBlock() != secondState.getBlock()) {
            return false;
        }
        for (int i = 0; i < firstFace.length; i++) {
            BlockState leftState = level.getBlockState(firstFace[i]);
            BlockState rightState = level.getBlockState(secondFace[i]);
            if (!leftState.is(tag) || !rightState.is(tag) || leftState.getBlock() != firstState.getBlock() || rightState.getBlock() != secondState.getBlock()) {
                return false;
            }
        }
        return true;
    }

    private static void addFace(Set<BlockPos> poses, BlockPos[] face) {
        for (BlockPos pos : face) {
            poses.add(pos);
        }
    }

    public static boolean consumeTopOilOnce(LargeCauldronBlockEntity cauldron) {
        LargeCauldronFluidHandler handler = cauldron.getFluids();
        java.util.List<FluidStack> fluids = handler.copyFluids();
        for (int i = fluids.size() - 1; i >= 0; i--) {
            FluidStack stack = fluids.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!stack.is(ModFluidTags.OIL)) {
                return false;
            }
            int consumeAmount = Math.min(stack.getAmount(), PLASMA_CONSUME_AMOUNT);
            int remaining = stack.getAmount() - consumeAmount;
            fluids.set(i, remaining > 0 ? stack.copyWithAmount(remaining) : FluidStack.EMPTY);
            handler.setFluids(fluids);
            if (!isTopFluidOil(cauldron)) {
                cauldron.setIgnited(false);
            }
            return true;
        }
        return false;
    }

    private static boolean hasUpwardNozzle(Level level, BlockPos cauldronMainPos) {
        BlockPos nozzleMainPos = cauldronMainPos.above(NOZZLE_MAIN_OFFSET_Y);
        BlockState state = level.getBlockState(nozzleMainPos);
        return state.getBlock() instanceof NozzleBlock nozzle
            && nozzle.isMainPart(state)
            && state.getValue(NozzleBlock.FACING) == Direction.UP;
    }

    private static boolean isTopFluidOil(LargeCauldronBlockEntity cauldron) {
        return cauldron.getTopFluid().is(ModFluidTags.OIL);
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

    public record NozzleRingTargets(
        Set<BlockPos> noMagnetHeatablePoses,
        Set<BlockPos> magnetHeatablePoses,
        Set<BlockPos> magnetPoses
    ) {
        public Pair<Set<BlockPos>, Set<BlockPos>> toHeatingPoses() {
            return Pair.of(this.noMagnetHeatablePoses, this.magnetHeatablePoses);
        }
    }
}
