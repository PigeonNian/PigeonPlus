package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.anvilcraft.pigeonplus.util.CompressedAirDrainFluidHandler;
import dev.anvilcraft.pigeonplus.util.GasLiquefactionTracker;
import dev.dubhe.anvilcraft.api.fluid.network.FluidEndpoint;
import dev.dubhe.anvilcraft.api.fluid.network.FluidPipeNetwork;
import dev.dubhe.anvilcraft.api.fluid.network.ValveState;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.GlassPipeBlockEntity;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(FluidPipeNetwork.class)
public abstract class FluidPipeNetworkMixin {
    @Getter
    @Shadow
    @Final
    private Level level;

    @Shadow
    @Final
    private Map<BlockPos, List<BlockPos>> adjacency;

    @Shadow
    @Final
    private Map<BlockPos, ValveState> valves;

    @Shadow
    @Final
    private Map<BlockPos, Direction> diodes;

    @Shadow
    @Final
    private Map<BlockPos, Map<Direction, Direction>> faceFlow;

    @Shadow
    @Final
    private List<FluidEndpoint> endpoints;

    @Shadow
    @Final
    private Set<BlockPos> glassPipePositions;

    @Shadow
    @Final
    private boolean directionalConstraints;

    @Shadow
    private void onTransferred(FluidEndpoint source) {
    }

    @Shadow
    private void showFluidOnPipe(GlassPipeBlockEntity pipe, BlockPos pos, FluidStack fluid, Set<Direction> directions) {
    }

    @Shadow
    private void showFluidBlockedAtSource(FluidEndpoint source) {
    }

