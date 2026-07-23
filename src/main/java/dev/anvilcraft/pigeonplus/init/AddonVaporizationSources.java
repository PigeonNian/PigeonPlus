package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.lib.v2.yukkuri.api.vapor.IVaporConsumer;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.VaporAction;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.VaporStack;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.VaporizationContext;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.VaporizationManager;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.VaporizationOffer;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.VaporizationSource;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.VaporizationSources;
import dev.anvilcraft.lib.v2.yukkuri.api.vapor.YukkuriVaporTypes;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
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
    private static final ResourceLocation METHANE_SOURCE_ID = AnvilCraftPigeonPlus.of("liquefied_biogas_liquid_oxygen");
    private static final ResourceLocation METHANE_VAPOR = AnvilCraftPigeonPlus.of("methane_combustion");
    private static final int BIOGAS_PER_REACTION = 1000;
    private static final int LIQUID_OXYGEN_PER_REACTION = 741;
    private static final int METHANE_OUTPUT_STEP = 1000;
    private static final Map<Long, ProcessState> PROCESSED_TICKS = new HashMap<>();
    private static boolean registered;

    private AddonVaporizationSources() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        VaporizationSources.register(new CrudeOilLiquidOxygenSource());
        VaporizationSources.register(new LiquefiedBiogasLiquidOxygenSource());
        registered = true;
    }

    public static boolean hasMixedPropellant(LargeCauldronBlockEntity cauldron) {
        return findMatchingFluid(cauldron, stack -> stack.is(ModFluidTags.OIL)) != null
            && hasLiquidOxygen(cauldron);
    }

    public static boolean hasMethanePropellant(LargeCauldronBlockEntity cauldron) {
        return findMatchingFluid(cauldron, stack -> stack.getFluid().isSame(AddonFluids.LIQUEFIED_BIOGAS.get())) != null
            && hasLiquidOxygen(cauldron);
    }

    public static boolean hasAnyPropellant(LargeCauldronBlockEntity cauldron) {
        return hasMixedPropellant(cauldron) || hasMethanePropellant(cauldron);
    }

    public static @Nullable JetPropellant getAvailableJetPropellant(LargeCauldronBlockEntity cauldron) {
        if (hasMethanePropellant(cauldron)) {
            return JetPropellant.METHANE;
        }
        if (hasMixedPropellant(cauldron)) {
            return JetPropellant.KEROSENE;
        }
        return null;
    }

    public static boolean wasCrudeOilVaporizedRecently(Level level, BlockPos cauldronPos) {
        return wasVaporizedRecently(level, cauldronPos, JetPropellant.KEROSENE);
    }

    public static boolean wasMethaneVaporizedRecently(Level level, BlockPos cauldronPos) {
        return wasVaporizedRecently(level, cauldronPos, JetPropellant.METHANE);
    }

    public static @Nullable JetPropellant getRecentJetPropellant(Level level, BlockPos cauldronPos) {
        ProcessState state = PROCESSED_TICKS.get(cauldronPos.asLong());
        if (state == null) {
            return null;
        }
        long age = level.getGameTime() - state.tick();
        return age >= 0 && age <= 1 ? state.kind() : null;
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

        markProcessed(context, JetPropellant.KEROSENE);
        spawnVaporizationParticles(context.level(), context.cauldronPos(), outputAmount);

        if (consumer != null && accepted > 0) {
            VaporStack delivery = new VaporStack(YukkuriVaporTypes.GASEOUS_OIL, accepted);
            deliverVapor(consumer, delivery, context);
        }
        return true;
    }

    public static boolean tryProcessMethaneVaporization(VaporizationContext context) {
        if (!(context.cauldron() instanceof LargeCauldronBlockEntity cauldron) || !cauldron.isIgnited()) {
            return false;
        }
        if (wasProcessedThisTick(context)) {
            return false;
        }

        MatchingFluid biogas = findMatchingFluid(cauldron, stack -> stack.getFluid().isSame(AddonFluids.LIQUEFIED_BIOGAS.get()));
        MatchingFluid oxygen = findMatchingFluid(cauldron, stack -> stack.getFluid().isSame(AddonFluids.LIQUID_OXYGEN.get()));
        if (biogas == null || oxygen == null) {
            return false;
        }

        int outputAmount = computeMethaneOutputAmount(METHANE_OUTPUT_STEP, biogas.amount(), oxygen.amount());
        if (outputAmount <= 0) {
            return false;
        }

        IVaporConsumer consumer = VaporizationManager.findConsumer(context);
        boolean sealedOutlet = consumer != null && consumer.sealsOutlet(context);
        int accepted = simulateMethaneAcceptance(consumer, outputAmount, context);
        if (sealedOutlet) {
            if (accepted <= 0) {
                return false;
            }
            outputAmount = computeMethaneOutputAmount(accepted, biogas.amount(), oxygen.amount());
            if (outputAmount <= 0) {
                return false;
            }
            accepted = simulateMethaneAcceptance(consumer, outputAmount, context);
            if (accepted != outputAmount) {
                return false;
            }
        }

        FluidStack biogasRequest = biogas.stack().copyWithAmount(outputToBiogas(outputAmount));
        FluidStack oxygenRequest = oxygen.stack().copyWithAmount(outputToLiquidOxygen(outputAmount));
        if (!FluidStack.matches(cauldron.drainVaporizationFluid(biogasRequest, IFluidHandler.FluidAction.SIMULATE), biogasRequest)
            || !FluidStack.matches(cauldron.drainVaporizationFluid(oxygenRequest, IFluidHandler.FluidAction.SIMULATE), oxygenRequest)) {
            return false;
        }

        if (!FluidStack.matches(cauldron.drainVaporizationFluid(biogasRequest, IFluidHandler.FluidAction.EXECUTE), biogasRequest)
            || !FluidStack.matches(cauldron.drainVaporizationFluid(oxygenRequest, IFluidHandler.FluidAction.EXECUTE), oxygenRequest)) {
            return false;
        }

        markProcessed(context, JetPropellant.METHANE);
        spawnMethaneVaporizationParticles(context.level(), context.cauldronPos(), outputAmount);

        if (consumer != null && accepted > 0) {
            VaporStack delivery = new VaporStack(METHANE_VAPOR, accepted);
            deliverVapor(consumer, delivery, context);
        }
        return true;
    }

    private static boolean wasVaporizedRecently(Level level, BlockPos cauldronPos, JetPropellant kind) {
        ProcessState state = PROCESSED_TICKS.get(cauldronPos.asLong());
        if (state == null || state.kind() != kind) {
            return false;
        }
        long age = level.getGameTime() - state.tick();
        return age >= 0 && age <= 1;
    }

    private static void markProcessed(VaporizationContext context, JetPropellant kind) {
        PROCESSED_TICKS.put(context.cauldronPos().asLong(), new ProcessState(context.level().getGameTime(), kind));
    }

    private static boolean wasProcessedThisTick(VaporizationContext context) {
        ProcessState state = PROCESSED_TICKS.get(context.cauldronPos().asLong());
        if (state == null) {
            return false;
        }
        if (state.tick() != context.level().getGameTime()) {
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

    private static int simulateMethaneAcceptance(@Nullable IVaporConsumer consumer, int outputAmount, VaporizationContext context) {
        if (consumer == null || outputAmount <= 0) {
            return 0;
        }
        return Math.clamp(
            consumer.receiveVapor(new VaporStack(METHANE_VAPOR, outputAmount), VaporAction.SIMULATE, context),
            0,
            outputAmount
        );
    }

    private static void deliverVapor(IVaporConsumer consumer, VaporStack delivery, VaporizationContext context) {
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

    private static int computeOutputAmount(int maxOutput, int oilAmount, int oxygenAmount) {
        int requested = roundDownToStep(Math.min(maxOutput, MAX_OIL_PER_TICK));
        int oilLimited = roundDownToStep(oilAmount);
        int oxygenLimited = (oxygenAmount / OXYGEN_PER_STEP) * OUTPUT_STEP;
        return Math.min(requested, Math.min(oilLimited, oxygenLimited));
    }

    private static int computeMethaneOutputAmount(int maxOutput, int biogasAmount, int oxygenAmount) {
        int requested = roundDownToMethaneStep(Math.min(maxOutput, METHANE_OUTPUT_STEP));
        int biogasLimited = (biogasAmount / BIOGAS_PER_REACTION) * METHANE_OUTPUT_STEP;
        int oxygenLimited = (oxygenAmount / LIQUID_OXYGEN_PER_REACTION) * METHANE_OUTPUT_STEP;
        return Math.min(requested, Math.min(biogasLimited, oxygenLimited));
    }

    private static int outputToOxygen(int outputAmount) {
        return (outputAmount / OUTPUT_STEP) * OXYGEN_PER_STEP;
    }

    private static int outputToBiogas(int outputAmount) {
        return (outputAmount / METHANE_OUTPUT_STEP) * BIOGAS_PER_REACTION;
    }

    private static int outputToLiquidOxygen(int outputAmount) {
        return (outputAmount / METHANE_OUTPUT_STEP) * LIQUID_OXYGEN_PER_REACTION;
    }

    private static int roundDownToStep(int amount) {
        return amount / OUTPUT_STEP * OUTPUT_STEP;
    }

    private static int roundDownToMethaneStep(int amount) {
        return amount / METHANE_OUTPUT_STEP * METHANE_OUTPUT_STEP;
    }

    private static boolean hasLiquidOxygen(LargeCauldronBlockEntity cauldron) {
        return findMatchingFluid(cauldron, stack -> stack.getFluid().isSame(AddonFluids.LIQUID_OXYGEN.get())) != null;
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

    private static void spawnMethaneVaporizationParticles(ServerLevel level, BlockPos cauldronPos, int outputAmount) {
        RandomSource random = level.getRandom();
        double centerX = cauldronPos.getX() + 0.5;
        double centerZ = cauldronPos.getZ() + 0.5;
        double baseY = cauldronPos.getY() + 0.18;
        int scale = Math.max(1, outputAmount / METHANE_OUTPUT_STEP);

        for (int i = 0; i < 30 + scale * 6; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = random.nextDouble() * 1.08;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            double y = baseY + random.nextDouble() * 0.55;
            level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                x,
                y,
                z,
                0,
                (random.nextDouble() - 0.5) * 0.012,
                0.020 + random.nextDouble() * 0.020,
                (random.nextDouble() - 0.5) * 0.012,
                1.0
            );
        }

        for (int i = 0; i < 8 + scale * 2; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 0.25 + random.nextDouble() * 0.72;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            double y = cauldronPos.getY() + 0.38 + random.nextDouble() * 0.42;
            level.sendParticles(
                ParticleTypes.END_ROD,
                x,
                y,
                z,
                0,
                (random.nextDouble() - 0.5) * 0.010,
                0.010 + random.nextDouble() * 0.014,
                (random.nextDouble() - 0.5) * 0.010,
                1.0
            );
        }

        level.sendParticles(ParticleTypes.CLOUD, centerX, cauldronPos.getY() + 0.62, centerZ, 3 + scale, 0.32, 0.12, 0.32, 0.010);
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
            markProcessed(context, JetPropellant.KEROSENE);
            int oxygenAmount = outputToOxygen(offer.output().amount());
            FluidStack oxygen = new FluidStack(AddonFluids.LIQUID_OXYGEN.get(), oxygenAmount);
            cauldron.drainVaporizationFluid(oxygen, IFluidHandler.FluidAction.EXECUTE);
            spawnVaporizationParticles(context.level(), context.cauldronPos(), offer.output().amount());
        }
    }

    private static final class LiquefiedBiogasLiquidOxygenSource implements VaporizationSource {
        @Override
        public ResourceLocation id() {
            return METHANE_SOURCE_ID;
        }

        @Override
        public int priority() {
            return 110;
        }

        @Override
        public VaporizationOffer createOffer(VaporizationContext context, FluidStack availableInput, int maxVapor) {
            if (wasProcessedThisTick(context)) {
                return null;
            }
            if (!(context.cauldron() instanceof LargeCauldronBlockEntity cauldron) || !cauldron.isIgnited()) {
                return null;
            }
            if (!availableInput.getFluid().isSame(AddonFluids.LIQUEFIED_BIOGAS.get())) {
                return null;
            }

            MatchingFluid oxygen = findMatchingFluid(cauldron, stack -> stack.getFluid().isSame(AddonFluids.LIQUID_OXYGEN.get()));
            if (oxygen == null) {
                return null;
            }
            int outputAmount = computeMethaneOutputAmount(maxVapor, availableInput.getAmount(), oxygen.amount());
            if (outputAmount <= 0) {
                return null;
            }

            FluidStack input = availableInput.copyWithAmount(outputToBiogas(outputAmount));
            VaporStack output = new VaporStack(METHANE_VAPOR, outputAmount);
            return new VaporizationOffer(input, output);
        }

        @Override
        public void commit(VaporizationContext context, VaporizationOffer offer) {
            if (!(context.cauldron() instanceof LargeCauldronBlockEntity cauldron)) {
                return;
            }
            markProcessed(context, JetPropellant.METHANE);
            int oxygenAmount = outputToLiquidOxygen(offer.output().amount());
            FluidStack oxygen = new FluidStack(AddonFluids.LIQUID_OXYGEN.get(), oxygenAmount);
            cauldron.drainVaporizationFluid(oxygen, IFluidHandler.FluidAction.EXECUTE);
            spawnMethaneVaporizationParticles(context.level(), context.cauldronPos(), offer.output().amount());
        }
    }

    public enum JetPropellant {
        KEROSENE,
        METHANE
    }

    private record MatchingFluid(FluidStack stack, int amount) {
    }

    private record ProcessState(long tick, JetPropellant kind) {
    }
}
