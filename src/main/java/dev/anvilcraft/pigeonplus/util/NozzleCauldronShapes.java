package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.pigeonplus.block.NozzleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 大炼药锅在装了喷口之后，顶部/底部会多出一圈「盖板」模型。
 *
 * <p>这些额外模型原先只有渲染，没有碰撞——因为碰撞走的是
 * {@code AbstractMultiPartBlock#getCollisionShape}，而那个方法直接取
 * {@code getPartShape}，<strong>完全不经过 {@code getShape}</strong>。
 * 只改 {@code getShape} 的话，模型出来了但撞不上去。
 *
 * <p>这里集中定义这些盖板的碰撞形状，供两处使用：
 * <ul>
 *   <li>{@code AbstractMultiPartBlockMixin}——补实体碰撞（走 {@code getCollisionShape}）；</li>
 *   <li>{@code LargeCauldronBlockMixin}——补准星选中轮廓（走 {@code getShape}）。</li>
 * </ul>
 *
 * <h3>坐标空间</h3>
 * 形状用<strong>单格局部坐标</strong>（0..16），与 {@code getShape} / {@code getPartShape}
 * 的约定一致。以顶盖为例，模型里写的是 {@code y 28..32}（以 3x3 中心块为原点的跨格坐标），
 * 换算到顶层那一格就是 {@code y 12..16}：
 * <pre>
 *   顶层格的基准 Y = (main + 1) * 16
 *   模型 Y 相对 main 为 28..32
 *   => 局部 = 28 - 16 .. 32 - 16 = 12..16   ✓
 * </pre>
 */
public final class NozzleCauldronShapes {
    /** 喷口主体相对炼药锅 main part 的偏移格数（main 向喷口方向数 3 格）。 */
    private static final int NOZZLE_OFFSET = NozzleExhaustUtil.NOZZLE_MAIN_OFFSET_Y;

    // ---------------------------------------------------------------- 顶盖

    private static final VoxelShape LID_CENTER = Block.box(0.0, 12.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape LID_W = Block.box(4.0, 12.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape LID_E = Block.box(0.0, 12.0, 0.0, 12.0, 16.0, 16.0);
    private static final VoxelShape LID_N = Block.box(0.0, 12.0, 4.0, 16.0, 16.0, 16.0);
    private static final VoxelShape LID_S = Block.box(0.0, 12.0, 0.0, 16.0, 16.0, 12.0);
    private static final VoxelShape LID_WN = Block.box(4.0, 12.0, 4.0, 16.0, 16.0, 16.0);
    private static final VoxelShape LID_WS = Block.box(4.0, 12.0, 0.0, 16.0, 16.0, 12.0);
    private static final VoxelShape LID_EN = Block.box(0.0, 12.0, 4.0, 12.0, 16.0, 16.0);
    private static final VoxelShape LID_ES = Block.box(0.0, 12.0, 0.0, 12.0, 16.0, 12.0);

    // ---------------------------------------------------------------- 底盖

    private static final VoxelShape BOTTOM_CENTER = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);
    private static final VoxelShape BOTTOM_W = Block.box(11.0, 0.0, 0.0, 16.0, 4.0, 16.0);
    private static final VoxelShape BOTTOM_E = Block.box(0.0, 0.0, 0.0, 5.0, 4.0, 16.0);
    private static final VoxelShape BOTTOM_N = Block.box(0.0, 0.0, 11.0, 16.0, 4.0, 16.0);
    private static final VoxelShape BOTTOM_S = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 5.0);
    private static final VoxelShape BOTTOM_WN = Block.box(11.0, 0.0, 11.0, 16.0, 4.0, 16.0);
    private static final VoxelShape BOTTOM_WS = Block.box(11.0, 0.0, 0.0, 16.0, 4.0, 5.0);
    private static final VoxelShape BOTTOM_EN = Block.box(0.0, 0.0, 11.0, 5.0, 4.0, 16.0);
    private static final VoxelShape BOTTOM_ES = Block.box(0.0, 0.0, 0.0, 5.0, 4.0, 5.0);

    private NozzleCauldronShapes() {
    }

    /**
     * 判断该喷口朝向是否与炼药锅相接。
     *
     * <p>喷口是独立的 3x3 结构，装在锅外侧：水平方向数 3 格、或正下方 3 格。
     * 同时校验朝向（喷口要背对锅），避免把「恰好放在附近但朝向无关」的喷口算进来。
     */
    public static boolean hasNozzleAt(Level level, BlockPos cauldronMainPos, Direction direction) {
        BlockPos nozzlePos = cauldronMainPos.relative(direction, NOZZLE_OFFSET);
        BlockState state = level.getBlockState(nozzlePos);
        return state.getBlock() instanceof NozzleBlock nozzle
            && nozzle.isMainPart(state)
            && state.getValue(NozzleBlock.FACING) == direction;
    }

    /** 是否有水平方向的喷口。 */
    public static boolean hasHorizontalNozzle(Level level, BlockPos cauldronMainPos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (hasNozzleAt(level, cauldronMainPos, direction)) {
                return true;
            }
        }
        return false;
    }

    /** 是否有向下的喷口。 */
    public static boolean hasBottomNozzle(Level level, BlockPos cauldronMainPos) {
        return hasNozzleAt(level, cauldronMainPos, Direction.DOWN);
    }

    /**
     * 求该部件需要补的额外形状。
     *
     * <p>规则：
     * <ul>
     *   <li>顶层（offsetY == 2）且存在水平或向下喷口 → 加顶盖；</li>
     *   <li>底层（offsetY == 0）且存在向下喷口 → 加底盖。</li>
     * </ul>
     * 两者是并列的独立条件，不是 else-if：向下喷口会同时让顶盖与底盖出现。
     *
     * @param partYOffset 部件的 Y 层偏移（{@code Cube3x3PartHalf#getOffsetY()}）
     */
    public static VoxelShape attachmentShape(
        BlockGetter level,
        BlockPos partPos,
        BlockPos cauldronMainPos,
        int partYOffset,
        String partName
    ) {
        if (!(level instanceof Level realLevel)) {
            return Shapes.empty();
        }
        if (partYOffset == 2
            && (hasHorizontalNozzle(realLevel, cauldronMainPos) || hasBottomNozzle(realLevel, cauldronMainPos))) {
            return lidShape(partName);
        }
        if (partYOffset == 0 && hasBottomNozzle(realLevel, cauldronMainPos)) {
            return bottomShape(partName);
        }
        return Shapes.empty();
    }

    private static VoxelShape lidShape(String partName) {
        return switch (partName) {
            case "TOP_CENTER" -> LID_CENTER;
            case "TOP_W" -> LID_W;
            case "TOP_E" -> LID_E;
            case "TOP_N" -> LID_N;
            case "TOP_S" -> LID_S;
            case "TOP_WN" -> LID_WN;
            case "TOP_WS" -> LID_WS;
            case "TOP_EN" -> LID_EN;
            case "TOP_ES" -> LID_ES;
            default -> Shapes.empty();
        };
    }

    private static VoxelShape bottomShape(String partName) {
        return switch (partName) {
            case "BOTTOM_CENTER" -> BOTTOM_CENTER;
            case "BOTTOM_W" -> BOTTOM_W;
            case "BOTTOM_E" -> BOTTOM_E;
            case "BOTTOM_N" -> BOTTOM_N;
            case "BOTTOM_S" -> BOTTOM_S;
            case "BOTTOM_WN" -> BOTTOM_WN;
            case "BOTTOM_WS" -> BOTTOM_WS;
            case "BOTTOM_EN" -> BOTTOM_EN;
            case "BOTTOM_ES" -> BOTTOM_ES;
            default -> Shapes.empty();
        };
    }
}
