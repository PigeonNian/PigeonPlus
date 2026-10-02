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
import dev.dubhe.anvilcraft.item.block.FlexibleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import static dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus.REGISTRUM;

/**
 * 本模组的方块注册与数据生成。
 *
 * <h3>26.1 的 datagen 改写说明</h3>
 * 1.21.1 用的 NeoForge {@code BlockStateProvider} / {@code ConfiguredModel} /
 * {@code ModelFile} 这一整套 API 在 26.1 <strong>已被移除</strong>，
 * 改为原版新的 {@code BlockModelGenerators} 体系（AnvilLib 的
 * {@code RegistrumBlockModelGenerator} 直接继承它）。主要差异：
 * <ul>
 *   <li>不再有 {@code ConfiguredModel.builder().modelFile(...).rotationY(n).build()}；
 *       改为 {@code MultiVariantGenerator.dispatch(block, plainVariant(id))}
 *       再 {@code .with(旋转)}。</li>
 *   <li>常用旋转已由原版常量提供：{@code ROTATION_HORIZONTAL_FACING}（东=90°）
 *       与 {@code ROTATION_HORIZONTAL_FACING_ALT}（南=0°）。
 *       本模组原先手写的 {@code rotationY}/{@code pumpRotationY}/{@code anvilRotationY}
 *       与这两个常量完全一致，故直接替换、不再需要自定义函数。</li>
 *   <li>物品模型由 {@code withExistingParent} 改为
 *       {@code RegistrumItemModelGenerator#createWithExistingModel}。</li>
 *   <li>{@code ExistingFileHelper} 已不存在，模型直接按 id 引用，无需再查找校验。</li>
 * </ul>
 */
public class AddonBlocks {
    static {
        REGISTRUM.defaultCreativeTab(AddonItemGroups.ADDON_ITEMS.getKey());
    }

