package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.init.AddonRecipeTypes;
import dev.anvilcraft.pigeonplus.recipe.GasLiquefactionRecipe;
import dev.anvilcraft.pigeonplus.util.FluidTransactions;
import dev.anvilcraft.pigeonplus.util.GasLiquefactionTracker;
import dev.dubhe.anvilcraft.block.container.LargeFluidTankBlock;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 大流体罐内的气体液化。
 *
 * <h3>26.1 的改写</h3>
 * 流体能力从 {@code IFluidHandler} 换成
 * {@link ResourceHandler}{@code <}{@link FluidResource}{@code >}，
 * 相应地把「按槽位取 FluidStack」改为「资源 + 数量」，
 * 抽取/注入统一走 {@link FluidTransactions}（内部处理事务提交）。
 *
 * <p>注意 {@code getTanks()} → {@code size()}、
 * {@code getFluidInTank(i)} → {@code getResource(i)} +
 * {@code getAmountAsLong(i)}、{@code getTankCapacity(i)} →
 * {@code getCapacityAsLong(i, resource)}。
 */
@Mixin(LargeFluidTankBlockEntity.class)
public abstract class LargeFluidTankBlockEntityMixin {
    @Unique
    private static final int PIGEONPLUS_GAS_INPUT_PER_TICK = 2000;

    @Inject(method = "tick", at = @At("HEAD"))
    private void pigeonplus$liquefyGasInTank(CallbackInfo ci) {
        LargeFluidTankBlockEntity self = (LargeFluidTankBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        if (!self.getBlockState().getValue(LargeFluidTankBlock.HALF).equals(Cube3x3PartHalf.MID_CENTER)) {
            return;
        }
        ResourceHandler<FluidResource> handler = self.getFluidHandler();
        BlockPos pos = self.getBlockPos();

        List<GasLiquefactionRecipe> recipes = pigeonplus$findRecipes(level, handler);
        if (recipes.isEmpty()) {
            return;
        }
        Fluid gas = recipes.getFirst().input().getFluid();
        int totalCapacity = pigeonplus$totalCapacity(handler);
        int totalAmount = pigeonplus$totalAmount(handler);
        int gasAmount = pigeonplus$gasAmount(handler, gas);
        if (totalCapacity <= 0 || gasAmount <= 0 || totalAmount < totalCapacity) {
            GasLiquefactionTracker.clear(level, pos, gas);
            return;
        }
        int inputAmount = Math.min(PIGEONPLUS_GAS_INPUT_PER_TICK, gasAmount);
        // 先按「气体总量是否装满」判断，再实际抽取；抽出多少用于后续换算
        int drainedAmount = pigeonplus$extractGas(handler, gas, inputAmount);
        if (drainedAmount <= 0) {
            return;
        }
        for (GasLiquefactionRecipe recipe : recipes) {
            int liquidAmount = GasLiquefactionTracker.addGasInput(
                level,
                pos,
                gas,
                recipe.output().getFluid(),
                drainedAmount,
                recipe.ratio()
            );
            if (liquidAmount > 0) {
                pigeonplus$replaceGasWithLiquid(handler, recipe, liquidAmount);
            }
        }
    }

    /** 从任意槽位抽出指定气体的前 N mB，返回实际抽出量。 */
    @Unique
    private static int pigeonplus$extractGas(ResourceHandler<FluidResource> handler, Fluid gas, int amount) {
        FluidResource resource = FluidResource.of(gas);
        for (int i = 0; i < handler.size(); i++) {
            if (!handler.getResource(i).getFluid().isSame(gas)) {
                continue;
            }
            int extracted = FluidTransactions.extract(handler, i, resource, amount);
            if (extracted > 0) {
                return extracted;
            }
        }
        return 0;
    }

    @Unique
    private static List<GasLiquefactionRecipe> pigeonplus$findRecipes(Level level, ResourceHandler<FluidResource> handler) {
        var server = level.getServer();
        if (server == null) {
            return List.of();
        }
        for (int i = 0; i < handler.size(); i++) {
            FluidResource resource = handler.getResource(i);
            if (resource.isEmpty() || handler.getAmountAsLong(i) <= 0) {
                continue;
            }
            List<GasLiquefactionRecipe> recipes = new ArrayList<>();
            for (var holder : server.getRecipeManager().getAllRecipesFor(
                    AddonRecipeTypes.GAS_LIQUEFACTION_TYPE.get())) {
                if (holder.value().input().getFluid().isSame(resource.getFluid())) {
                    recipes.add(holder.value());
                }
            }
            if (!recipes.isEmpty()) {
                return recipes;
            }
        }
        return List.of();
    }

    @Unique
    private static int pigeonplus$gasAmount(ResourceHandler<FluidResource> handler, Fluid gas) {
        int amount = 0;
        for (int i = 0; i < handler.size(); i++) {
            if (handler.getResource(i).getFluid().isSame(gas)) {
                amount += (int) handler.getAmountAsLong(i);
            }
        }
        return amount;
    }

    @Unique
    private static int pigeonplus$totalAmount(ResourceHandler<FluidResource> handler) {
        int total = 0;
        for (int i = 0; i < handler.size(); i++) {
            total += (int) handler.getAmountAsLong(i);
        }
        return total;
    }

    @Unique
    private static int pigeonplus$totalCapacity(ResourceHandler<FluidResource> handler) {
        int total = 0;
        for (int i = 0; i < handler.size(); i++) {
            FluidResource resource = handler.getResource(i);
            // 空槽位没有资源可用于查询容量，退化为按「空资源」查
            total += (int) handler.getCapacityAsLong(i, resource);
        }
        return total;
    }

    /**
     * 把等量的气体换成液体：先抽走气体，再注入液体。
     *
     * <p>若注入不完整（液体那一侧满了），把剩余的气体还回去，
     * 避免凭空吞掉流体。
     */
    @Unique
    private static void pigeonplus$replaceGasWithLiquid(
        ResourceHandler<FluidResource> handler,
        GasLiquefactionRecipe recipe,
        int liquidAmount
    ) {
        if (liquidAmount <= 0) {
            return;
        }
        Fluid gas = recipe.input().getFluid();
        Fluid liquid = recipe.output().getFluid();
        int actualAmount = pigeonplus$extractGas(handler, gas, liquidAmount);
        if (actualAmount <= 0) {
            return;
        }
        FluidResource liquidResource = FluidResource.of(liquid);
        int filled = 0;
        for (int i = 0; i < handler.size() && filled < actualAmount; i++) {
            if (handler.getResource(i).getFluid().isSame(liquid)) {
                filled += FluidTransactions.insert(handler, i, liquidResource, actualAmount - filled);
            }
        }
        if (filled < actualAmount) {
            // 液体没全部装下，把多余气体退回
            int remaining = actualAmount - filled;
            for (int i = 0; i < handler.size() && remaining > 0; i++) {
                if (handler.getResource(i).isEmpty()
                    || handler.getResource(i).getFluid().isSame(gas)) {
                    remaining -= FluidTransactions.insert(handler, i, FluidResource.of(gas), remaining);
                }
            }
        }
    }
}
