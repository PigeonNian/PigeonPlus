package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.block.AnvilPumpBlock;
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

/**
 * 让铁砧泵（{@link AnvilPumpBlock}）接入本体的流体管网扫描。
 */
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
        Map<IFluidHandler, FluidEndpoint> endpoints
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
        Map<IFluidHandler, FluidEndpoint> endpoints
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
        Map<IFluidHandler, FluidEndpoint> endpoints
    ) {
    }

    @Shadow
    private static int pumpHalfLift(Level level, BlockPos pumpPos) {
        return 0;
    }

    @Shadow
    public static boolean isPipePart(BlockState state) {
        return false;
    }

    @Inject(method = "isPipePart", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$anvilPumpIsPipePart(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof AnvilPumpBlock) {
            cir.setReturnValue(true);
        }
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

    @Inject(method = "pumpHalfLift", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$useAnvilPumpHeadlift(
        Level level,
        BlockPos pumpPos,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (level.getBlockState(pumpPos).getBlock() instanceof AnvilPumpBlock
            && level.getBlockEntity(pumpPos) instanceof dev.anvilcraft.pigeonplus.block.entity.AnvilPumpBlockEntity pump) {
            cir.setReturnValue(pump.getCurrentHeadlift());
        }
    }

    /**
     * 以注入方式完整接管扫描：复制本体的扫描流程并补上铁砧泵识别；
     * 其余部件全部走本体方法，避免破坏性 Overwrite。
     */
    @Inject(method = "scan", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$scan(
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
        Set<BlockPos> glassPipes = new HashSet<>();
        Map<IFluidHandler, FluidEndpoint> endpoints = new LinkedHashMap<>();
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
                    potential, adjacency, queue, endpoints);
            } else if (state.getBlock() instanceof PumpBlock || state.getBlock() instanceof AnvilPumpBlock) {
                // 本体只把工作中的泵记为二极管：扬程为 0（关闭/断电/未蓄压）时它只是普通连通管段。
                // 二极管的进液侧与提升势场的一侧一致，故两者都取"输出侧"。
                Direction outputDir = pigeonplus$outputDirection(state);
                int lift = pumpHalfLift(level, pos);
                if (lift > 0) {
                    diodes.put(pos.immutable(), outputDir);
                }
                // 沿输出侧 +扬程、反侧 -扬程展开两端
                for (Direction side : new Direction[]{outputDir, outputDir.getOpposite()}) {
                    int neighborPhi = phi + (side == outputDir ? lift : -lift);
                    visitNeighborWithPhi(level, pos, side, neighborPhi, potential, adjacency, queue, endpoints);
                }
            } else if (state.getBlock() instanceof PipeBlock pipe) {
                if (pipe.isGlassPipe()) {
                    glassPipes.add(pos.immutable());
                }
                // 记录管道面止逆阀约束（若有）
                if (state.getValue(PipeBlock.HAS_CHECK_VALVE)
                    && level.getBlockEntity(pos) instanceof AbstractPipeCheckValveBlockEntity cv
                    && !cv.isEmpty()) {
                    faceFlow.put(pos.immutable(), new EnumMap<>(cv.effectiveFlows()));
                }
                expandPipe(level, pos, state, phi, potential, adjacency, queue, endpoints);
            }
        }

        cir.setReturnValue(new FluidPipeNetwork(
            level,
            potential.keySet(),
            adjacency,
            valves,
            diodes,
            faceFlow,
            glassPipes,
            new ArrayList<>(endpoints.values())
        ));
    }

    /**
     * 泵的输出（+扬程）方向：铁砧泵用 {@code FACING}，本体泵用 {@code ORIENTATION}。
     */
    @Unique
    private static Direction pigeonplus$outputDirection(BlockState state) {
        return state.getBlock() instanceof AnvilPumpBlock
            ? AnvilPumpBlock.getOutputDirection(state)
            : state.getValue(PumpBlock.ORIENTATION).getDirection();
    }

    /**
     * 修复：本体 enqueuePump 无条件读取 {@code PumpBlock.ORIENTATION}，
     * 而 {@link AnvilPumpBlock} 只有 {@code FACING} 属性，扫描含铁砧泵的流体网络时服务端崩溃。
     * 遇到铁砧泵时用 {@code getOutputDirection} 复刻本体的势场计算与入队逻辑并取消原方法。
     */
    @Inject(method = "enqueuePump", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$enqueueAnvilPump(
        Level level,
        BlockPos pumpPos,
        BlockState state,
        BlockPos fromPos,
        Map<BlockPos, Integer> potential,
        Deque<BlockPos> queue,
        CallbackInfo ci
    ) {
        if (!(state.getBlock() instanceof AnvilPumpBlock)) {
            return;
        }

        Direction outputDir = AnvilPumpBlock.getOutputDirection(state);
        Integer fromPhi = potential.get(fromPos);
        if (fromPhi == null) {
            return;
        }
        int lift = pumpHalfLift(level, pumpPos);
        int pumpPhi;
        if (fromPos.equals(pumpPos.relative(outputDir))) {
            pumpPhi = fromPhi - lift;      // fromPos 在输出侧：fromPhi = pumpPhi + lift
        } else if (fromPos.equals(pumpPos.relative(outputDir.getOpposite()))) {
            pumpPhi = fromPhi + lift;      // fromPos 在输入侧：fromPhi = pumpPhi - lift
        } else {
            ci.cancel();
            return;
        }

        Integer old = potential.get(pumpPos);
        if (old == null) {
            potential.put(pumpPos.immutable(), pumpPhi);
            queue.add(pumpPos.immutable());
        } else if (pumpPhi < old) {
            potential.put(pumpPos.immutable(), pumpPhi);
        }
        ci.cancel();
    }
}