    public static final BlockEntry<PigeonAnvilBlock> PIGEON_ANVIL = REGISTRUM
        .block("pigeon_anvil", PigeonAnvilBlock::new)
        .initialProperties(() -> Blocks.ANVIL)
        .properties(properties -> properties.noOcclusion().sound(SoundType.WOOL))
        // 铁砧朝向：东=270°、南=0°，与 ALT 常量一致
        .blockstate(() -> (ctx, gen) -> gen.blockStateOutput.accept(
            MultiVariantGenerator.dispatch(ctx.getEntry(), pigeonplus$variant("block/pigeon_anvil"))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING_ALT)))
        .item(BlockItem::new)
        .model(() -> (ctx, gen) -> gen.createWithExistingModel(ctx.get(), pigeonplus$id("block/pigeon_anvil")))
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.ANVIL)
        .register();

    public static final BlockEntry<BlenderBlock> BLENDER = REGISTRUM
        .block("blender", BlenderBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate(() -> (ctx, gen) -> gen.blockStateOutput.accept(
            MultiVariantGenerator.dispatch(ctx.getEntry(), pigeonplus$variant("block/blender_bottom"))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING)))
        .item(BlockItem::new)
        .model(() -> (ctx, gen) -> gen.createWithExistingModel(ctx.get(), pigeonplus$id("block/blender")))
        .build()
        .register();

    public static final BlockEntry<AnvilPumpBlock> ANVIL_PUMP = REGISTRUM
        .block("anvil_pump", AnvilPumpBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.noOcclusion().sound(SoundType.METAL))
        // 铁砧泵朝向：西=90°、南=0°，同样与 ALT 常量一致
        .blockstate(() -> (ctx, gen) -> gen.blockStateOutput.accept(
            MultiVariantGenerator.dispatch(ctx.getEntry(), pigeonplus$variant("block/anvil_pump"))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING_ALT)))
        .item(BlockItem::new)
        .model(() -> (ctx, gen) -> gen.createWithExistingModel(ctx.get(), pigeonplus$id("block/anvil_pump_full")))
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<FeedSpreaderBlock> FEED_SPREADER = REGISTRUM
        .block("feed_spreader", FeedSpreaderBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.noOcclusion().sound(SoundType.METAL))
        .blockstate(() -> (ctx, gen) -> gen.blockStateOutput.accept(
            BlockModelGenerators.createSimpleBlock(ctx.getEntry(), pigeonplus$variant("block/feed_spreader_bottom"))))
        .item(BlockItem::new)
        .model(() -> (ctx, gen) -> gen.createWithExistingModel(ctx.get(), pigeonplus$id("block/feed_spreader_full")))
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
        // 喷口同时取决于 PART（中心件用完整模型）与 FACING（三轴朝向），
        // 三轴旋转无法用原版常量表达，因此按两个属性自行 dispatch。
        .blockstate(() -> (ctx, gen) -> gen.blockStateOutput.accept(
            MultiVariantGenerator.dispatch(ctx.getEntry()).with(
                PropertyDispatch.initial(NozzleBlock.PART, NozzleBlock.FACING)
                    .generate((part, facing) -> pigeonplus$variant(
                            part == DirectionCube3x3PartHalf.MID_CENTER ? "block/nozzle" : "block/nozzle_part")
                        .with(pigeonplus$nozzleXRot(facing))
                        .with(pigeonplus$nozzleYRot(facing))))))
        .loot(FlexibleMultiPartBlock::loot)
        .item(FlexibleMultiPartBlockItem<DirectionCube3x3PartHalf, EnumProperty<Direction>, Direction>::new)
            .model(() -> (ctx, gen) -> gen.createWithExistingModel(ctx.get(), pigeonplus$id("block/nozzle")))
            .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<StasisBeaconBlock> STASIS_BEACON = REGISTRUM
        .block("stasis_beacon", StasisBeaconBlock::new)
        .initialProperties(() -> Blocks.BEACON)
        .properties(properties -> properties.isValidSpawn(Blocks::never))
        .blockstate(() -> (ctx, gen) -> gen.blockStateOutput.accept(
            BlockModelGenerators.createSimpleBlock(ctx.getEntry(), pigeonplus$variant("block/stasis_beacon"))))
        .item(BlockItem::new)
            .model(() -> (ctx, gen) -> gen.createWithExistingModel(ctx.get(), pigeonplus$id("block/stasis_beacon")))
            .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<MixedBiomassCauldronBlock> MIXED_BIOMASS_CAULDRON = REGISTRUM
        .block("mixed_biomass_cauldron", MixedBiomassCauldronBlock::new)
        .initialProperties(() -> Blocks.CAULDRON)
        // 模型随液面等级变化，按 LEVEL 属性 dispatch
        .blockstate(() -> (ctx, gen) -> gen.blockStateOutput.accept(
            MultiVariantGenerator.dispatch(ctx.getEntry()).with(
                PropertyDispatch.initial(MixedBiomassCauldronBlock.LEVEL).generate(level ->
                    pigeonplus$variant("block/mixed_biomass_cauldron_%s".formatted(level == 4 ? "full" : "level" + level))))))
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.CAULDRONS)
        .onRegister(block -> Item.BY_BLOCK.put(block, Items.CAULDRON))
        .register();

    public static void register() {
    }

    /** 本模组的模型 id。 */
    private static Identifier pigeonplus$id(String path) {
        return Identifier.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, path);
    }

    /** 引用一个已有模型（不再需要 ExistingFileHelper 查找）。 */
    private static MultiVariant pigeonplus$variant(String path) {
        return BlockModelGenerators.plainVariant(pigeonplus$id(path));
    }

    /**
     * 喷口俯仰角：朝下翻 180°、朝上不翻、其余（水平四向）翻 90°。
     */
    private static VariantMutator pigeonplus$nozzleXRot(Direction facing) {
        return switch (facing) {
            case DOWN -> BlockModelGenerators.X_ROT_180;
            case UP -> BlockModelGenerators.NOP;
            default -> BlockModelGenerators.X_ROT_90;
        };
    }

    /**
     * 喷口水平角：上/下/北为基准，其余按象限旋转。
     */
    private static VariantMutator pigeonplus$nozzleYRot(Direction facing) {
        return switch (facing) {
            case UP, DOWN, NORTH -> BlockModelGenerators.NOP;
            case EAST -> BlockModelGenerators.Y_ROT_90;
            case SOUTH -> BlockModelGenerators.Y_ROT_180;
            default -> BlockModelGenerators.Y_ROT_270;
        };
    }
}
