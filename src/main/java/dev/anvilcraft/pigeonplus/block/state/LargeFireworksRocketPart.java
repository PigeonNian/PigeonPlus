package dev.anvilcraft.pigeonplus.block.state;

import dev.dubhe.anvilcraft.block.state.ISimpleMultiPartBlockState;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public enum LargeFireworksRocketPart implements ISimpleMultiPartBlockState<LargeFireworksRocketPart> {

    Y0_WN(-1, 0, -1), Y0_N(0, 0, -1), Y0_EN(1, 0, -1),
    Y0_W(-1, 0, 0), Y0_C(0, 0, 0), Y0_E(1, 0, 0),
    Y0_WS(-1, 0, 1), Y0_S(0, 0, 1), Y0_ES(1, 0, 1),

    Y1_WN(-1, 1, -1), Y1_N(0, 1, -1), Y1_EN(1, 1, -1),
    Y1_W(-1, 1, 0), Y1_C(0, 1, 0), Y1_E(1, 1, 0),
    Y1_WS(-1, 1, 1), Y1_S(0, 1, 1), Y1_ES(1, 1, 1),

    Y2_WN(-1, 2, -1), Y2_N(0, 2, -1), Y2_EN(1, 2, -1),
    Y2_W(-1, 2, 0), Y2_C(0, 2, 0), Y2_E(1, 2, 0),
    Y2_WS(-1, 2, 1), Y2_S(0, 2, 1), Y2_ES(1, 2, 1),

    Y3_WN(-1, 3, -1), Y3_N(0, 3, -1), Y3_EN(1, 3, -1),
    Y3_W(-1, 3, 0), Y3_C(0, 3, 0), Y3_E(1, 3, 0),
    Y3_WS(-1, 3, 1), Y3_S(0, 3, 1), Y3_ES(1, 3, 1),

    Y4_WN(-1, 4, -1), Y4_N(0, 4, -1), Y4_EN(1, 4, -1),
    Y4_W(-1, 4, 0), Y4_C(0, 4, 0), Y4_E(1, 4, 0),
    Y4_WS(-1, 4, 1), Y4_S(0, 4, 1), Y4_ES(1, 4, 1),

    Y5_WN(-1, 5, -1), Y5_N(0, 5, -1), Y5_EN(1, 5, -1),
    Y5_W(-1, 5, 0), Y5_C(0, 5, 0), Y5_E(1, 5, 0),
    Y5_WS(-1, 5, 1), Y5_S(0, 5, 1), Y5_ES(1, 5, 1),

    Y6_WN(-1, 6, -1), Y6_N(0, 6, -1), Y6_EN(1, 6, -1),
    Y6_W(-1, 6, 0), Y6_C(0, 6, 0), Y6_E(1, 6, 0),
    Y6_WS(-1, 6, 1), Y6_S(0, 6, 1), Y6_ES(1, 6, 1),

    Y7_WN(-1, 7, -1), Y7_N(0, 7, -1), Y7_EN(1, 7, -1),
    Y7_W(-1, 7, 0), Y7_C(0, 7, 0), Y7_E(1, 7, 0),
    Y7_WS(-1, 7, 1), Y7_S(0, 7, 1), Y7_ES(1, 7, 1),

    Y8_WN(-1, 8, -1), Y8_N(0, 8, -1), Y8_EN(1, 8, -1),
    Y8_W(-1, 8, 0), Y8_C(0, 8, 0), Y8_E(1, 8, 0),
    Y8_WS(-1, 8, 1), Y8_S(0, 8, 1), Y8_ES(1, 8, 1);

    public static final int HEIGHT = 9;

    private final String name;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private LargeFireworksRocketPart clockwise90;
    private LargeFireworksRocketPart clockwise180;
    private LargeFireworksRocketPart clockwise270;
    private LargeFireworksRocketPart mirrorX;
    private LargeFireworksRocketPart mirrorZ;

    static {
        for (LargeFireworksRocketPart part : LargeFireworksRocketPart.values()) {
            int x = part.offsetX;
            int y = part.offsetY;
            int z = part.offsetZ;
            part.clockwise90 = findByOffset(-z, y, x);
            part.clockwise180 = findByOffset(-x, y, -z);
            part.clockwise270 = findByOffset(z, y, -x);
            part.mirrorX = findByOffset(-x, y, z);
            part.mirrorZ = findByOffset(x, y, -z);
        }
    }

    LargeFireworksRocketPart(int offsetX, int offsetY, int offsetZ) {
        this.name = "y" + offsetY + "_" + horizontalSuffix(offsetX, offsetZ);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    private static String horizontalSuffix(int x, int z) {
        String vertical = z < 0 ? "n" : z > 0 ? "s" : "";
        String horizontal = x < 0 ? "w" : x > 0 ? "e" : "";
        if (vertical.isEmpty() && horizontal.isEmpty()) return "c";
        return horizontal + vertical;
    }

    @Nullable
    public static LargeFireworksRocketPart findByOffset(int offsetX, int offsetY, int offsetZ) {
        return Arrays.stream(LargeFireworksRocketPart.values())
            .filter(part -> part.offsetX == offsetX)
            .filter(part -> part.offsetY == offsetY)
            .filter(part -> part.offsetZ == offsetZ)
            .findFirst()
            .orElse(null);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    @Override
    public int getOffsetX() {
        return this.offsetX;
    }

    @Override
    public int getOffsetY() {
        return this.offsetY;
    }

    @Override
    public int getOffsetZ() {
        return this.offsetZ;
    }

    @Override
    public Vec3i getOffset() {
        return new Vec3i(this.offsetX, this.offsetY, this.offsetZ);
    }

    public String getName() {
        return this.name;
    }

    public LargeFireworksRocketPart rotate(Rotation rotation) {
        LargeFireworksRocketPart result = switch (rotation) {
            case NONE -> this;
            case CLOCKWISE_90 -> this.clockwise90;
            case CLOCKWISE_180 -> this.clockwise180;
            case COUNTERCLOCKWISE_90 -> this.clockwise270;
        };
        return result == null ? this : result;
    }

    public LargeFireworksRocketPart mirror(Mirror mirror) {
        LargeFireworksRocketPart result = switch (mirror) {
            case NONE -> this;
            case LEFT_RIGHT -> this.mirrorZ;
            case FRONT_BACK -> this.mirrorX;
        };
        return result == null ? this : result;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public int getSegment() {
        return this.offsetY / 3;
    }

    public boolean isModelAnchor() {
        return this.offsetX == 0 && this.offsetZ == 0 && this.offsetY % 3 == 1;
    }

    public String getModelName() {
        if (!this.isModelAnchor()) {
            return "part";
        }
        return switch (this.getSegment()) {
            case 0 -> "bottom";
            case 1 -> "middle";
            default -> "top";
        };
    }

    public boolean isMainPart() {
        return this.offsetX == 0 && this.offsetY == 0 && this.offsetZ == 0;
    }
}
