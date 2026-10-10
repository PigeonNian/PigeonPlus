package dev.anvilcraft.pigeonplus.data.recipe;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.pigeonplus.block.LargeFireworksRocketBlock;
import dev.anvilcraft.pigeonplus.block.state.LargeFireworksRocketPart;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.multiblock.LargeFireworksRocketStructure;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LargeFireworksRocketDisplayRecipe {
    private static final int SIZE = 3;
    private static final String OUTPUT_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz123456789]^_`{|}~:;<=>?@#$%&(";

    private static final char FIRST_STAIR_SYMBOL = 'D';

    static final String[][] INPUT_GRID = new String[LargeFireworksRocketStructure.HEIGHT][SIZE];
    static final Map<Character, BlockStatePredicate.Builder> INPUT_MAPPING = new LinkedHashMap<>();
    static final String[][] OUTPUT_GRID = new String[LargeFireworksRocketStructure.HEIGHT][SIZE];
    static final Map<Character, BlockStatePredicate.Builder> OUTPUT_MAPPING = new LinkedHashMap<>();

    static {
        buildInput();
        buildOutput();
    }

    private LargeFireworksRocketDisplayRecipe() {
    }

    private static void buildInput() {
        INPUT_MAPPING.put('A', BlockStatePredicate.builder()
            .of(Blocks.STRIPPED_OAK_LOG)
            .with(BlockStateProperties.AXIS, Direction.Axis.Y));
        INPUT_MAPPING.put('B', BlockStatePredicate.builder().of(AddonBlocks.PAPER_BLOCK.get()));
        INPUT_MAPPING.put('C', BlockStatePredicate.builder().of(ModBlocks.GUNPOWER_BLOCK.get()));

        List<StairVariant> variants = new ArrayList<>();
        for (int y = 0; y < LargeFireworksRocketStructure.HEIGHT; y++) {
            for (int z = 0; z < SIZE; z++) {
                StringBuilder row = new StringBuilder();
                for (int x = 0; x < SIZE; x++) {
                    char symbol = LargeFireworksRocketStructure.PATTERN[y][z].charAt(x);
                    if (!LargeFireworksRocketStructure.isStair(symbol)) {
                        row.append(symbol);
                        continue;
                    }
                    StairVariant variant = new StairVariant(
                        LargeFireworksRocketStructure.baseFacing(symbol),
                        LargeFireworksRocketStructure.expectedShape(x, z)
                    );
                    int index = variants.indexOf(variant);
                    if (index < 0) {
                        index = variants.size();
                        variants.add(variant);
                    }
                    row.append((char) (FIRST_STAIR_SYMBOL + index));
                }
                INPUT_GRID[y][z] = row.toString();
            }
        }
        for (int i = 0; i < variants.size(); i++) {
            INPUT_MAPPING.put((char) (FIRST_STAIR_SYMBOL + i), variants.get(i).builder());
        }
    }

    private static void buildOutput() {
        int index = 0;
        for (int y = 0; y < LargeFireworksRocketStructure.HEIGHT; y++) {
            for (int z = 0; z < SIZE; z++) {
                StringBuilder row = new StringBuilder();
                for (int x = 0; x < SIZE; x++) {
                    LargeFireworksRocketPart part = LargeFireworksRocketPart.findByOffset(x - 1, y, z - 1);
                    if (part == null) {
                        throw new IllegalStateException("零件缺失: " + (x - 1) + "," + y + "," + (z - 1));
                    }
                    char symbol = OUTPUT_ALPHABET.charAt(index++);
                    OUTPUT_MAPPING.put(symbol, BlockStatePredicate.builder()
                        .of(AddonBlocks.LARGE_FIREWORKS_ROCKET.get())
                        .with(LargeFireworksRocketBlock.PART, part)
                        .with(LargeFireworksRocketBlock.FACING, Direction.NORTH));
                    row.append(symbol);
                }
                OUTPUT_GRID[y][z] = row.toString();
            }
        }
    }

    private record StairVariant(Direction facing, StairsShape shape) {
        BlockStatePredicate.Builder builder() {
            return BlockStatePredicate.builder()
                .of(AddonBlocks.PAPER_BLOCK_STAIRS)
                .with(StairBlock.FACING, this.facing)
                .with(StairBlock.HALF, Half.BOTTOM)
                .with(StairBlock.SHAPE, this.shape);
        }
    }
}
