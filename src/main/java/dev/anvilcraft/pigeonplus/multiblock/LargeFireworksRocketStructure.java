package dev.anvilcraft.pigeonplus.multiblock;

import dev.anvilcraft.pigeonplus.block.state.LargeFireworksRocketPart;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.StairsShape;

public final class LargeFireworksRocketStructure {

    public static final int HEIGHT = LargeFireworksRocketPart.HEIGHT;

    public static final int HALF = 1;

    public static final int FIN_LAYER = 7;
    public static final String[][] PATTERN = {
        {"   ", "   ", "  A"},
        {"   ", "   ", "  A"},
        {"BBB", "BCB", "BBB"},
        {"BBB", "BCB", "BBB"},
        {"BBB", "BCB", "BBB"},
        {"BBB", "BCB", "BBB"},
        {"BBB", "BCB", "BBB"},
        {"DDD", "ECF", "EGG"},
        {"   ", " B ", "   "},
    };

    private static final StairsShape[][] EXPECTED_SHAPES = buildExpectedShapes();

    private LargeFireworksRocketStructure() {
    }

    public static StairsShape expectedShape(int x, int z) {
        return EXPECTED_SHAPES[x][z];
    }

    public static Direction baseFacing(char symbol) {
        return switch (symbol) {
            case 'D' -> Direction.SOUTH;
            case 'E' -> Direction.EAST;
            case 'F' -> Direction.WEST;
            case 'G' -> Direction.NORTH;
            default -> throw new IllegalStateException("非楼梯符号: " + symbol);
        };
    }

    public static boolean isStair(char symbol) {
        return symbol == 'D' || symbol == 'E' || symbol == 'F' || symbol == 'G';
    }

    public static Direction appearance(Direction facing, StairsShape shape) {
        return switch (shape) {
            case INNER_LEFT, OUTER_LEFT -> facing.getCounterClockWise();
            default -> facing;
        };
    }

    private static StairsShape[][] buildExpectedShapes() {
        StairsShape[][] shapes = new StairsShape[3][3];
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                if (isStair(PATTERN[FIN_LAYER][z].charAt(x))) {
                    shapes[x][z] = computeShape(x, z);
                }
            }
        }
        return shapes;
    }

    private static StairsShape computeShape(int x, int z) {
        Direction dir = baseFacing(PATTERN[FIN_LAYER][z].charAt(x));
        Direction front = facingAt(x + dir.getStepX(), z + dir.getStepZ());
        if (front != null && front.getAxis() != dir.getAxis()
            && canTakeShape(x, z, dir, front.getOpposite())) {
            return front == dir.getCounterClockWise()
                ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT;
        }
        Direction backDir = dir.getOpposite();
        Direction back = facingAt(x + backDir.getStepX(), z + backDir.getStepZ());
        if (back != null && back.getAxis() != dir.getAxis()
            && canTakeShape(x, z, dir, back)) {
            return back == dir.getCounterClockWise()
                ? StairsShape.INNER_LEFT : StairsShape.INNER_RIGHT;
        }
        return StairsShape.STRAIGHT;
    }

    private static Direction facingAt(int x, int z) {
        if (x < 0 || x > 2 || z < 0 || z > 2) {
            return null;
        }
        char symbol = PATTERN[FIN_LAYER][z].charAt(x);
        return isStair(symbol) ? baseFacing(symbol) : null;
    }

    private static boolean canTakeShape(int x, int z, Direction dir, Direction face) {
        Direction neighbour = facingAt(x + face.getStepX(), z + face.getStepZ());
        return neighbour == null || neighbour != dir;
    }
}
