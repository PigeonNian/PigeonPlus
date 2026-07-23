package dev.anvilcraft.pigeonplus.util;

import com.mojang.datafixers.util.Pair;
import dev.anvilcraft.pigeonplus.init.AddonVaporizationSources;
import dev.anvilcraft.pigeonplus.block.NozzleBlock;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

public final class NozzlePlasmaJetUtil {
    public static final int JET_TUBE_HEIGHT = 4;
    public static final int JET_RANGE_RADIUS = 1;
    public static final int JET_RANGE_HEIGHT = 12;
    public static final int JET_VISUAL_HEIGHT = 16;
    public static final int JET_DAMAGE_HEIGHT = 16;
    public static final int JET_OUTLET_OFFSET_Y = 5;
    public static final int NOZZLE_MAIN_OFFSET_Y = 3;
    public static final int PLASMA_CONSUME_AMOUNT = FluidType.BUCKET_VOLUME;
    public static final int PLASMA_CONSUME_INTERVAL = 20;

    private NozzlePlasmaJetUtil() {
    }

    public static BlockPos getJetOutletPos(BlockPos cauldronMainPos, Direction facing) {
        return cauldronMainPos.relative(facing, JET_OUTLET_OFFSET_Y);
    }

    public static boolean trySpawn(Level level, LargeCauldronBlockEntity cauldron) {
        Direction facing = getNozzleFacing(level, cauldron.getBlockPos());
        if (facing == null || !canSustainJet(level, cauldron)) {
            return false;
        }
        BlockPos outletPos = getJetOutletPos(cauldron.getBlockPos(), facing);
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
        return getNozzleFacing(level, cauldron.getBlockPos()) != null
            && cauldron.isIgnited()
            && AddonVaporizationSources.wasCrudeOilVaporizedRecently(level, cauldron.getBlockPos());
    }

    public static LargeCauldronBlockEntity getStructuralCauldron(Level level, BlockPos jetPos) {
        StructuralJet jet = findStructuralJet(level, jetPos);
        return jet != null ? jet.cauldron() : null;
    }

    public static @Nullable Direction getStructuralFacing(Level level, BlockPos jetPos) {
        StructuralJet jet = findStructuralJet(level, jetPos);
        return jet != null ? jet.facing() : null;
    }

    public static boolean isNozzleJetActive(Level level, BlockPos jetPos) {
        StructuralJet jet = findStructuralJet(level, jetPos);
        return jet != null && canSustainJet(level, jet.cauldron());
    }

    public static void seedTubeWalls(Set<PlasmaJetsBlockEntity.TubeWallLayer> tubeWalls, BlockPos jetPos, Direction facing) {
        if (!tubeWalls.isEmpty()) {
            return;
        }
        for (int i = 1; i <= JET_TUBE_HEIGHT; i++) {
            tubeWalls.add(PlasmaJetsBlockEntity.TubeWallLayer.of(jetPos.relative(facing.getOpposite(), i)));
        }
    }

    public static NozzleRingTargets collectRingTargets(Level level, BlockPos jetPos) {
        Direction facing = getStructuralFacing(level, jetPos);
        if (facing == null) {
            return new NozzleRingTargets(Set.of(), Set.of(), Set.of());
        }
        Set<BlockPos> noMagnetHeatablePoses = new HashSet<>();
        Set<BlockPos> magnetHeatablePoses = new HashSet<>();
        Set<BlockPos> magnetPoses = new HashSet<>();
        Direction[] plane = getPlaneDirections(facing);
        Direction firstAxis = plane[0];
        Direction secondAxis = plane[1];
        for (int i = 0; i < JET_RANGE_HEIGHT; i++) {
            BlockPos layerCenter = jetPos.relative(facing, i);
            BlockPos[] firstFace = createFace(layerCenter, firstAxis, secondAxis);
            BlockPos[] oppositeFirstFace = createFace(layerCenter, firstAxis.getOpposite(), secondAxis);
            BlockPos[] secondFace = createFace(layerCenter, secondAxis, firstAxis);
            BlockPos[] oppositeSecondFace = createFace(layerCenter, secondAxis.getOpposite(), firstAxis);

            if (matchesHeatablePair(level, firstFace, oppositeFirstFace)
                && matchesMagnetPair(level, secondFace, oppositeSecondFace)) {
                addFace(magnetHeatablePoses, firstFace);
                addFace(magnetHeatablePoses, oppositeFirstFace);
                addFace(magnetPoses, secondFace);
                addFace(magnetPoses, oppositeSecondFace);
                continue;
            }

            if (matchesHeatablePair(level, secondFace, oppositeSecondFace)
                && matchesMagnetPair(level, firstFace, oppositeFirstFace)) {
                addFace(magnetHeatablePoses, secondFace);
                addFace(magnetHeatablePoses, oppositeSecondFace);
                addFace(magnetPoses, firstFace);
                addFace(magnetPoses, oppositeFirstFace);
                continue;
            }

            if (matchesHeatablePair(level, firstFace, oppositeFirstFace)
                && matchesHeatablePair(level, secondFace, oppositeSecondFace)) {
                addFace(noMagnetHeatablePoses, firstFace);
                addFace(noMagnetHeatablePoses, oppositeFirstFace);
                addFace(noMagnetHeatablePoses, secondFace);
                addFace(noMagnetHeatablePoses, oppositeSecondFace);
            }
        }
        return new NozzleRingTargets(noMagnetHeatablePoses, magnetHeatablePoses, magnetPoses);
    }