    @Shadow
    private void showFluidToBlockedPart(FluidStack fluid, FluidEndpoint source, int tankIdx) {
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void pigeonplus$equalizeGasPressure(CallbackInfo ci) {
        Set<GasFluid> gases = pigeonplus$collectGases();
        for (GasFluid gas : gases) {
            pigeonplus$equalizeGas(gas);
        }
    }

    @Inject(method = "canTarget", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$skipGravityForGas(
        FluidEndpoint source,
        int tankIdx,
        FluidEndpoint target,
        FluidStack stored,
        @Coerce Object reach,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (stored.getFluid() instanceof GasFluid) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private Set<GasFluid> pigeonplus$collectGases() {
        Set<GasFluid> gases = new HashSet<>();
        for (FluidEndpoint endpoint : endpoints) {
            IFluidHandler handler = endpoint.handler();
            for (int i = 0; i < handler.getTanks(); i++) {
                if (handler.getFluidInTank(i).getFluid() instanceof GasFluid gas) {
                    gases.add(gas);
                }
            }
        }
        return gases;
    }

    @Unique
    private void pigeonplus$equalizeGas(GasFluid gas) {
        List<FluidEndpoint> sources = endpoints.stream()
            .filter(endpoint -> pigeonplus$gasAmount(endpoint, gas) > 0)
            .sorted(Comparator.comparingDouble((FluidEndpoint endpoint) -> pigeonplus$pressure(endpoint, gas)).reversed())
            .toList();
        for (FluidEndpoint source : sources) {
            pigeonplus$spreadFromPressureSource(source, gas);
        }
    }

    @Unique
    private void pigeonplus$spreadFromPressureSource(FluidEndpoint source, GasFluid gas) {
        if (!pigeonplus$canDrainFromEndpoint(source)) {
            return;
        }
        int sourceAmount = pigeonplus$gasAmount(source, gas);
        int sourceCapacity = pigeonplus$totalCapacity(source.handler());
        if (sourceAmount <= 0 || sourceCapacity <= 0) {
            return;
        }
        FluidStack gasStack = new FluidStack(gas, 1);
        PigeonPlus_Reachability reach = directionalConstraints ? pigeonplus$computeReachable(source.fromPipePos(), gasStack) : null;
        Map<BlockPos, List<ValveState>> pathValves = reach == null ? Map.of() : reach.pathValves();
        List<FluidEndpoint> targets = endpoints.stream()
            .filter(target -> pigeonplus$canPressureTarget(source, target, gas, gasStack, reach))
            .sorted(Comparator.comparingDouble(target -> pigeonplus$pressure(target, gas)))
            .toList();
        int budget = FluidPipeNetwork.MAX_SPEED;
        for (FluidEndpoint target : targets) {
            if (budget <= 0) {
                break;
            }
            int amount = pigeonplus$pressureTransferAmount(source, target, gas);
            if (amount <= 0) {
                pigeonplus$showGasBlockedAt(source, target, gasStack);
                continue;
            }
            List<ValveState> valvePath = pathValves.get(target.fromPipePos());
            amount = Math.min(amount, Math.min(budget, pigeonplus$minValveRemaining(valvePath)));
            if (amount <= 0) {
                pigeonplus$showGasBlockedAt(source, target, gasStack);
                continue;
            }
            int moved = pigeonplus$moveGas(source, target, gas, amount);
            if (moved <= 0) {
                continue;
            }
            budget -= moved;
            pigeonplus$deductValves(valvePath, moved);
            pigeonplus$showGasOnPath(gasStack, source, target, reach);
            onTransferred(source);
        }
    }

    /**
     * 气体被阀门挡住、无法到达目标时，复用本体的“被挡显示”机制，把气体亮到被挡位置为止。
     */
    @Unique
    private void pigeonplus$showGasBlockedAt(FluidEndpoint source, FluidEndpoint target, FluidStack gas) {
        if (level.isClientSide() || glassPipePositions.isEmpty()) {
            return;
        }
        if (pigeonplus$sameNetworkArea(source, target)) {
            showFluidToBlockedPart(gas, source, 0);
        }
    }

    /**
     * 粗略判断两个端点是否在同一网络（避免对跨网络端点错误触发被挡显示）。
     */
    @Unique
    private boolean pigeonplus$sameNetworkArea(FluidEndpoint source, FluidEndpoint target) {
        return source.fromPipePos().equals(target.fromPipePos())
            || adjacency.getOrDefault(source.fromPipePos(), List.of()).contains(target.fromPipePos())
            || pigeonplus$undirectedConnected(source.fromPipePos(), target.fromPipePos());
    }

    @Unique
    private boolean pigeonplus$undirectedConnected(BlockPos from, BlockPos to) {
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> stack = new ArrayDeque<>();
        seen.add(from);
        stack.push(from);
        while (!stack.isEmpty()) {
            BlockPos cur = stack.pop();
            if (cur.equals(to)) {
                return true;
            }
            for (BlockPos next : adjacency.getOrDefault(cur, List.of())) {
                if (seen.add(next)) {
                    stack.push(next);
                }
            }
        }
        return false;
    }

    /**
     * 气体在管道中移动时，把气体显示到路径上的玻璃管道（与本体液体显示机制一致）。
     * 路径复用传输时已经算好的可达性结果，保证渲染与实际流动、阀门约束完全一致。
     */
    @Unique
    private void pigeonplus$showGasOnPath(FluidStack gas, FluidEndpoint source, FluidEndpoint target, PigeonPlus_Reachability reach) {
        if (level.isClientSide() || glassPipePositions.isEmpty()) {
            return;
        }
        List<BlockPos> path;
        if (directionalConstraints && reach != null) {
            Map<BlockPos, BlockPos> cameFrom = reach.cameFrom();
            if (!cameFrom.containsKey(target.fromPipePos())) {
                return;
            }
            path = new ArrayList<>();
            BlockPos cur = target.fromPipePos();
            while (cur != null) {
                path.add(cur);
                cur = cameFrom.get(cur);
            }
            Collections.reverse(path);
        } else {
            path = pigeonplus$undirectedPipePath(source.fromPipePos(), target.fromPipePos());
        }
        if (path == null || path.size() < 2) {
            return;
        }
        Map<BlockPos, EnumSet<Direction>> directions = new HashMap<>();
        for (int i = 1; i < path.size(); i++) {
            pigeonplus$addPipePathDirections(directions, path.get(i - 1), path.get(i));
        }
        pigeonplus$addEndpointDisplayDirection(directions, source);
        pigeonplus$addEndpointDisplayDirection(directions, target);
        for (BlockPos pos : path) {
            if (!glassPipePositions.contains(pos)) {
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof GlassPipeBlockEntity pipe)) {
                continue;
            }
            showFluidOnPipe(pipe, pos, gas, directions.getOrDefault(pos, EnumSet.noneOf(Direction.class)));
        }
    }

    @Unique
    private List<BlockPos> pigeonplus$undirectedPipePath(BlockPos from, BlockPos to) {
        if (from.equals(to)) {
            return null;
        }
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        cameFrom.put(from, null);
        queue.add(from);
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            if (cur.equals(to)) {
                break;
            }
            for (BlockPos next : adjacency.getOrDefault(cur, List.of())) {
                if (cameFrom.containsKey(next)) {
                    continue;
                }
                cameFrom.put(next, cur);
                queue.add(next);
            }
        }
        if (!cameFrom.containsKey(to)) {
            return null;
        }
        List<BlockPos> path = new ArrayList<>();
        BlockPos cur = to;
        while (cur != null) {
            path.add(cur);
            cur = cameFrom.get(cur);
        }
        Collections.reverse(path);
        return path;
    }

    @Unique
    private static void pigeonplus$addPipePathDirections(Map<BlockPos, EnumSet<Direction>> dirs, BlockPos from, BlockPos to) {
        Direction direction = Direction.fromDelta(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
        if (direction == null) {
            return;
        }
        dirs.computeIfAbsent(from, k -> EnumSet.noneOf(Direction.class)).add(direction);
        dirs.computeIfAbsent(to, k -> EnumSet.noneOf(Direction.class)).add(direction.getOpposite());
    }

    @Unique
    private static void pigeonplus$addEndpointDisplayDirection(Map<BlockPos, EnumSet<Direction>> dirs, FluidEndpoint endpoint) {
        if (endpoint.sideToPipe() == null) {
            return;
        }
        dirs.computeIfAbsent(endpoint.fromPipePos(), k -> EnumSet.noneOf(Direction.class))
            .add(endpoint.sideToPipe().getOpposite());
    }

    @Unique
    private boolean pigeonplus$canPressureTarget(
        FluidEndpoint source,
        FluidEndpoint target,
        GasFluid gas,
        FluidStack gasStack,
        PigeonPlus_Reachability reach
    ) {
        if (target == source || target.handler().equals(source.handler())) {
            return false;
        }
        if (target.handler().fill(gasStack, IFluidHandler.FluidAction.SIMULATE) <= 0
            && !pigeonplus$canLiquefyGas(source, target, gas)) {
            return false;
        }
        return reach == null || pigeonplus$isEndpointReachable(reach, target);
    }

    @Unique
    private int pigeonplus$pressureTransferAmount(FluidEndpoint source, FluidEndpoint target, GasFluid gas) {
        int sourceAmount = pigeonplus$gasAmount(source, gas);
        int sourceCapacity = pigeonplus$totalCapacity(source.handler());
        int targetCapacity = pigeonplus$totalCapacity(target.handler());
        if (sourceAmount <= 0 || sourceCapacity <= 0 || targetCapacity <= 0) {
            return 0;
        }
        int targetAmount = pigeonplus$gasAmount(target, gas);
        long numerator = (long) sourceAmount * targetCapacity - (long) targetAmount * sourceCapacity;
        if (pigeonplus$isAirSource(source)) {
            numerator += (long) sourceCapacity * targetCapacity;
        }
        int pumpPressureDiff = pigeonplus$pumpPressureDiff(source, target);
        if (source.handler() instanceof CompressedAirDrainFluidHandler
            && gas.isSame(AddonFluids.COMPRESSED_AIR.get())
            && pumpPressureDiff <= 0) {
            return 0;
        }
        if (pumpPressureDiff > 0) {
            numerator += (long) sourceCapacity * targetCapacity * pumpPressureDiff / FluidPipeNetwork.FULL_SPEED_HEIGHT;
        }
        if (numerator <= 0) {
            return 0;
        }
        long denominator = (long) sourceCapacity + targetCapacity;
        int equalizingAmount = (int) Math.max(1L, numerator / denominator);
        return Math.min(equalizingAmount, FluidPipeNetwork.MAX_SPEED);
    }

    @Unique
    private static boolean pigeonplus$isAirSource(FluidEndpoint source) {
        return source.handler() instanceof CompressedAirDrainFluidHandler;
    }

    @Unique
    private static int pigeonplus$pumpPressureDiff(FluidEndpoint source, FluidEndpoint target) {
        int sourcePotential = source.effectiveHeight() - source.containerPos().getY();
        int targetPotential = target.effectiveHeight() - target.containerPos().getY();
        return Math.max(0, sourcePotential - targetPotential);
    }

    @Unique
    private int pigeonplus$moveGas(FluidEndpoint source, FluidEndpoint target, GasFluid gas, int amount) {
        FluidStack wanted = new FluidStack(gas, amount);
        int fillable = target.handler().fill(wanted, IFluidHandler.FluidAction.SIMULATE);
        if (fillable <= 0) {
            return pigeonplus$liquefyGas(source, target, gas, amount);
        }
        FluidStack drained = source.handler().drain(new FluidStack(gas, fillable), IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return 0;
        }
        int filled = target.handler().fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (filled < drained.getAmount()) {
            source.handler().fill(drained.copyWithAmount(drained.getAmount() - filled), IFluidHandler.FluidAction.EXECUTE);
        }
        return filled;
    }

    @Unique
    private boolean pigeonplus$canLiquefyGas(FluidEndpoint source, FluidEndpoint target, GasFluid gas) {
        Fluid liquefied = pigeonplus$liquefiedFluid(gas);
        if (liquefied == null || pigeonplus$pumpPressureDiff(source, target) <= 0) {
            GasLiquefactionTracker.clear(level, target.containerPos(), gas);
            return false;
        }
        if (!pigeonplus$isLargeTank(target)) {
            GasLiquefactionTracker.clear(level, target.containerPos(), gas);
            return false;
        }
        IFluidHandler handler = target.handler();
        int capacity = pigeonplus$totalCapacity(handler);
        int totalAmount = pigeonplus$totalAmount(handler);
        int gasAmount = pigeonplus$gasAmount(target, gas);
        if (capacity <= 0 || totalAmount < capacity || gasAmount <= 0) {
            GasLiquefactionTracker.clear(level, target.containerPos(), gas);
            return false;
        }
        return true;
    }

    @Unique
    private int pigeonplus$liquefyGas(FluidEndpoint source, FluidEndpoint target, GasFluid gas, int amount) {
        if (!pigeonplus$canLiquefyGas(source, target, gas) || amount <= 0) {
            return 0;
        }
        int sourceGas = pigeonplus$gasAmount(source, gas);
        int targetGas = pigeonplus$gasAmount(target, gas);
        int inputAmount = Math.min(amount, Math.min(sourceGas, targetGas));
        if (inputAmount <= 0) {
            return 0;
        }
        FluidStack drained = source.handler().drain(new FluidStack(gas, inputAmount), IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return 0;
        }

        int liquidAmount = GasLiquefactionTracker.addGasInput(
            level,
            target.containerPos(),
            gas,
            drained.getAmount(),
            pigeonplus$liquefactionRatio(gas)
        );
        if (liquidAmount > 0) {
            pigeonplus$replaceTargetGasWithLiquid(target, gas, liquidAmount);
        }
        return drained.getAmount();
    }

    @Unique
    private int pigeonplus$replaceTargetGasWithLiquid(FluidEndpoint target, GasFluid gas, int liquidAmount) {
        Fluid liquefied = pigeonplus$liquefiedFluid(gas);
        if (liquefied == null || liquidAmount <= 0) {
            return 0;
        }
        IFluidHandler handler = target.handler();
        FluidStack targetGas = new FluidStack(gas, liquidAmount);
        FluidStack drainedTargetGas = handler.drain(targetGas, IFluidHandler.FluidAction.EXECUTE);
        int actualLiquidAmount = drainedTargetGas.getAmount();
        if (actualLiquidAmount <= 0) {
            return 0;
        }
        FluidStack liquefiedStack = new FluidStack(liquefied, actualLiquidAmount);
        int filled = handler.fill(liquefiedStack, IFluidHandler.FluidAction.EXECUTE);
        if (filled < actualLiquidAmount) {
            handler.fill(drainedTargetGas.copyWithAmount(actualLiquidAmount - filled), IFluidHandler.FluidAction.EXECUTE);
        }
        return filled;
    }

    @Unique
    private Fluid pigeonplus$liquefiedFluid(GasFluid gas) {
        if (gas.isSame(AddonFluids.GASEOUS_BIOGAS.get())) {
            return AddonFluids.LIQUEFIED_BIOGAS.get();
        }
        if (gas.isSame(AddonFluids.COMPRESSED_AIR.get())) {
            return AddonFluids.LIQUID_OXYGEN.get();
        }
        return null;
    }

    @Unique
    private int pigeonplus$liquefactionRatio(GasFluid gas) {
        if (gas.isSame(AddonFluids.GASEOUS_BIOGAS.get())) {
            return GasLiquefactionTracker.BIOGAS_TO_LIQUEFIED_BIOGAS_RATIO;
        }
        if (gas.isSame(AddonFluids.COMPRESSED_AIR.get())) {
            return GasLiquefactionTracker.COMPRESSED_AIR_TO_LIQUID_OXYGEN_RATIO;
        }
        return 0;
    }

    @Unique
    private boolean pigeonplus$isLargeTank(FluidEndpoint endpoint) {
        if (!level.isLoaded(endpoint.containerPos())) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(endpoint.containerPos());
        return blockEntity instanceof LargeFluidTankBlockEntity;
    }

    @Unique
    private boolean pigeonplus$canDrainFromEndpoint(FluidEndpoint source) {
        if (source.sideToPipe() == null) {
            return true;
        }
        Direction faceToContainer = source.sideToPipe().getOpposite();
        Map<Direction, Direction> faces = faceFlow.get(source.fromPipePos());
        if (faces == null) {
            return true;
        }
        Direction allowed = faces.get(faceToContainer);
        return allowed == null || allowed != faceToContainer;
    }

    @Unique
    private PigeonPlus_Reachability pigeonplus$computeReachable(BlockPos start, FluidStack fluid) {
        Map<BlockPos, List<ValveState>> result = new HashMap<>();
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        List<ValveState> startPath = pigeonplus$valvesAt(start, fluid, List.of());
        if (startPath == null) {
            return new PigeonPlus_Reachability(result, cameFrom);
        }
        result.put(start, startPath);
        cameFrom.put(start, null);
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            List<ValveState> curPath = result.get(cur);
            for (BlockPos next : adjacency.getOrDefault(cur, List.of())) {
                if (result.containsKey(next)) {
                    continue;
                }
                if (!pigeonplus$canLeaveDiode(cur, cameFrom.get(cur), next) || !pigeonplus$canPassFaceValve(cur, next)) {
                    continue;
                }
                List<ValveState> nextPath = pigeonplus$valvesAt(next, fluid, curPath);
                if (nextPath == null) {
                    continue;
                }
                result.put(next, nextPath);
                cameFrom.put(next, cur);
                queue.add(next);
            }
        }
        return new PigeonPlus_Reachability(result, cameFrom);
    }

    @Unique
    private boolean pigeonplus$isEndpointReachable(PigeonPlus_Reachability reach, FluidEndpoint target) {
        BlockPos pipe = target.fromPipePos();
        if (!reach.pathValves().containsKey(pipe)) {
            return false;
        }
        if (!pigeonplus$canLeaveDiode(pipe, reach.cameFrom().get(pipe), target.containerPos())) {
            return false;
        }
        if (target.sideToPipe() != null) {
            Direction toContainer = target.sideToPipe().getOpposite();
            Map<Direction, Direction> faces = faceFlow.get(pipe);
            if (faces != null) {
                Direction allowed = faces.get(toContainer);
                return allowed == null || allowed == toContainer;
            }
        }
        return true;
    }

    @Unique
    private boolean pigeonplus$canPassFaceValve(BlockPos cur, BlockPos next) {
        Direction direction = Direction.fromDelta(next.getX() - cur.getX(), next.getY() - cur.getY(), next.getZ() - cur.getZ());
        if (direction == null) {
            return true;
        }
        Map<Direction, Direction> currentFaces = faceFlow.get(cur);
        if (currentFaces != null) {
            Direction allowed = currentFaces.get(direction);
            if (allowed != null && allowed != direction) {
                return false;
            }
        }
        Map<Direction, Direction> nextFaces = faceFlow.get(next);
        if (nextFaces != null) {
            Direction allowed = nextFaces.get(direction.getOpposite());
            return allowed == null || allowed == direction;
        }
        return true;
    }

    @Unique
    private boolean pigeonplus$canLeaveDiode(BlockPos cur, BlockPos from, BlockPos to) {
        Direction inflowDir = diodes.get(cur);
        if (inflowDir == null) {
            return true;
        }
        BlockPos highSide = cur.relative(inflowDir);
        BlockPos lowSide = cur.relative(inflowDir.getOpposite());
        if (!to.equals(lowSide)) {
            return false;
        }
        return from == null || from.equals(highSide);
    }

    @Unique
    private @Nullable List<ValveState> pigeonplus$valvesAt(BlockPos pos, FluidStack fluid, List<ValveState> base) {
        ValveState valve = valves.get(pos);
        if (valve == null) {
            return base;
        }
        if (!valve.allows(fluid)) {
            return null;
        }
        List<ValveState> extended = new ArrayList<>(base);
        extended.add(valve);
        return extended;
    }

    @Unique
    private static int pigeonplus$gasAmount(FluidEndpoint endpoint, GasFluid gas) {
        IFluidHandler handler = endpoint.handler();
        int amount = 0;
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack stack = handler.getFluidInTank(i);
            if (stack.getFluid().isSame(gas)) {
                amount += stack.getAmount();
            }
        }
        return amount;
    }

