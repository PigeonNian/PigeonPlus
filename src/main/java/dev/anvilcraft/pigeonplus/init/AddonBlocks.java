package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.anvilcraft.pigeonplus.block.MixedBiomassCauldronBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
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
}
