package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.LargeFireworksRocketBlock;
import dev.anvilcraft.pigeonplus.block.state.LargeFireworksRocketPart;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.multiblock.LargeFireworksRocketStructure;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.event.GiantAnvilEvent;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class LargeFireworksRocketConversionEventListener {
    private static final int HEIGHT = LargeFireworksRocketStructure.HEIGHT;
    private static final int HALF = LargeFireworksRocketStructure.HALF;
    private static final String[][] PATTERN = LargeFireworksRocketStructure.PATTERN;

    private static final int FIN_LAYER = 7;

    private LargeFireworksRocketConversionEventListener() {
    }

    @SubscribeEvent
    public static void onGiantAnvilMultiblock(GiantAnvilEvent.Multiblock event) {
        AnvilEvent.GiantOnLand landing = event.getLanding();
        Level level = landing.getLevel();
        if (level.isClientSide()) {
            return;
        }
        BlockPos centerPos = landing.getPos().below(2);
        BlockState center = level.getBlockState(centerPos);

        boolean craft = center.is(ModBlocks.SPACE_OVERCOMPRESSOR.get());
        if (!craft && !center.is(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES)) {
            return;
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (!matches(level, centerPos, rotationFor(facing))) {
                continue;
            }
            if (craft) {
                craftItem(level, centerPos);
            } else {
                placeFirework(level, centerPos, facing);
            }

            event.setCanceled(true);
            return;
        }
    }

    private static boolean matches(Level level, BlockPos centerPos, Rotation rotation) {
        BlockPos mainPos = mainPos(centerPos);
        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    char symbol = PATTERN[y][z].charAt(x);
                    Vec3i offset = rotateOffset(rotation, x - HALF, y, z - HALF);
                    BlockPos pos = mainPos.offset(offset);
                    BlockState actual = level.getBlockState(pos);
                    if (symbol == ' ') {
                        if (!actual.isAir()) {
                            return false;
                        }
                    } else if (isStair(symbol)) {
                        if (!matchesStair(actual, rotation, symbol, x, z)) {
                            return false;
                        }
                    } else if (!accepts(symbol, actual)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isStair(char symbol) {
        return LargeFireworksRocketStructure.isStair(symbol);
    }

    private static boolean matchesStair(BlockState actual, Rotation rotation, char symbol, int x, int z) {
        if (!actual.is(AddonBlocks.PAPER_BLOCK_STAIRS.get())
            || actual.getValue(StairBlock.HALF) != Half.BOTTOM) {
            return false;
        }
        Direction expected = LargeFireworksRocketStructure.appearance(
            rotation.rotate(LargeFireworksRocketStructure.baseFacing(symbol)),
            LargeFireworksRocketStructure.expectedShape(x, z)
        );
        Direction got = LargeFireworksRocketStructure.appearance(
            actual.getValue(StairBlock.FACING), actual.getValue(StairBlock.SHAPE)
        );
        return expected == got;
    }

    private static boolean accepts(char symbol, BlockState actual) {
        return switch (symbol) {
            case 'A' -> actual.is(Blocks.STRIPPED_OAK_LOG)
                && actual.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y;
            case 'B' -> actual.is(AddonBlocks.PAPER_BLOCK.get());
            case 'C' -> actual.is(ModBlocks.GUNPOWER_BLOCK.get());
            default -> throw new IllegalStateException("未知符号: " + symbol);
        };
    }

    private static void placeFirework(Level level, BlockPos centerPos, Direction facing) {
        BlockPos mainPos = mainPos(centerPos);
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (LargeFireworksRocketPart part : LargeFireworksRocketPart.values()) {
            BlockState state = AddonBlocks.LARGE_FIREWORKS_ROCKET.get().defaultBlockState()
                .setValue(LargeFireworksRocketBlock.PART, part)
                .setValue(LargeFireworksRocketBlock.FACING, facing);
            level.setBlock(mainPos.offset(part.getOffset()), state, flags);
        }
        for (BlockPos pos : fireworkBounds(mainPos)) {
            level.updateNeighborsAt(pos, AddonBlocks.LARGE_FIREWORKS_ROCKET.get());
        }
        playSound(level, centerPos);
    }

    private static void craftItem(Level level, BlockPos centerPos) {
        BlockPos mainPos = mainPos(centerPos);
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    if (PATTERN[y][z].charAt(x) != ' ') {
                        level.setBlock(mainPos.offset(x - HALF, y, z - HALF), air, flags);
                    }
                }
            }
        }

        AnvilUtil.dropItems(
            List.of(new ItemStack(AddonBlocks.LARGE_FIREWORKS_ROCKET)),
            level,
            centerPos.relative(Direction.Axis.Y, -HEIGHT / 2).getCenter()
        );
        playSound(level, centerPos);
    }

    private static void playSound(Level level, BlockPos centerPos) {
        level.playSound(null, centerPos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static Iterable<BlockPos> fireworkBounds(BlockPos mainPos) {
        return BlockPos.betweenClosed(
            mainPos.offset(-HALF, 0, -HALF),
            mainPos.offset(HALF, HEIGHT - 1, HALF)
        );
    }

    private static BlockPos mainPos(BlockPos centerPos) {
        return centerPos.below(HEIGHT);
    }

    private static Rotation rotationFor(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static Vec3i rotateOffset(Rotation rotation, int x, int y, int z) {
        return switch (rotation) {
            case CLOCKWISE_90 -> new Vec3i(-z, y, x);
            case CLOCKWISE_180 -> new Vec3i(-x, y, -z);
            case COUNTERCLOCKWISE_90 -> new Vec3i(z, y, -x);
            default -> new Vec3i(x, y, z);
        };
    }
}