    @Unique
    private static double pigeonplus$pressure(FluidEndpoint endpoint, GasFluid gas) {
        int capacity = pigeonplus$totalCapacity(endpoint.handler());
        if (capacity <= 0) {
            return 0.0;
        }
        double pressure = (double) pigeonplus$gasAmount(endpoint, gas) / capacity;
        if (endpoint.handler() instanceof CompressedAirDrainFluidHandler
            && gas.isSame(AddonFluids.COMPRESSED_AIR.get())) {
            return pressure + 10.0;
        }
        return pressure;
    }

    @Unique
    private static int pigeonplus$totalCapacity(IFluidHandler handler) {
        int total = 0;
        for (int i = 0; i < handler.getTanks(); i++) {
            total += handler.getTankCapacity(i);
        }
        return total;
    }

    @Unique
    private static int pigeonplus$totalAmount(IFluidHandler handler) {
        int total = 0;
        for (int i = 0; i < handler.getTanks(); i++) {
            total += handler.getFluidInTank(i).getAmount();
        }
        return total;
    }

    @Unique
    private static int pigeonplus$minValveRemaining(List<ValveState> valvePath) {
        if (valvePath == null || valvePath.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        int min = Integer.MAX_VALUE;
        for (ValveState valve : valvePath) {
            min = Math.min(min, valve.remaining());
        }
        return min;
    }

    @Unique
    private static void pigeonplus$deductValves(List<ValveState> valvePath, int amount) {
        if (valvePath == null) {
            return;
        }
        for (ValveState valve : valvePath) {
            valve.consume(amount);
        }
    }

    private record PigeonPlus_Reachability(Map<BlockPos, List<ValveState>> pathValves, Map<BlockPos, BlockPos> cameFrom) {
    }
}
