package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.block.AnvilPumpBlock;
import dev.anvilcraft.pigeonplus.block.entity.AnvilPumpBlockEntity;
import dev.dubhe.anvilcraft.api.fluid.network.FluidEndpoint;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkScanner;
import dev.dubhe.anvilcraft.api.fluid.network.FluidPipeNetwork;
import dev.dubhe.anvilcraft.api.fluid.network.ValveState;
import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.PipeCheckValveBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.ControlValveBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeNodeBlock;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(FluidNetworkScanner.class)
public abstract class FluidNetworkScannerMixin {
    @Shadow
    private static void expandAxial(
        Level level,
        BlockPos pos,
        Direction.Axis axis,
        int phi,
        boolean unusedFlag,
        Map<BlockPos, Integer> potential,
        Map<BlockPos, List<BlockPos>> adjacency,
        Deque<BlockPos> queue,
        Map<BlockPos, FluidEndpoint> endpoints,
        Set<IFluidHandler> seenHandlers
    ) {
    }

    @Shadow
    private static void expandPipe(
        Level level,
        BlockPos pos,
        BlockState state,
        int phi,
        Map<BlockPos, Integer> potential,
        Map<BlockPos, List<BlockPos>> adjacency,
        Deque<BlockPos> queue,
        Map<BlockPos, FluidEndpoint> endpoints,
        Set<IFluidHandler> seenHandlers
    ) {
    }

    @Shadow
    private static void visitNeighborWithPhi(
        Level level,
        BlockPos pos,
        Direction dir,
        int neighborPhi,
        Map<BlockPos, Integer> potential,
        Map<BlockPos, List<BlockPos>> adjacency,
        Deque<BlockPos> queue,
        Map<BlockPos, FluidEndpoint> endpoints,
        Set<IFluidHandler> seenHandlers
    ) {
    }

    @Shadow
    private static int pumpHalfLift(Level level, BlockPos pumpPos) {
        return 0;
    }

    @Overwrite
    public static boolean isPipePart(BlockState state) {
        return state.getBlock() instanceof PipeBlock
            || state.getBlock() instanceof PumpBlock
            || state.getBlock() instanceof AnvilPumpBlock
            || state.getBlock() instanceof ControlValveBlock;
    }

    @Overwrite
    private static boolean isConnectablePump(BlockState state, Direction faceToPump) {
        if (state.getBlock() instanceof AnvilPumpBlock) {
            return AnvilPumpBlock.isConnectableFace(state, faceToPump);
        }
        return state.getBlock() instanceof PumpBlock && PumpBlock.isConnectableFace(state, faceToPump);
    }

    @Overwrite
    public static FluidPipeNetwork scan(Level level, BlockPos seed) {
        if (!isPipePart(level.getBlockState(seed))) {
            return null;
        }

        Map<BlockPos, Integer> potential = new HashMap<>();
        Map<BlockPos, List<BlockPos>> adjacency = new HashMap<>();
        Map<BlockPos, ValveState> valves = new HashMap<>();
        Map<BlockPos, Direction> diodes = new HashMap<>();
        Map<BlockPos, Map<Direction, Direction>> faceFlow = new HashMap<>();
        Map<BlockPos, FluidEndpoint> endpoints = new LinkedHashMap<>();
        Set<IFluidHandler> seenHandlers = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        potential.put(seed, 0);
        queue.add(seed);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int phi = potential.get(pos);
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof ControlValveBlock
                && level.getBlockEntity(pos) instanceof ControlValveBlockEntity valveBe) {
                valves.putIfAbsent(pos, new ValveState(valveBe));
                expandAxial(level, pos, state.getValue(ControlValveBlock.AXIS), phi, false,
                    potential, adjacency, queue, endpoints, seenHandlers);
            } else if (state.getBlock() instanceof PumpBlock) {
                diodes.put(pos.immutable(), state.getValue(PumpBlock.ORIENTATION).getDirection());
                expandPump(level, pos, state, phi, potential, adjacency, queue, endpoints, seenHandlers);
            } else if (state.getBlock() instanceof AnvilPumpBlock) {
                diodes.put(pos.immutable(), AnvilPumpBlock.getOutputDirection(state));
                expandPump(level, pos, state, phi, potential, adjacency, queue, endpoints, seenHandlers);
            } else if (state.getBlock() instanceof PipeBlock) {
                if (state.getValue(PipeBlock.HAS_CHECK_VALVE)
                    && level.getBlockEntity(pos) instanceof PipeCheckValveBlockEntity cv
                    && !cv.isEmpty()) {
                    faceFlow.put(pos.immutable(), new EnumMap<>(cv.effectiveFlows()));
                }
                expandPipe(level, pos, state, phi, potential, adjacency, queue, endpoints, seenHandlers);
            }
        }

        return new FluidPipeNetwork(
            level, potential.keySet(), adjacency, valves, diodes, faceFlow, new ArrayList<>(endpoints.values()));
    }

    @Overwrite
    private static void expandPump(
        Level level,
        BlockPos pos,
        BlockState state,
        int phi,
        Map<BlockPos, Integer> potential,
        Map<BlockPos, List<BlockPos>> adjacency,
        Deque<BlockPos> queue,
        Map<BlockPos, FluidEndpoint> endpoints,
        Set<IFluidHandler> seenHandlers
    ) {
        Direction outputDir = state.getBlock() instanceof AnvilPumpBlock
            ? AnvilPumpBlock.getOutputDirection(state)
            : state.getValue(PumpBlock.ORIENTATION).getDirection();
        int lift = pumpHalfLift(level, pos);
        for (Direction side : new Direction[]{outputDir, outputDir.getOpposite()}) {
            int neighborPhi = phi + (side == outputDir ? lift : -lift);
            visitNeighborWithPhi(level, pos, side, neighborPhi, potential, adjacency, queue, endpoints, seenHandlers);
        }
    }

    @Overwrite
    private static void enqueuePump(
        Level level,
        BlockPos pumpPos,
        BlockState pumpState,
        BlockPos fromPos,
        Map<BlockPos, Integer> potential,
        Deque<BlockPos> queue
    ) {
        Direction outputDir = pumpState.getBlock() instanceof AnvilPumpBlock
            ? AnvilPumpBlock.getOutputDirection(pumpState)
            : pumpState.getValue(PumpBlock.ORIENTATION).getDirection();
        int fromPhi = potential.get(fromPos);
        int lift = pumpHalfLift(level, pumpPos);
        int pumpPhi;
        if (fromPos.equals(pumpPos.relative(outputDir))) {
            pumpPhi = fromPhi - lift;
        } else if (fromPos.equals(pumpPos.relative(outputDir.getOpposite()))) {
            pumpPhi = fromPhi + lift;
        } else {
            return;
        }
        Integer old = potential.get(pumpPos);
        if (old != null) {
            if (pumpPhi < old) {
                potential.put(pumpPos.immutable(), pumpPhi);
            }
            return;
        }
        potential.put(pumpPos.immutable(), pumpPhi);
        queue.add(pumpPos.immutable());
    }

    @Inject(method = "pumpHalfLift", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$useAnvilPumpHeadlift(Level level, BlockPos pumpPos, CallbackInfoReturnable<Integer> cir) {
        if (level.getBlockEntity(pumpPos) instanceof AnvilPumpBlockEntity pump) {
            cir.setReturnValue(pump.getCurrentHeadlift());
        }
    }
}
