package dev.anvilcraft.pigeonplus.util;

import com.mojang.datafixers.util.Pair;
import dev.anvilcraft.pigeonplus.init.AddonVaporizationSources;
import dev.anvilcraft.pigeonplus.block.NozzleBlock;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

public final class NozzleExhaustUtil {
    public static final int JET_TUBE_HEIGHT = 4;
    public static final int JET_RANGE_RADIUS = 1;
    public static final int JET_RANGE_HEIGHT = 12;
    public static final int JET_VISUAL_HEIGHT = 16;
    public static final int JET_DAMAGE_HEIGHT = 16;
    public static final int JET_OUTLET_OFFSET_Y = 5;
    public static final int NOZZLE_MAIN_OFFSET_Y = 3;
    public static final int PLASMA_CONSUME_AMOUNT = FluidType.BUCKET_VOLUME;
    public static final int PLASMA_CONSUME_INTERVAL = 20;

    private NozzleExhaustUtil() {
    }

    public static BlockPos getJetOutletPos(BlockPos cauldronMainPos, Direction facing) {
        return cauldronMainPos.relative(facing, JET_OUTLET_OFFSET_Y);
    }

    public static boolean canSustainJet(Level level, LargeCauldronBlockEntity cauldron) {
        Direction facing = getNozzleFacing(level, cauldron.getBlockPos());
        if (facing == null) {
            return false;
        }
        BlockPos nozzlePos = cauldron.getBlockPos().relative(facing, NOZZLE_MAIN_OFFSET_Y);
        return !isNozzlePowered(level, nozzlePos)
            && cauldron.isIgnited()
            && getJetPropellant(level, cauldron) != null;
    }

    public static @Nullable AddonVaporizationSources.JetPropellant getJetPropellant(
        Level level,
        LargeCauldronBlockEntity cauldron
    ) {
        AddonVaporizationSources.JetPropellant recent = AddonVaporizationSources.getRecentJetPropellant(
            level,
            cauldron.getBlockPos()
        );
        if (recent != null) {
            return recent;
        }
        return AddonVaporizationSources.getAvailableJetPropellant(cauldron);
    }

    public static LargeCauldronBlockEntity getStructuralCauldron(Level level, BlockPos nozzlePos) {
        StructuralJet jet = findStructuralJet(level, nozzlePos);
        return jet != null ? jet.cauldron() : null;
    }

    public static @Nullable Direction getStructuralFacing(Level level, BlockPos nozzlePos) {
        StructuralJet jet = findStructuralJet(level, nozzlePos);
        return jet != null ? jet.facing() : null;
    }

    public static @Nullable BlockPos getStructuralOutletPos(Level level, BlockPos nozzlePos) {
        StructuralJet jet = findStructuralJet(level, nozzlePos);
        return jet != null ? jet.outletPos() : null;
    }

    public static boolean isNozzleActive(Level level, BlockPos nozzlePos) {
        StructuralJet jet = findStructuralJet(level, nozzlePos);
        return jet != null
            && !isNozzlePowered(level, nozzlePos)
            && canSustainJet(level, jet.cauldron());
    }

    public static boolean isNozzlePowered(Level level, BlockPos nozzlePos) {
        Direction facing = getStructuralFacing(level, nozzlePos);
        if (facing == null) {
            return level.hasNeighborSignal(nozzlePos);
        }
        for (BlockPos pos : BlockPos.betweenClosed(nozzlePos.offset(-1, -1, -1), nozzlePos.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof NozzleBlock && state.getValue(NozzleBlock.FACING) == facing
                && level.hasNeighborSignal(pos)) {
                return true;
            }
        }
        return false;
    }

