package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class AddonFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(
        NeoForgeRegistries.FLUID_TYPES,
        AnvilCraftPigeonPlus.MOD_ID
    );
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(
        Registries.FLUID,
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

    public static final DeferredHolder<FluidType, FluidType> COMPRESSED_AIR_TYPE = FLUID_TYPES.register(
        "compressed_air",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.%s.compressed_air".formatted(AnvilCraftPigeonPlus.MOD_ID))
            .density(10)
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

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }
}
