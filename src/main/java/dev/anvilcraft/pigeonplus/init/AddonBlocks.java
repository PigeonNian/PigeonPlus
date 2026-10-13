package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.AnvilPumpBlock;
import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.anvilcraft.pigeonplus.block.ElectricBlenderBlock;
import dev.anvilcraft.pigeonplus.block.FeedSpreaderBlock;
import dev.anvilcraft.pigeonplus.block.LargeFireworksRocketBlock;
import dev.anvilcraft.pigeonplus.block.MixedBiomassCauldronBlock;
import dev.anvilcraft.pigeonplus.block.NozzleBlock;
import dev.anvilcraft.pigeonplus.block.PigeonAnvilBlock;
import dev.anvilcraft.pigeonplus.block.StasisBeaconBlock;
import dev.anvilcraft.pigeonplus.block.state.LargeFireworksRocketPart;
import dev.dubhe.anvilcraft.block.item.FlexibleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.item.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;

import static dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus.REGISTRUM;

public class AddonBlocks {
    /** 纸块的顶/底面贴图。 */
    private static final ResourceLocation PAPER_BLOCK_END =
        AnvilCraftPigeonPlus.of("block/paper_block");
    /** 纸块的侧面贴图。 */
    private static final ResourceLocation PAPER_BLOCK_SIDE =
        AnvilCraftPigeonPlus.of("block/paper_block_side");

    static {
        REGISTRUM.defaultCreativeTab(AddonItemGroups.ADDON_ITEMS.getKey());
    }

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

    private static final ResourceLocation BLENDER_BASE =
        AnvilCraftPigeonPlus.of("block/blender/pneumatic_blender");
    private static final ResourceLocation BLENDER_BASE_ACTIVE =
        AnvilCraftPigeonPlus.of("block/blender/pneumatic_blender_active");
    private static final ResourceLocation ELECTRIC_BLENDER_BASE =
        AnvilCraftPigeonPlus.of("block/blender/electric_blender");
    private static final ResourceLocation ELECTRIC_BLENDER_BASE_ACTIVE =
        AnvilCraftPigeonPlus.of("block/blender/electric_blender_active");

    public static final BlockEntry<BlenderBlock> BLENDER = REGISTRUM
        .block("blender", BlenderBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate((ctx, prov) -> prov.getVariantBuilder(ctx.getEntry()).forAllStates(state ->
            ConfiguredModel.builder()
                .modelFile(prov.models().getExistingFile(
                    state.getValue(BlenderBlock.WORKING) ? BLENDER_BASE_ACTIVE : BLENDER_BASE))
                .rotationY(rotationY(state.getValue(BlenderBlock.FACING)))
                .build()))
        .item(BlockItem::new)
        .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), BLENDER_BASE))
        .build()
        .register();

    public static final BlockEntry<ElectricBlenderBlock> ELECTRIC_BLENDER = REGISTRUM
        .block("electric_blender", ElectricBlenderBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate((ctx, prov) -> prov.getVariantBuilder(ctx.getEntry()).forAllStates(state ->
            ConfiguredModel.builder()
                .modelFile(prov.models().getExistingFile(
                    state.getValue(BlenderBlock.WORKING) || state.getValue(ElectricBlenderBlock.HEATING)
                        ? ELECTRIC_BLENDER_BASE_ACTIVE
                        : ELECTRIC_BLENDER_BASE))
                .rotationY(rotationY(state.getValue(BlenderBlock.FACING)))
                .build()))
        .item(BlockItem::new)
        .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), ELECTRIC_BLENDER_BASE))
        .build()
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

    public static final BlockEntry<LargeFireworksRocketBlock> LARGE_FIREWORKS_ROCKET = REGISTRUM
        .block("large_fireworks_rocket", LargeFireworksRocketBlock::new)
        .initialProperties(() -> Blocks.WHITE_WOOL)
        .properties(properties -> properties
            .sound(SoundType.WOOL)
            .noOcclusion()
            .isViewBlocking((state, level, pos) -> false))
        .blockstate((ctx, provider) -> provider.getVariantBuilder(ctx.getEntry()).forAllStates(state ->
            ConfiguredModel.builder()
                .modelFile(provider.models().getExistingFile(AnvilCraftPigeonPlus.of(
                    "block/large_fireworks_rocket_" + state.getValue(LargeFireworksRocketBlock.PART).getModelName())))
                .rotationY(rotationY(state.getValue(LargeFireworksRocketBlock.FACING)))
                .build()))
        .loot(SimpleMultiPartBlock::loot)
        .item(SimpleMultiPartBlockItem<LargeFireworksRocketPart>::new)
        // 物品模型分两层：手写的 Blockbench 模型 item/large_fireworks_rocket_model（自带 display），
        // 与这里生成的 item/large_fireworks_rocket 包装（parent 指向它）。
        // 两层不能同名：Registrum 必定为方块生成物品模型，同名会让 src/main 与 src/generated
        // 出现重复条目导致 processResources 失败，且包装会退化成自引用。
        .model((ctx, prov) -> prov.withExistingParent(
            ctx.getName(), AnvilCraftPigeonPlus.of("item/large_fireworks_rocket_model")))
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<Block> PAPER_BLOCK = REGISTRUM
        .block("paper_block", Block::new)
        .initialProperties(() -> Blocks.WHITE_WOOL)
        .blockstate((ctx, provider) -> provider.simpleBlock(
            ctx.getEntry(),
            provider.models().getExistingFile(AnvilCraftPigeonPlus.of("block/paper_block"))))
        .item(BlockItem::new)
        .model((ctx, prov) -> prov.withExistingParent(
            ctx.getName(), AnvilCraftPigeonPlus.of("block/paper_block")))
        .build()
        .tag(BlockTags.WOOL)
        .register();

    public static final BlockEntry<StairBlock> PAPER_BLOCK_STAIRS = REGISTRUM
        .block("paper_block_stairs", properties ->
            new StairBlock(PAPER_BLOCK.get().defaultBlockState(), properties))
        .initialProperties(() -> Blocks.WHITE_WOOL)
        .blockstate((ctx, provider) -> provider.stairsBlock(
            ctx.getEntry(), PAPER_BLOCK_SIDE, PAPER_BLOCK_END, PAPER_BLOCK_END))
        .item(BlockItem::new)
        .model((ctx, prov) -> prov.withExistingParent(
            ctx.getName(), AnvilCraftPigeonPlus.of("block/paper_block_stairs")))
        .build()
        .tag(BlockTags.WOOL)
        .register();

    public static final BlockEntry<SlabBlock> PAPER_BLOCK_SLAB = REGISTRUM
        .block("paper_block_slab", SlabBlock::new)
        .initialProperties(() -> Blocks.WHITE_WOOL)
        .blockstate((ctx, provider) -> provider.slabBlock(
            ctx.getEntry(),
            AnvilCraftPigeonPlus.of("block/paper_block"),
            PAPER_BLOCK_SIDE, PAPER_BLOCK_END, PAPER_BLOCK_END))
        .loot((tables, block) -> tables.add(block, tables.createSlabItemTable(block)))
        .item(BlockItem::new)
        .model((ctx, prov) -> prov.withExistingParent(
            ctx.getName(), AnvilCraftPigeonPlus.of("block/paper_block_slab")))
        .build()
        .tag(BlockTags.WOOL)
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
