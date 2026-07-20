package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
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