    public static AABB getJetEffectBounds(BlockPos jetPos, Direction facing, int length) {
        return switch (facing) {
            case UP -> new AABB(
                jetPos.getX() - JET_RANGE_RADIUS,
                jetPos.getY(),
                jetPos.getZ() - JET_RANGE_RADIUS,
                jetPos.getX() + JET_RANGE_RADIUS + 2,
                jetPos.getY() + length,
                jetPos.getZ() + JET_RANGE_RADIUS + 2
            );
            case DOWN -> new AABB(
                jetPos.getX() - JET_RANGE_RADIUS,
                jetPos.getY() - length + 1,
                jetPos.getZ() - JET_RANGE_RADIUS,
                jetPos.getX() + JET_RANGE_RADIUS + 2,
                jetPos.getY() + 1,
                jetPos.getZ() + JET_RANGE_RADIUS + 2
            );
            case SOUTH -> new AABB(
                jetPos.getX() - JET_RANGE_RADIUS,
                jetPos.getY() - JET_RANGE_RADIUS,
                jetPos.getZ(),
                jetPos.getX() + JET_RANGE_RADIUS + 2,
                jetPos.getY() + JET_RANGE_RADIUS + 2,
                jetPos.getZ() + length
            );
            case NORTH -> new AABB(
                jetPos.getX() - JET_RANGE_RADIUS,
                jetPos.getY() - JET_RANGE_RADIUS,
                jetPos.getZ() - length + 1,
                jetPos.getX() + JET_RANGE_RADIUS + 2,
                jetPos.getY() + JET_RANGE_RADIUS + 2,
                jetPos.getZ() + 1
            );
            case EAST -> new AABB(
                jetPos.getX(),
                jetPos.getY() - JET_RANGE_RADIUS,
                jetPos.getZ() - JET_RANGE_RADIUS,
                jetPos.getX() + length,
                jetPos.getY() + JET_RANGE_RADIUS + 2,
                jetPos.getZ() + JET_RANGE_RADIUS + 2
            );
            case WEST -> new AABB(
                jetPos.getX() - length + 1,
                jetPos.getY() - JET_RANGE_RADIUS,
                jetPos.getZ() - JET_RANGE_RADIUS,
                jetPos.getX() + 1,
                jetPos.getY() + JET_RANGE_RADIUS + 2,
                jetPos.getZ() + JET_RANGE_RADIUS + 2
            );
        };
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
                continue;
            }
            int consumeAmount = Math.min(stack.getAmount(), PLASMA_CONSUME_AMOUNT);
            int remaining = stack.getAmount() - consumeAmount;
            fluids.set(i, remaining > 0 ? stack.copyWithAmount(remaining) : FluidStack.EMPTY);
            handler.setFluids(fluids);
            if (!AddonVaporizationSources.hasMixedPropellant(cauldron)) {
                cauldron.setIgnited(false);
            }
            return true;
        }
        return false;
    }

    public static @Nullable Direction getNozzleFacing(Level level, BlockPos cauldronMainPos) {
        for (Direction facing : Direction.values()) {
            BlockPos nozzleMainPos = cauldronMainPos.relative(facing, NOZZLE_MAIN_OFFSET_Y);
            BlockState state = level.getBlockState(nozzleMainPos);
            if (state.getBlock() instanceof NozzleBlock nozzle
                && nozzle.isMainPart(state)
                && state.getValue(NozzleBlock.FACING) == facing) {
                return facing;
            }
        }
        return null;
    }

    private static @Nullable StructuralJet findStructuralJet(Level level, BlockPos jetPos) {
        for (Direction facing : Direction.values()) {
            BlockPos cauldronMainPos = jetPos.relative(facing.getOpposite(), JET_OUTLET_OFFSET_Y);
            if (!(level.getBlockEntity(cauldronMainPos) instanceof LargeCauldronBlockEntity cauldron) || !cauldron.isMainPart()) {
                continue;
            }
            Direction nozzleFacing = getNozzleFacing(level, cauldronMainPos);
            if (nozzleFacing == facing && getJetOutletPos(cauldronMainPos, facing).equals(jetPos)) {
                return new StructuralJet(cauldron, facing);
            }
        }
        return null;
    }

    private static Direction[] getPlaneDirections(Direction facing) {
        return switch (facing.getAxis()) {
            case Y -> new Direction[] {Direction.NORTH, Direction.EAST};
            case X -> new Direction[] {Direction.UP, Direction.NORTH};
            case Z -> new Direction[] {Direction.UP, Direction.EAST};
        };
    }

    private static BlockPos[] createFace(BlockPos center, Direction faceDirection, Direction spanDirection) {
        return new BlockPos[] {
            center.relative(faceDirection, 2).relative(spanDirection.getOpposite()),
            center.relative(faceDirection, 2),
            center.relative(faceDirection, 2).relative(spanDirection)
        };
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

    private record StructuralJet(LargeCauldronBlockEntity cauldron, Direction facing) {
    }
}
