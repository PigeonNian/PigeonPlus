package dev.anvilcraft.pigeonplus.block.entity;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AnvilCraftPigeonPlus.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlenderBlockEntity>> BLENDER =
        BLOCK_ENTITIES.register("blender", () ->
            BlockEntityType.Builder.of(BlenderBlockEntity::new, AddonBlocks.BLENDER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnvilPumpBlockEntity>> ANVIL_PUMP =
        BLOCK_ENTITIES.register("anvil_pump", () ->
            BlockEntityType.Builder.of(AnvilPumpBlockEntity::new, AddonBlocks.ANVIL_PUMP.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FeedSpreaderBlockEntity>> FEED_SPREADER =
        BLOCK_ENTITIES.register("feed_spreader", () ->
            BlockEntityType.Builder.of(FeedSpreaderBlockEntity::new, AddonBlocks.FEED_SPREADER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StasisBeaconBlockEntity>> STASIS_BEACON =
        BLOCK_ENTITIES.register("stasis_beacon", () ->
            BlockEntityType.Builder.of(StasisBeaconBlockEntity::new, AddonBlocks.STASIS_BEACON.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NozzleExhaustBlockEntity>> NOZZLE_EXHAUST =
        BLOCK_ENTITIES.register("nozzle_exhaust", () ->
            BlockEntityType.Builder.of(NozzleExhaustBlockEntity::new, AddonBlocks.NOZZLE.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            BLENDER.get(),
            BlenderBlockEntity::getFluidHandler
        );
    }
}