    public static NozzleRingTargets collectRingTargets(Level level, BlockPos nozzlePos) {
        StructuralJet jet = findStructuralJet(level, nozzlePos);
        if (jet == null || !isNozzleActive(level, nozzlePos)) {
            return new NozzleRingTargets(Set.of(), Set.of(), Set.of());
        }
        Direction facing = jet.facing();
        Set<BlockPos> noMagnetHeatablePoses = new HashSet<>();
        Set<BlockPos> magnetHeatablePoses = new HashSet<>();
        Set<BlockPos> magnetPoses = new HashSet<>();
        Direction[] plane = getPlaneDirections(facing);
        Direction firstAxis = plane[0];
        Direction secondAxis = plane[1];
        int effectiveHeight = getEffectiveJetLength(level, jet.outletPos(), facing, JET_RANGE_HEIGHT);
        for (int i = 0; i < effectiveHeight; i++) {
            BlockPos layerCenter = jet.outletPos().relative(facing, i);
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

    public static float getVisibleJetRenderLength(Level level, BlockPos jetPos, Direction facing, int length) {
        return getEffectiveJetLength(level, jetPos, facing, length);
    }

    public static int getEffectiveJetLength(Level level, BlockPos jetPos, Direction facing, int length) {
        BlockPos obstructionPos = getJetRenderObstructionPos(level, jetPos, facing, length);
        if (obstructionPos == null) {
            return length;
        }
        int offset = Math.abs(
            (obstructionPos.getX() - jetPos.getX()) * facing.getStepX()
                + (obstructionPos.getY() - jetPos.getY()) * facing.getStepY()
                + (obstructionPos.getZ() - jetPos.getZ()) * facing.getStepZ()
        );
        return Math.min(length, offset + 1);
    }

    public static @Nullable BlockPos getJetRenderObstructionPos(Level level, BlockPos jetPos, Direction facing, int length) {
        for (int offset = 0; offset <= length; offset++) {
            BlockPos pos = jetPos.relative(facing, offset);
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof NozzleBlock) && !state.getCollisionShape(level, pos).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    public static boolean isOutletAreaFullyBlocked(Level level, BlockPos outletPos, Direction facing) {
        BlockPos center = outletPos;
        Direction[] plane = getPlaneDirections(facing);
        Direction first = plane[0];
        Direction second = plane[1];
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos pos = center.relative(first, i).relative(second, j);
                BlockState state = level.getBlockState(pos);
                if (state.getCollisionShape(level, pos).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isInJetCenterLine(Entity entity, BlockPos jetPos, Direction facing) {
        Vec3 center = entity.getBoundingBox().getCenter();
        double x = jetPos.getX() + 0.5;
        double y = jetPos.getY() + 0.5;
        double z = jetPos.getZ() + 0.5;
        double tolerance = 0.5 + Math.max(entity.getBbWidth(), 0.1) * 0.5;
        return switch (facing.getAxis()) {
            case X -> Math.abs(center.y - y) <= tolerance && Math.abs(center.z - z) <= tolerance;
            case Y -> Math.abs(center.x - x) <= tolerance && Math.abs(center.z - z) <= tolerance;
            case Z -> Math.abs(center.x - x) <= tolerance && Math.abs(center.y - y) <= tolerance;
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

    public static boolean consumeTopFuelOnce(
        LargeCauldronBlockEntity cauldron,
        AddonVaporizationSources.JetPropellant propellant
    ) {
        LargeCauldronFluidHandler handler = cauldron.getFluids();
        java.util.List<FluidStack> fluids = handler.copyFluids();
        for (int i = fluids.size() - 1; i >= 0; i--) {
            FluidStack stack = fluids.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!matchesFuel(stack, propellant)) {
                continue;
            }
            int consumeAmount = Math.min(stack.getAmount(), PLASMA_CONSUME_AMOUNT);
            int remaining = stack.getAmount() - consumeAmount;
            fluids.set(i, remaining > 0 ? stack.copyWithAmount(remaining) : FluidStack.EMPTY);
            handler.setFluids(fluids);
            if (!hasPropellant(cauldron, propellant)) {
                cauldron.setIgnited(false);
            }
            return true;
        }
        return false;
    }

    private static boolean matchesFuel(FluidStack stack, AddonVaporizationSources.JetPropellant propellant) {
        return switch (propellant) {
            case KEROSENE -> stack.is(ModFluidTags.OIL);
            case METHANE -> stack.getFluid().isSame(AddonFluids.LIQUEFIED_BIOGAS.get());
        };
    }

    private static boolean hasPropellant(
        LargeCauldronBlockEntity cauldron,
        AddonVaporizationSources.JetPropellant propellant
    ) {
        return switch (propellant) {
            case KEROSENE -> AddonVaporizationSources.hasMixedPropellant(cauldron);
            case METHANE -> AddonVaporizationSources.hasMethanePropellant(cauldron);
        };
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

    private static @Nullable StructuralJet findStructuralJet(Level level, BlockPos nozzlePos) {
        BlockState state = level.getBlockState(nozzlePos);
        if (!(state.getBlock() instanceof NozzleBlock nozzle) || !nozzle.isMainPart(state)) {
            return null;
        }
        Direction facing = state.getValue(NozzleBlock.FACING);
        BlockPos cauldronMainPos = nozzlePos.relative(facing.getOpposite(), NOZZLE_MAIN_OFFSET_Y);
        if (!(level.getBlockEntity(cauldronMainPos) instanceof LargeCauldronBlockEntity cauldron) || !cauldron.isMainPart()) {
            return null;
        }
        Direction nozzleFacing = getNozzleFacing(level, cauldronMainPos);
        if (nozzleFacing != facing) {
            return null;
        }
        return new StructuralJet(cauldron, facing, getJetOutletPos(cauldronMainPos, facing));
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

    private record StructuralJet(LargeCauldronBlockEntity cauldron, Direction facing, BlockPos outletPos) {
    }
}
