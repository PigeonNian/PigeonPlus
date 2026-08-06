package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.block.AnvilPumpBlock;
import dev.anvilcraft.pigeonplus.block.entity.AnvilPumpBlockEntity;
import dev.dubhe.anvilcraft.api.fluid.network.FluidEndpoint;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkScanner;
import dev.dubhe.anvilcraft.api.fluid.network.FluidPipeNetwork;
import dev.dubhe.anvilcraft.api.fluid.network.ValveState;
import dev.dubhe.anvilcraft.block.entity.fluid.AbstractPipeCheckValveBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.ControlValveBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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

    @Inject(method = "isPipePart", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$anvilPumpIsPipePart(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof AnvilPumpBlock) {
            cir.setReturnValue(true);
        }
    }

    @Shadow
    private static boolean isPipePart(BlockState state) {
        return false;
    }

    @Inject(method = "isConnectablePump", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$anvilPumpConnectable(
        BlockState state,
        Direction faceToPump,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (state.getBlock() instanceof AnvilPumpBlock) {
            cir.setReturnValue(AnvilPumpBlock.isConnectableFace(state, faceToPump));
        }
    }

    /**
     * 以注入方式完整接管扫描：复制本体的扫描流程，额外收集玻璃管位置、识别铁砧泵，
     * 其余（控制阀、止回阀、泵扬程）全部走本体的 expand 方法，避免破坏性 Overwrite。
     */
    @Inject(method = "scan", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$scanWithAnvilPumpAndGlass(
        Level level,
        BlockPos seed,
        CallbackInfoReturnable<FluidPipeNetwork> cir
    ) {
        if (!isPipePart(level.getBlockState(seed))) {
            cir.setReturnValue(null);
            return;
        }

        Map<BlockPos, Integer> potential = new HashMap<>();
        Map<BlockPos, List<BlockPos>> adjacency = new HashMap<>();
        Map<BlockPos, ValveState> valves = new HashMap<>();
        Map<BlockPos, Direction> diodes = new HashMap<>();
        Map<BlockPos, Map<Direction, Direction>> faceFlow = new HashMap<>();
        Map<BlockPos, FluidEndpoint> endpoints = new LinkedHashMap<>();
        Set<BlockPos> glassPipes = new HashSet<>();
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
            } else if (state.getBlock() instanceof PipeBlock pipe) {
                if (pipe.isGlassPipe()) {
                    glassPipes.add(pos.immutable());
                }
                if (state.getValue(PipeBlock.HAS_CHECK_VALVE)
                    && level.getBlockEntity(pos) instanceof AbstractPipeCheckValveBlockEntity cv
                    && !cv.isEmpty()) {
                    faceFlow.put(pos.immutable(), new EnumMap<>(cv.effectiveFlows()));
                }
                expandPipe(level, pos, state, phi, potential, adjacency, queue, endpoints, seenHandlers);
            }
        }

        cir.setReturnValue(new FluidPipeNetwork(
            level, potential.keySet(), adjacency, valves, diodes, faceFlow, glassPipes, new ArrayList<>(endpoints.values())));
    }

    /**
     * 展开泵的进出口两侧（铁砧泵与普通泵一致）：沿输出方向取扬程、沿反方向减扬程。
     */
    @Unique
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

    @Inject(method = "pumpHalfLift", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$useAnvilPumpHeadlift(Level level, BlockPos pumpPos, CallbackInfoReturnable<Integer> cir) {
        if (level.getBlockEntity(pumpPos) instanceof AnvilPumpBlockEntity pump) {
            cir.setReturnValue(pump.getCurrentHeadlift());
        }
    }
}
