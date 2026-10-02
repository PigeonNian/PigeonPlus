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

/**
 * 本模组的方块实体类型注册与能力绑定。
 *
 * <h3>26.1 的写法变化</h3>
 * <ul>
 *   <li>{@code BlockEntityType.Builder} 已被移除，改为直接
 *       {@code new BlockEntityType<>(supplier, blocks...)}。</li>
 *   <li>能力常量换了名字与类型：{@code Capabilities.FluidHandler.BLOCK}
 *       → {@link Capabilities.Fluid#BLOCK}、{@code ItemHandler.BLOCK}
 *       → {@link Capabilities.Item#BLOCK}；且它们现在暴露的是新的
 *       {@code ResourceHandler} 而不是 {@code IFluidHandler}/{@code IItemHandler}。</li>
 * </ul>
 */
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AnvilCraftPigeonPlus.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlenderBlockEntity>> BLENDER =
        BLOCK_ENTITIES.register("blender", () ->
            new BlockEntityType<>(BlenderBlockEntity::new, AddonBlocks.BLENDER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnvilPumpBlockEntity>> ANVIL_PUMP =
        BLOCK_ENTITIES.register("anvil_pump", () ->
            new BlockEntityType<>(AnvilPumpBlockEntity::new, AddonBlocks.ANVIL_PUMP.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FeedSpreaderBlockEntity>> FEED_SPREADER =
        BLOCK_ENTITIES.register("feed_spreader", () ->
            new BlockEntityType<>(FeedSpreaderBlockEntity::new, AddonBlocks.FEED_SPREADER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StasisBeaconBlockEntity>> STASIS_BEACON =
        BLOCK_ENTITIES.register("stasis_beacon", () ->
            new BlockEntityType<>(StasisBeaconBlockEntity::new, AddonBlocks.STASIS_BEACON.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NozzleExhaustBlockEntity>> NOZZLE_EXHAUST =
        BLOCK_ENTITIES.register("nozzle_exhaust", () ->
            new BlockEntityType<>(NozzleExhaustBlockEntity::new, AddonBlocks.NOZZLE.get()));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.Fluid.BLOCK,
            BLENDER.get(),
            BlenderBlockEntity::getFluidHandler
        );
        event.registerBlockEntity(
            Capabilities.Item.BLOCK,
            FEED_SPREADER.get(),
            (blockEntity, side) -> blockEntity.getInventory()
        );
    }
}
