package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.AnvilPumpBlock;
import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.anvilcraft.pigeonplus.block.FeedSpreaderBlock;
import dev.anvilcraft.pigeonplus.block.MixedBiomassCauldronBlock;
import dev.anvilcraft.pigeonplus.block.NozzleBlock;
import dev.anvilcraft.pigeonplus.block.PigeonAnvilBlock;
import dev.anvilcraft.pigeonplus.block.StasisBeaconBlock;
import dev.dubhe.anvilcraft.block.item.FlexibleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;

import static dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus.REGISTRUM;

public class AddonBlocks {
    static {
        REGISTRUM.defaultCreativeTab(AddonItemGroups.ADDON_ITEMS.getKey());
    }

    public static final BlockEntry<NozzleBlock> NOZZLE = REGISTRUM
        .block("nozzle", NozzleBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties
            .noOcclusion()
            .sound(SoundType.METAL)
            .forceSolidOn()
            .explosionResistance(1200.0F))
        .blockstate((ctx, provider) -> provider.getVariantBuilder(ctx.getEntry()).forAllStates(state ->
            ConfiguredModel.builder()
                .modelFile(provider.models().getExistingFile(ResourceLocation.fromNamespaceAndPath(
                    AnvilCraftPigeonPlus.MOD_ID,
                    state.getValue(NozzleBlock.PART) == DirectionCube3x3PartHalf.MID_CENTER
                        ? "block/nozzle"
                        : "block/nozzle_part"
                )))
                .rotationX(nozzleRotationX(state.getValue(NozzleBlock.FACING)))
                .rotationY(nozzleRotationY(state.getValue(NozzleBlock.FACING)))
                .build()))
        .loot(FlexibleMultiPartBlock::loot)
        .item(FlexibleMultiPartBlockItem<DirectionCube3x3PartHalf, DirectionProperty, Direction>::new)
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(),
                ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/nozzle")))
            .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<BlenderBlock> BLENDER = REGISTRUM
        .block("blender", BlenderBlock::new)
        .blockstate((ctx, prov) -> prov.getVariantBuilder(ctx.getEntry()).forAllStates(state ->
            ConfiguredModel.builder()
                .modelFile(prov.models().getExistingFile(
                    ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/blender_bottom")))
                .rotationY(rotationY(state.getValue(BlenderBlock.FACING)))
                .build()))
        .item(BlockItem::new)
        .model((ctx, prov) -> prov.withExistingParent(ctx.getName(),
            ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/blender")))
        .build()
        .register();

    public static final BlockEntry<StasisBeaconBlock> STASIS_BEACON = REGISTRUM
        .block("stasis_beacon", StasisBeaconBlock::new)
        .initialProperties(() -> Blocks.BEACON)
        .properties(properties -> properties.isValidSpawn(Blocks::never))
        .blockstate((ctx, provider) -> provider.getVariantBuilder(ctx.getEntry()).forAllStates(state ->
            ConfiguredModel.builder()
                .modelFile(provider.models().getExistingFile(ResourceLocation.fromNamespaceAndPath(
                    AnvilCraftPigeonPlus.MOD_ID,
                    "block/stasis_beacon"
                )))
                .build()))
        .item(BlockItem::new)
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(),
                ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/stasis_beacon")))
            .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<AnvilPumpBlock> ANVIL_PUMP = REGISTRUM
        .block("anvil_pump", AnvilPumpBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.noOcclusion().sound(SoundType.METAL))
        .blockstate((ctx, provider) -> provider.getVariantBuilder(ctx.getEntry()).forAllStates(state -> ConfiguredModel.builder()
            .modelFile(provider.models().getExistingFile(ResourceLocation.fromNamespaceAndPath(
                AnvilCraftPigeonPlus.MOD_ID,
                "block/anvil_pump"
            )))
            .rotationY(pumpRotationY(state.getValue(AnvilPumpBlock.FACING)))
            .build()))
        .item(BlockItem::new)
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(),
                ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/anvil_pump_full")))
            .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<FeedSpreaderBlock> FEED_SPREADER = REGISTRUM
        .block("feed_spreader", FeedSpreaderBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.noOcclusion().sound(SoundType.METAL))
        .blockstate((ctx, provider) -> provider.simpleBlock(
            ctx.getEntry(),
            provider.models().getExistingFile(ResourceLocation.fromNamespaceAndPath(
                AnvilCraftPigeonPlus.MOD_ID,
                "block/feed_spreader_bottom"
            ))
        ))
        .item(BlockItem::new)
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(),
                ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/feed_spreader_full")))
            .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<PigeonAnvilBlock> PIGEON_ANVIL = REGISTRUM
        .block("pigeon_anvil", PigeonAnvilBlock::new)
        .initialProperties(() -> Blocks.ANVIL)
        .properties(properties -> properties.noOcclusion().sound(SoundType.WOOL))
        .blockstate((ctx, provider) -> provider.getVariantBuilder(ctx.getEntry()).forAllStates(state ->
            ConfiguredModel.builder()
                .modelFile(provider.models().getExistingFile(ResourceLocation.fromNamespaceAndPath(
                    AnvilCraftPigeonPlus.MOD_ID,
                    "block/pigeon_anvil"
                )))
                .rotationY(anvilRotationY(state.getValue(PigeonAnvilBlock.FACING)))
                .build()))
        .item(BlockItem::new)
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(),
                ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/pigeon_anvil")))
            .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.ANVIL)
        .register();

    public static final BlockEntry<MixedBiomassCauldronBlock> MIXED_BIOMASS_CAULDRON = REGISTRUM
        .block("mixed_biomass_cauldron", MixedBiomassCauldronBlock::new)
        .initialProperties(() -> Blocks.CAULDRON)
        .blockstate((ctx, prov) -> prov.getVariantBuilder(ctx.getEntry()).forAllStates(state ->
            ConfiguredModel.builder()
                .modelFile(prov.models().getExistingFile(ResourceLocation.fromNamespaceAndPath(
                    AnvilCraftPigeonPlus.MOD_ID,
                    "block/mixed_biomass_cauldron_%s".formatted(
                        state.getValue(MixedBiomassCauldronBlock.LEVEL) == 4
                            ? "full"
                            : "level" + state.getValue(MixedBiomassCauldronBlock.LEVEL)
                    )
                )))
                .build()))
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.CAULDRONS)
        .onRegister(block -> Item.BY_BLOCK.put(block, Items.CAULDRON))
        .register();

    public static void register() {
    }

    private static int rotationY(Direction direction) {
        return switch (direction) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    private static int pumpRotationY(Direction direction) {
        return switch (direction) {
            case WEST -> 90;
            case NORTH -> 180;
            case EAST -> 270;
            default -> 0;
        };
    }

    private static int anvilRotationY(Direction direction) {
        return switch (direction) {
            case EAST -> 270;
            case SOUTH -> 0;
            case WEST -> 90;
            default -> 180;
        };
    }

    private static int nozzleRotationX(Direction direction) {
        return switch (direction) {
            case DOWN -> 180;
            case UP -> 0;
            default -> 90;
        };
    }

    private static int nozzleRotationY(Direction direction) {
        return switch (direction) {
            case UP, DOWN, NORTH -> 0;
            case EAST -> 90;
            case SOUTH -> 180;
            default -> 270;
        };
    }
}
