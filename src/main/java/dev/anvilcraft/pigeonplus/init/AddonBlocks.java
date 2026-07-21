package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.AnvilPumpBlock;
import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.anvilcraft.pigeonplus.block.MixedBiomassCauldronBlock;
import dev.anvilcraft.pigeonplus.block.NozzleBlock;
import dev.dubhe.anvilcraft.block.item.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;

import static dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus.REGISTRUM;

public class AddonBlocks {
    static {
        REGISTRUM.defaultCreativeTab(AddonItemGroups.ADDON_ITEMS.getKey());
    }

    public static final BlockEntry<BlenderBlock> BLENDER = REGISTRUM
        .block("blender", BlenderBlock::new)
        .blockstate((ctx, prov) -> prov.getVariantBuilder(ctx.getEntry()).forAllStates(state ->
            ConfiguredModel.builder()
                .modelFile(prov.models().getExistingFile(
                    ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/blender_bottom")))
                .rotationY(rotationY(state.getValue(BlenderBlock.FACING)))
                .build()))
        .item((block, props) -> new BlockItem(block, props))
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(),
                ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/blender")))
        .build()
        .register();

    public static final BlockEntry<NozzleBlock> NOZZLE = REGISTRUM
        .block("nozzle", NozzleBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.noOcclusion().sound(SoundType.METAL))
        .blockstate((ctx, provider) -> provider.getVariantBuilder(ctx.getEntry()).forAllStates(state ->
            ConfiguredModel.builder()
                .modelFile(provider.models().getExistingFile(ResourceLocation.fromNamespaceAndPath(
                    AnvilCraftPigeonPlus.MOD_ID,
                    state.getValue(NozzleBlock.PART) == Cube3x3PartHalf.MID_CENTER
                        ? "block/nozzle"
                        : "block/nozzle_part"
                )))
                .build()))
        .loot((tables, block) -> SimpleMultiPartBlock.loot(tables, block))
        .item(SimpleMultiPartBlockItem<Cube3x3PartHalf>::new)
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(),
                ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/nozzle")))
            .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<AnvilPumpBlock> ANVIL_PUMP = REGISTRUM
        .block("anvil_pump", AnvilPumpBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.noOcclusion().sound(SoundType.METAL))
        .blockstate((ctx, provider) -> provider.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
            return ConfiguredModel.builder()
                .modelFile(provider.models().getExistingFile(ResourceLocation.fromNamespaceAndPath(
                    AnvilCraftPigeonPlus.MOD_ID,
                    "block/anvil_pump"
                )))
                .rotationY(pumpRotationY(state.getValue(AnvilPumpBlock.FACING)))
                .build();
        }))
        .item((block, props) -> new BlockItem(block, props))
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(),
                ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "block/anvil_pump_full")))
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
}
