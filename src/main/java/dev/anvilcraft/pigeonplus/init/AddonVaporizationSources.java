package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.IVaporConsumer;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.VaporAction;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.VaporStack;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.VaporizationContext;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.VaporizationManager;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.VaporizationOffer;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.VaporizationSource;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.VaporizationSources;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.YukkuriVaporTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class AddonVaporizationSources {
    private static final int MAX_OIL_PER_TICK = 25;
    private static final int OXYGEN_PER_STEP = 13;
    private static final int OUTPUT_STEP = 5;
    private static final ResourceLocation SOURCE_ID = AnvilCraftPigeonPlus.of("crude_oil_liquid_oxygen");
    private static final Map<Long, Long> PROCESSED_TICKS = new HashMap<>();
    private static boolean registered;

    private AddonVaporizationSources() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        VaporizationSources.register(new CrudeOilLiquidOxygenSource());
        registered = true;
    }

    public static boolean hasMixedPropellant(LargeCauldronBlockEntity cauldron) {
        return findMatchingFluid(cauldron, stack -> stack.is(ModFluidTags.OIL)) != null
            && findMatchingFluid(cauldron, stack -> stack.getFluid().isSame(AddonFluids.LIQUID_OXYGEN.get())) != null;
    }

    public static boolean wasCrudeOilVaporizedRecently(Level level, BlockPos cauldronPos) {
        Long tick = PROCESSED_TICKS.get(cauldronPos.asLong());
        if (tick == null) {
            return false;
        }
        long age = level.getGameTime() - tick;
        return age >= 0 && age <= 1;
    }

    public static boolean tryProcessMixedVaporization(VaporizationContext context) {
        if (!(context.cauldron() instanceof LargeCauldronBlockEntity cauldron) || !cauldron.isIgnited()) {
            return false;
        }
        if (wasProcessedThisTick(context)) {
            return false;
        }

        MatchingFluid oil = findMatchingFluid(cauldron, stack -> stack.is(ModFluidTags.OIL));
        MatchingFluid oxygen = findMatchingFluid(cauldron, stack -> stack.getFluid().isSame(AddonFluids.LIQUID_OXYGEN.get()));
        if (oil == null || oxygen == null) {
            return false;
        }

        int outputAmount = computeOutputAmount(MAX_OIL_PER_TICK, oil.amount(), oxygen.amount());
        if (outputAmount <= 0) {
            return false;
        }

        IVaporConsumer consumer = VaporizationManager.findConsumer(context);
        boolean sealedOutlet = consumer != null && consumer.sealsOutlet(context);
        int accepted = simulateAcceptance(consumer, outputAmount, context);
        if (sealedOutlet) {
            if (accepted <= 0) {
                return false;
            }
            outputAmount = computeOutputAmount(accepted, oil.amount(), oxygen.amount());
            if (outputAmount <= 0) {
                return false;
            }
            accepted = simulateAcceptance(consumer, outputAmount, context);
            if (accepted != outputAmount) {
                return false;
            }
        }

        FluidStack oilRequest = oil.stack().copyWithAmount(outputAmount);
        FluidStack oxygenRequest = oxygen.stack().copyWithAmount(outputToOxygen(outputAmount));
        if (!FluidStack.matches(cauldron.drainVaporizationFluid(oilRequest, IFluidHandler.FluidAction.SIMULATE), oilRequest)
            || !FluidStack.matches(cauldron.drainVaporizationFluid(oxygenRequest, IFluidHandler.FluidAction.SIMULATE), oxygenRequest)) {
            return false;
        }

        if (!FluidStack.matches(cauldron.drainVaporizationFluid(oilRequest, IFluidHandler.FluidAction.EXECUTE), oilRequest)
            || !FluidStack.matches(cauldron.drainVaporizationFluid(oxygenRequest, IFluidHandler.FluidAction.EXECUTE), oxygenRequest)) {
            return false;
        }

        markProcessed(context);
        spawnVaporizationParticles(context.level(), context.cauldronPos(), outputAmount);

        if (consumer != null && accepted > 0) {
            VaporStack delivery = new VaporStack(YukkuriVaporTypes.GASEOUS_OIL, accepted);
            int delivered = Math.clamp(
                consumer.receiveVapor(delivery, VaporAction.EXECUTE, context),
                0,
                delivery.amount()
            );
            if (delivered != delivery.amount()) {
                AnvilCraftPigeonPlus.LOGGER.error(
                    "Vapor consumer at {} accepted {} mB after simulating {} mB",
                    context.outletPos(),
                    delivered,
                    delivery.amount()
                );
            }
        }
        return true;
    }

    private static void markProcessed(VaporizationContext context) {
        PROCESSED_TICKS.put(context.cauldronPos().asLong(), context.level().getGameTime());
    }

    private static boolean wasProcessedThisTick(VaporizationContext context) {
        Long tick = PROCESSED_TICKS.get(context.cauldronPos().asLong());
        if (tick == null) {
            return false;
        }
        if (tick.longValue() != context.level().getGameTime()) {
            PROCESSED_TICKS.remove(context.cauldronPos().asLong());
            return false;
        }
        return true;
    }

    private static int simulateAcceptance(@Nullable IVaporConsumer consumer, int outputAmount, VaporizationContext context) {
        if (consumer == null || outputAmount <= 0) {
            return 0;
        }
        return Math.clamp(
            consumer.receiveVapor(new VaporStack(YukkuriVaporTypes.GASEOUS_OIL, outputAmount), VaporAction.SIMULATE, context),
            0,
            outputAmount
        );
    }

    private static int computeOutputAmount(int maxOutput, int oilAmount, int oxygenAmount) {
        int requested = roundDownToStep(Math.min(maxOutput, MAX_OIL_PER_TICK));
        int oilLimited = roundDownToStep(oilAmount);
        int oxygenLimited = (oxygenAmount / OXYGEN_PER_STEP) * OUTPUT_STEP;
        return Math.min(requested, Math.min(oilLimited, oxygenLimited));
    }

    private static @Nullable MatchingFluid findMatchingFluid(
        LargeCauldronBlockEntity cauldron,
        Predicate<FluidStack> predicate
    ) {
        FluidStack selected = FluidStack.EMPTY;
        int maxAmount = 0;
        for (FluidStack stack : cauldron.getFluids().copyFluids()) {
            if (stack.isEmpty() || !predicate.test(stack)) {
                continue;
            }
            if (stack.getAmount() > maxAmount) {
                selected = stack.copy();
                maxAmount = stack.getAmount();
            }
        }
        return maxAmount > 0 ? new MatchingFluid(selected, maxAmount) : null;
    }

    private static void spawnVaporizationParticles(ServerLevel level, BlockPos cauldronPos, int outputAmount) {
        RandomSource random = level.getRandom();
        double centerX = cauldronPos.getX() + 0.5;
        double centerZ = cauldronPos.getZ() + 0.5;
        double baseY = cauldronPos.getY() + 0.22;
        double upperY = cauldronPos.getY() + 0.78;
        double innerRadius = 1.02;
        int scale = Math.max(1, outputAmount / OUTPUT_STEP);

        for (int i = 0; i < 8 + scale; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = random.nextDouble() * innerRadius;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            double y = baseY + random.nextDouble() * 0.38;
            level.sendParticles(
                ParticleTypes.FLAME,
                x,
                y,
                z,
                0,
                (random.nextDouble() - 0.5) * 0.010,
                0.016 + random.nextDouble() * 0.020,
                (random.nextDouble() - 0.5) * 0.010,
                1.0
            );
        }

        for (int i = 0; i < 3 + scale / 2; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 0.34 + random.nextDouble() * 0.42;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            double y = upperY + random.nextDouble() * 0.18;
            level.sendParticles(
                ParticleTypes.FLAME,
                x,
                y,
                z,
                0,
                (random.nextDouble() - 0.5) * 0.008,
                0.014 + random.nextDouble() * 0.016,
                (random.nextDouble() - 0.5) * 0.008,
                1.0
            );
        }

        for (int i = 0; i < 2 + scale / 3; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 0.24 + random.nextDouble() * 0.28;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            double y = cauldronPos.getY() + 0.30 + random.nextDouble() * 0.24;
            level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                x,
                y,
                z,
                0,
                (random.nextDouble() - 0.5) * 0.006,
                0.010 + random.nextDouble() * 0.012,
                (random.nextDouble() - 0.5) * 0.006,
                1.0
            );
        }

        level.sendParticles(ParticleTypes.SMOKE, centerX, cauldronPos.getY() + 0.68, centerZ, 2 + scale / 2, 0.16, 0.08, 0.16, 0.010);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, centerX, cauldronPos.getY() + 0.84, centerZ, 1 + scale / 3, 0.12, 0.06, 0.12, 0.006);
        level.sendParticles(ParticleTypes.CLOUD, centerX, cauldronPos.getY() + 0.62, centerZ, 1 + scale / 3, 0.14, 0.06, 0.14, 0.006);
    }

    private static final class CrudeOilLiquidOxygenSource implements VaporizationSource {
        @Override
        public ResourceLocation id() {
            return SOURCE_ID;
        }

        @Override
        public int priority() {
            return 100;
        }

        @Override
        public VaporizationOffer createOffer(VaporizationContext context, FluidStack availableInput, int maxVapor) {
            if (wasProcessedThisTick(context)) {
                return null;
            }
            if (!(context.cauldron() instanceof LargeCauldronBlockEntity cauldron) || !cauldron.isIgnited()) {
                return null;
            }
            if (!availableInput.is(ModFluidTags.OIL)) {
                return null;
            }

            MatchingFluid oxygen = findMatchingFluid(cauldron, stack -> stack.getFluid().isSame(AddonFluids.LIQUID_OXYGEN.get()));
            if (oxygen == null) {
                return null;
            }
            int outputAmount = computeOutputAmount(maxVapor, availableInput.getAmount(), oxygen.amount());
            if (outputAmount <= 0) {
                return null;
            }

            FluidStack input = availableInput.copyWithAmount(outputAmount);
            VaporStack output = new VaporStack(YukkuriVaporTypes.GASEOUS_OIL, outputAmount);
            return new VaporizationOffer(input, output);
        }

        @Override
        public void commit(VaporizationContext context, VaporizationOffer offer) {
            if (!(context.cauldron() instanceof LargeCauldronBlockEntity cauldron)) {
                return;
            }
            markProcessed(context);
            int oxygenAmount = outputToOxygen(offer.output().amount());
            FluidStack oxygen = new FluidStack(AddonFluids.LIQUID_OXYGEN.get(), oxygenAmount);
            cauldron.drainVaporizationFluid(oxygen, IFluidHandler.FluidAction.EXECUTE);
            spawnVaporizationParticles(context.level(), context.cauldronPos(), offer.output().amount());
        }
    }

    private static int outputToOxygen(int outputAmount) {
        return (outputAmount / OUTPUT_STEP) * OXYGEN_PER_STEP;
    }

    private static int roundDownToStep(int amount) {
        return amount / OUTPUT_STEP * OUTPUT_STEP;
    }

    private record MatchingFluid(FluidStack stack, int amount) {
    }
}
