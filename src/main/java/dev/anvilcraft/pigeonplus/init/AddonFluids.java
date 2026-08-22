package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.EvaporatingLiquidBlock;
import dev.anvilcraft.pigeonplus.block.MixedBiomassBlock;
import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.joml.Vector3f;

public class AddonFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(
        NeoForgeRegistries.FLUID_TYPES,
        AnvilCraftPigeonPlus.MOD_ID
    );
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(
        Registries.FLUID,
        AnvilCraftPigeonPlus.MOD_ID
    );
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
        Registries.BLOCK,
        AnvilCraftPigeonPlus.MOD_ID
    );

    public static final DeferredHolder<FluidType, FluidType> GASEOUS_BIOGAS_TYPE = FLUID_TYPES.register(
        "gaseous_biogas",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.%s.gaseous_biogas".formatted(AnvilCraftPigeonPlus.MOD_ID))
            .density(-100)
            .viscosity(100)
            .temperature(310)
            .canDrown(false)
            .canSwim(false)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY))
    );

    public static final DeferredHolder<Fluid, GasFluid> GASEOUS_BIOGAS = FLUIDS.register(
        "gaseous_biogas",
        () -> new GasFluid(GASEOUS_BIOGAS_TYPE, AddonItems.GASEOUS_BIOGAS_BUCKET)
    );

    public static final DeferredHolder<FluidType, FluidType> LIQUEFIED_BIOGAS_TYPE = FLUID_TYPES.register(
        "liquefied_biogas",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.%s.liquefied_biogas".formatted(AnvilCraftPigeonPlus.MOD_ID))
            .density(450)
            .viscosity(700)
            .temperature(112)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY))
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUEFIED_BIOGAS = FLUIDS.register(
        "liquefied_biogas",
        () -> new BaseFlowingFluid.Source(liquefiedBiogasProperties())
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LIQUEFIED_BIOGAS_FLOWING = FLUIDS.register(
        "flowing_liquefied_biogas",
        () -> new BaseFlowingFluid.Flowing(liquefiedBiogasProperties())
    );

    public static final DeferredHolder<Block, LiquidBlock> LIQUEFIED_BIOGAS_BLOCK = BLOCKS.register(
        "liquefied_biogas",
        () -> new EvaporatingLiquidBlock(LIQUEFIED_BIOGAS.get(), BlockBehaviour.Properties.of()
            .replaceable()
            .noCollission()
            .strength(100.0f)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid()
            .randomTicks(),
            new Vector3f(0.42f, 0.56f, 0.24f))
    );

    public static final DeferredHolder<FluidType, FluidType> COMPRESSED_AIR_TYPE = FLUID_TYPES.register(
        "compressed_air",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.%s.compressed_air".formatted(AnvilCraftPigeonPlus.MOD_ID))
            .density(-100)
            .viscosity(50)
            .temperature(300)
            .canDrown(false)
            .canSwim(false)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY))
    );

    public static final DeferredHolder<Fluid, GasFluid> COMPRESSED_AIR = FLUIDS.register(
        "compressed_air",
        () -> new GasFluid(COMPRESSED_AIR_TYPE, AddonItems.COMPRESSED_AIR_BUCKET)
    );

    public static final DeferredHolder<FluidType, FluidType> MIXED_BIOMASS_TYPE = FLUID_TYPES.register(
        "mixed_biomass",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.%s.mixed_biomass".formatted(AnvilCraftPigeonPlus.MOD_ID))
            .density(1200)
            .viscosity(1800)
            .temperature(305)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY))
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> MIXED_BIOMASS = FLUIDS.register(
        "mixed_biomass",
        () -> new BaseFlowingFluid.Source(mixedBiomassProperties())
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> MIXED_BIOMASS_FLOWING = FLUIDS.register(
        "flowing_mixed_biomass",
        () -> new BaseFlowingFluid.Flowing(mixedBiomassProperties())
    );

    public static final DeferredHolder<Block, MixedBiomassBlock> MIXED_BIOMASS_BLOCK = BLOCKS.register(
        "mixed_biomass",
        () -> new MixedBiomassBlock(MIXED_BIOMASS.get(), BlockBehaviour.Properties.of()
            .replaceable()
            .noCollission()
            .strength(100.0f)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid())
    );

    public static final DeferredHolder<FluidType, FluidType> LIQUID_OXYGEN_TYPE = FLUID_TYPES.register(
        "liquid_oxygen",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.%s.liquid_oxygen".formatted(AnvilCraftPigeonPlus.MOD_ID))
            .density(1140)
            .viscosity(900)
            .temperature(90)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY))
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_OXYGEN = FLUIDS.register(
        "liquid_oxygen",
        () -> new BaseFlowingFluid.Source(liquidOxygenProperties())
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LIQUID_OXYGEN_FLOWING = FLUIDS.register(
        "flowing_liquid_oxygen",
        () -> new BaseFlowingFluid.Flowing(liquidOxygenProperties())
    );

    public static final DeferredHolder<Block, LiquidBlock> LIQUID_OXYGEN_BLOCK = BLOCKS.register(
        "liquid_oxygen",
        () -> new EvaporatingLiquidBlock(LIQUID_OXYGEN.get(), BlockBehaviour.Properties.of()
            .replaceable()
            .noCollission()
            .strength(100.0f)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid()
            .randomTicks(),
            new Vector3f(0.85f, 0.95f, 1.0f))
    );

    public static final DeferredHolder<FluidType, FluidType> LIQUID_HYDROGEN_TYPE = FLUID_TYPES.register(
        "liquid_hydrogen",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.%s.liquid_hydrogen".formatted(AnvilCraftPigeonPlus.MOD_ID))
            .density(70)
            .viscosity(50)
            .temperature(20)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY))
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_HYDROGEN = FLUIDS.register(
        "liquid_hydrogen",
        () -> new BaseFlowingFluid.Source(liquidHydrogenProperties())
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LIQUID_HYDROGEN_FLOWING = FLUIDS.register(
        "flowing_liquid_hydrogen",
        () -> new BaseFlowingFluid.Flowing(liquidHydrogenProperties())
    );

    public static final DeferredHolder<Block, LiquidBlock> LIQUID_HYDROGEN_BLOCK = BLOCKS.register(
        "liquid_hydrogen",
        () -> new EvaporatingLiquidBlock(LIQUID_HYDROGEN.get(), BlockBehaviour.Properties.of()
            .replaceable()
            .noCollission()
            .strength(100.0f)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid()
            .randomTicks(),
            new Vector3f(0.78f, 0.92f, 1.0f))
    );

    private static BaseFlowingFluid.Properties mixedBiomassProperties() {
        return new BaseFlowingFluid.Properties(MIXED_BIOMASS_TYPE, MIXED_BIOMASS, MIXED_BIOMASS_FLOWING)
            .bucket(AddonItems.MIXED_BIOMASS_BUCKET)
            .block(MIXED_BIOMASS_BLOCK)
            .slopeFindDistance(3)
            .levelDecreasePerBlock(2)
            .tickRate(10)
            .explosionResistance(100.0f);
    }

    private static BaseFlowingFluid.Properties liquefiedBiogasProperties() {
        return new BaseFlowingFluid.Properties(LIQUEFIED_BIOGAS_TYPE, LIQUEFIED_BIOGAS, LIQUEFIED_BIOGAS_FLOWING)
            .bucket(AddonItems.LIQUEFIED_BIOGAS_BUCKET)
            .block(LIQUEFIED_BIOGAS_BLOCK)
            .slopeFindDistance(4)
            .levelDecreasePerBlock(1)
            .tickRate(5)
            .explosionResistance(100.0f);
    }

    private static BaseFlowingFluid.Properties liquidOxygenProperties() {
        return new BaseFlowingFluid.Properties(LIQUID_OXYGEN_TYPE, LIQUID_OXYGEN, LIQUID_OXYGEN_FLOWING)
            .bucket(AddonItems.LIQUID_OXYGEN_BUCKET)
            .block(LIQUID_OXYGEN_BLOCK)
            .slopeFindDistance(4)
            .levelDecreasePerBlock(1)
            .tickRate(5)
            .explosionResistance(100.0f);
    }

    private static BaseFlowingFluid.Properties liquidHydrogenProperties() {
        return new BaseFlowingFluid.Properties(LIQUID_HYDROGEN_TYPE, LIQUID_HYDROGEN, LIQUID_HYDROGEN_FLOWING)
            .bucket(AddonItems.LIQUID_HYDROGEN_BUCKET)
            .block(LIQUID_HYDROGEN_BLOCK)
            .slopeFindDistance(4)
            .levelDecreasePerBlock(1)
            .tickRate(5)
            .explosionResistance(100.0f);
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCKS.register(modEventBus);
    }
}
