package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.anvilcraft.pigeonplus.block.state.LargeCreamyBreadRollPart;
import dev.anvilcraft.pigeonplus.init.ModCriterionTriggers;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class LargeCreamyBreadRollBlock
    extends FlexibleMultiPartBlock<LargeCreamyBreadRollPart, DirectionProperty, Direction>
    implements IHammerRemovable {
    public static final MapCodec<LargeCreamyBreadRollBlock> CODEC = simpleCodec(LargeCreamyBreadRollBlock::new);
    public static final EnumProperty<LargeCreamyBreadRollPart> PART =
        EnumProperty.create("part", LargeCreamyBreadRollPart.class);
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty CONNECTED_FRONT = BooleanProperty.create("connected_front");
    public static final BooleanProperty CONNECTED_BACK = BooleanProperty.create("connected_back");
    private static final int FOOD_LEVEL = 15;
    private static final float SATURATION_LEVEL = 0.8F;

    private static final AABB[] GLOBAL_SHAPE = {
        // 奶油芯：贯穿整卷
        new AABB(-0.28125, -1, 0.0625, 1.28125, 2, 0.9375),
        new AABB(-1, -0.28125, 0.0625, 2, 1.28125, 0.9375),
        // 四角的小方块
        new AABB(-0.71875, 1.28125, 0.0625, -0.28125, 1.71875, 0.9375),
        new AABB(1.28125, 1.28125, 0.0625, 1.71875, 1.71875, 0.9375),
        new AABB(1.28125, -0.71875, 0.0625, 1.71875, -0.28125, 0.9375),
        new AABB(-0.71875, -0.71875, 0.0625, -0.28125, -0.28125, 0.9375),
    };

    private static final Map<Direction, Map<LargeCreamyBreadRollPart, VoxelShape>> SHAPES_BY_FACING =
        createShapesByFacing();

    public LargeCreamyBreadRollBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(PART, LargeCreamyBreadRollPart.X1_Y1_Z0)
            .setValue(FACING, Direction.NORTH)
            .setValue(CONNECTED_FRONT, false)
            .setValue(CONNECTED_BACK, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONNECTED_FRONT, CONNECTED_BACK);
    }

    @Override
    public EnumProperty<LargeCreamyBreadRollPart> getPart() {
        return PART;
    }

    @Override
    public LargeCreamyBreadRollPart[] getParts() {
        return LargeCreamyBreadRollPart.values();
    }

    @Override
    public DirectionProperty getAdditionalProperty() {
        return FACING;
    }

    public static Direction frontFacing(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.SOUTH;
            case SOUTH -> Direction.NORTH;
            case EAST -> Direction.WEST;
            case WEST -> Direction.EAST;
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(
                FACING,
                context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                    ? context.getNearestLookingDirection().getOpposite()
                    : context.getNearestLookingDirection()
            );
    }

    @Override
    public BlockState getPlacementState(BlockPlaceContext context) {
        return this.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(
        Level level,
        BlockPos pos,
        BlockState state,
        @Nullable LivingEntity placer,
        ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        for (LargeCreamyBreadRollPart part : this.getParts()) {
            BlockPos partPos = pos.offset(this.offsetFrom(state, part));
            BlockState partState = level.getBlockState(partPos);
            if (partState.is(this)) {
                level.setBlockAndUpdate(partPos, this.withConnections(level, partPos, partState));
            }
        }
    }

    @Override
    public BlockState updateShape(
        BlockState state,
        Direction directionToNeighbour,
        BlockState neighbourState,
        LevelAccessor level,
        BlockPos pos,
        BlockPos neighbourPos
    ) {
        BlockState updated = super.updateShape(
            state, directionToNeighbour, neighbourState, level, pos, neighbourPos);
        if (!updated.is(this)) {
            return updated;
        }
        Direction front = frontFacing(state.getValue(FACING));
        if (directionToNeighbour == front) {
            return updated.setValue(CONNECTED_FRONT, this.connects(pos, updated, front, neighbourState));
        }
        if (directionToNeighbour == front.getOpposite()) {
            return updated.setValue(
                CONNECTED_BACK, this.connects(pos, updated, front.getOpposite(), neighbourState));
        }
        return updated;
    }

    private BlockState withConnections(Level level, BlockPos pos, BlockState state) {
        Direction front = frontFacing(state.getValue(FACING));
        return state
            .setValue(
                CONNECTED_FRONT,
                this.connects(pos, state, front, level.getBlockState(pos.relative(front))))
            .setValue(
                CONNECTED_BACK,
                this.connects(pos, state, front.getOpposite(), level.getBlockState(pos.relative(front.getOpposite()))));
    }

    private boolean connects(
        BlockPos pos,
        BlockState state,
        Direction direction,
        BlockState neighbourState
    ) {
        if (!neighbourState.is(this) || neighbourState.getValue(FACING) != state.getValue(FACING)) {
            return false;
        }
        BlockPos mainPos = this.getMainPartPos(pos, state);
        BlockPos neighbourMainPos = this.getMainPartPos(pos.relative(direction), neighbourState);
        return mainPos.relative(direction).equals(neighbourMainPos);
    }

    public static String getModelName(BlockState state) {
        if (!state.getValue(PART).isMain()) {
            return "large_creamy_bread_roll_part";
        }
        boolean front = state.getValue(CONNECTED_FRONT);
        boolean back = state.getValue(CONNECTED_BACK);
        if (front && back) {
            return "large_creamy_bread_roll_part2";
        }
        if (front) {
            return "large_creamy_bread_roll_part1";
        }
        if (back) {
            return "large_creamy_bread_roll_part3";
        }
        return "large_creamy_bread_roll";
    }

    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.getItem().canPerformAction(stack, ItemAbilities.SHOVEL_DIG)) {
            return InteractionResult.PASS;
        }
        if (!player.canEat(false)) {
            return InteractionResult.PASS;
        }
        player.getFoodData().eat(FOOD_LEVEL, SATURATION_LEVEL);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        level.playSound(
            null,
            pos,
            SoundEvents.GENERIC_EAT,
            SoundSource.PLAYERS,
            1.0F,
            1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.4F
        );
        List<BlockPos> parts = new ArrayList<>();
        this.forEachPart(level, pos, parts::add);
        for (BlockPos part : parts) {
            level.setBlock(
                part,
                Blocks.AIR.defaultBlockState(),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
            );
        }
        this.refreshNeighbourConnections(level, parts);
        level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
        stack.hurtAndBreak(1, player, PlayerUtil.handToSlot(hand));
        if (player instanceof ServerPlayer serverPlayer) {
            ModCriterionTriggers.BREAD_ROLL_EATEN.get().trigger(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    private void refreshNeighbourConnections(Level level, List<BlockPos> parts) {
        for (BlockPos part : parts) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbour = part.relative(direction);
                BlockState neighbourState = level.getBlockState(neighbour);
                if (!neighbourState.is(this)) {
                    continue;
                }
                level.setBlockAndUpdate(neighbour, this.withConnections(level, neighbour, neighbourState));
            }
        }
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public VoxelShape getPartShape(BlockState state) {
        if (!state.hasProperty(PART) || !state.hasProperty(FACING)) {
            return Shapes.block();
        }
        return SHAPES_BY_FACING
            .getOrDefault(state.getValue(FACING), Map.of())
            .getOrDefault(state.getValue(PART), Shapes.block());
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(PART, state.getValue(PART).rotate(rotation))
            .setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(PART, state.getValue(PART).mirror(mirror))
            .setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    private static Map<Direction, Map<LargeCreamyBreadRollPart, VoxelShape>> createShapesByFacing() {
        Map<Direction, Map<LargeCreamyBreadRollPart, VoxelShape>> byFacing = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.values()) {
            Map<LargeCreamyBreadRollPart, VoxelShape> shapes = new EnumMap<>(LargeCreamyBreadRollPart.class);
            for (LargeCreamyBreadRollPart part : LargeCreamyBreadRollPart.values()) {
                shapes.put(part, slicePartShape(part, facing));
            }
            byFacing.put(facing, shapes);
        }
        return byFacing;
    }

    private static VoxelShape slicePartShape(LargeCreamyBreadRollPart part, Direction facing) {
        double minX = part.getOffsetX(facing);
        double minY = part.getOffsetY(facing);
        double minZ = part.getOffsetZ(facing);
        double maxX = minX + 1.0;
        double maxY = minY + 1.0;
        double maxZ = minZ + 1.0;
        List<AABB> localParts = new ArrayList<>();
        for (AABB source : GLOBAL_SHAPE) {
            AABB globalPart = rotateShape(source, facing);
            double clippedMinX = Math.max(globalPart.minX, minX);
            double clippedMinY = Math.max(globalPart.minY, minY);
            double clippedMinZ = Math.max(globalPart.minZ, minZ);
            double clippedMaxX = Math.min(globalPart.maxX, maxX);
            double clippedMaxY = Math.min(globalPart.maxY, maxY);
            double clippedMaxZ = Math.min(globalPart.maxZ, maxZ);
            if (clippedMinX >= clippedMaxX || clippedMinY >= clippedMaxY || clippedMinZ >= clippedMaxZ) {
                continue;
            }
            // 方块空间 -> 该部件的局部坐标 -> 像素（ShapeUtil 的 16 格制）。
            localParts.add(new AABB(
                (clippedMinX - minX) * 16.0, (clippedMinY - minY) * 16.0, (clippedMinZ - minZ) * 16.0,
                (clippedMaxX - minX) * 16.0, (clippedMaxY - minY) * 16.0, (clippedMaxZ - minZ) * 16.0
            ));
        }
        return localParts.isEmpty() ? Shapes.empty() : ShapeUtil.merge(localParts.toArray(AABB[]::new));
    }

    private static AABB rotateShape(AABB box, Direction facing) {
        AABB rotated = switch (facing) {
            // 与 LargeFireworksRocketBlock#rotateShape 同一套约定（模型 y=90 把局部 -Z 转到 +X）。
            case EAST -> new AABB(1.0 - box.maxZ, box.minY, box.minX, 1.0 - box.minZ, box.maxY, box.maxX);
            case SOUTH -> new AABB(1.0 - box.maxX, box.minY, 1.0 - box.maxZ, 1.0 - box.minX, box.maxY, 1.0 - box.minZ);
            case WEST -> new AABB(box.minZ, box.minY, 1.0 - box.maxX, box.maxZ, box.maxY, 1.0 - box.minX);
            default -> box;
        };
        return switch (facing) {
            // up: 模型 x=+90，把局部 +Z 转到 -Y；绕 X 轴转 90° 即 (y,z) -> (1-z, y)。
            case UP -> new AABB(rotated.minX, 1.0 - rotated.maxZ, rotated.minY,
                                rotated.maxX, 1.0 - rotated.minZ, rotated.maxY);
            // down: 模型 x=270，等价于绕 X 轴 -90°：(y,z) -> (z, 1-y)。
            case DOWN -> new AABB(rotated.minX, rotated.minZ, 1.0 - rotated.maxY,
                                  rotated.maxX, rotated.maxZ, 1.0 - rotated.minY);
            default -> rotated;
        };
    }
}
