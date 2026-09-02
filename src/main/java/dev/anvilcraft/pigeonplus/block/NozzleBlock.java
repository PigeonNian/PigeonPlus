package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.anvilcraft.pigeonplus.block.entity.ModBlockEntities;
import dev.anvilcraft.pigeonplus.block.entity.NozzleExhaustBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class NozzleBlock extends FlexibleMultiPartBlock<DirectionCube3x3PartHalf, DirectionProperty, Direction> implements EntityBlock {
    public static final MapCodec<NozzleBlock> CODEC = simpleCodec(NozzleBlock::new);
    public static final EnumProperty<DirectionCube3x3PartHalf> PART =
        EnumProperty.create("part", DirectionCube3x3PartHalf.class);
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    private static final AABB[] UP_GLOBAL_SHAPE_PARTS = {
        new AABB(-9.0, -11.0, -9.0, 25.0, -6.0, 25.0),
        new AABB(-14.0, -16.0, -14.0, 30.0, -11.0, 30.0),
        new AABB(-5.0, -6.0, -5.0, 21.0, 2.0, 21.0),
        new AABB(-8.0, 2.0, -8.0, 24.0, 10.0, 24.0),
        new AABB(-14.0, 10.0, -14.0, 30.0, 17.0, 30.0),
        new AABB(-16.0, 26.0, -16.0, 32.0, 32.0, -12.0),
        new AABB(-14.0, 22.0, -12.0, -12.0, 26.0, 28.0),
        new AABB(-16.0, 16.0, -12.0, -12.0, 22.0, 28.0),
        new AABB(28.0, 16.0, -12.0, 32.0, 22.0, 28.0),
        new AABB(-16.0, 26.0, -12.0, -12.0, 32.0, 28.0),
        new AABB(-16.0, 16.0, 28.0, 32.0, 22.0, 32.0),
        new AABB(-16.0, 26.0, 28.0, 32.0, 32.0, 32.0),
        new AABB(-16.0, 16.0, -16.0, 32.0, 22.0, -12.0),
        new AABB(0.0, 13.0, -16.0, 16.0, 26.0, -14.0),
        new AABB(0.0, 13.0, 30.0, 16.0, 26.0, 32.0),
        new AABB(-16.0, 13.0, 0.0, -14.0, 26.0, 16.0),
        new AABB(30.0, 13.0, 0.0, 32.0, 26.0, 16.0),
        new AABB(28.0, 26.0, -12.0, 32.0, 32.0, 28.0),
        new AABB(28.0, 22.0, -12.0, 30.0, 26.0, 28.0),
        new AABB(-14.0, 22.0, -14.0, 30.0, 26.0, -12.0),
        new AABB(-14.0, 22.0, 28.0, 30.0, 26.0, 30.0),
        new AABB(-10.0, -10.99, -10.0, 26.0, -5.0, 26.0),
    };
    private static final Map<Direction, Map<DirectionCube3x3PartHalf, VoxelShape>> SHAPES = createShapes();

    public NozzleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(PART, DirectionCube3x3PartHalf.BOTTOM_CENTER)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getPartShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getPartShape(state);
    }

    @Override
    public EnumProperty<DirectionCube3x3PartHalf> getPart() {
        return PART;
    }

    @Override
    public DirectionCube3x3PartHalf[] getParts() {
        return DirectionCube3x3PartHalf.values();
    }

    @Override
    public DirectionProperty getAdditionalProperty() {
        return FACING;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(
                FACING,
                context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                    ? context.getNearestLookingDirection().getOpposite()
                    : context.getNearestLookingDirection()
            );
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

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, net.minecraft.core.BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, net.minecraft.core.BlockPos pos) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.isMainPart(state) ? new NozzleExhaustBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        if (!this.isMainPart(state) || blockEntityType != ModBlockEntities.NOZZLE_EXHAUST.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) ->
            NozzleExhaustBlockEntity.tick(tickerLevel, pos, tickerState, (NozzleExhaustBlockEntity) blockEntity);
    }

    @Override
    public VoxelShape getPartShape(BlockState state) {
        return SHAPES.getOrDefault(state.getValue(FACING), SHAPES.get(Direction.UP))
            .getOrDefault(state.getValue(PART), Shapes.block());
    }

    private static Map<Direction, Map<DirectionCube3x3PartHalf, VoxelShape>> createShapes() {
        Map<Direction, Map<DirectionCube3x3PartHalf, VoxelShape>> shapes = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.values()) {
            AABB[] rotated = rotateGlobalParts(facing);
            Map<DirectionCube3x3PartHalf, VoxelShape> partShapes = new EnumMap<>(DirectionCube3x3PartHalf.class);
            for (DirectionCube3x3PartHalf part : DirectionCube3x3PartHalf.values()) {
                partShapes.put(part, slicePartShape(rotated, part));
            }
            shapes.put(facing, partShapes);
        }
        return shapes;
    }

    private static AABB[] rotateGlobalParts(Direction facing) {
        float xRotation = switch (facing) {
            case DOWN -> 180.0F;
            case UP -> 0.0F;
            default -> 90.0F;
        };
        float yRotation = switch (facing) {
            case UP, DOWN, NORTH -> 0.0F;
            case EAST -> 270.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
        };
        AABB[] rotated = new AABB[UP_GLOBAL_SHAPE_PARTS.length];
        for (int i = 0; i < UP_GLOBAL_SHAPE_PARTS.length; i++) {
            rotated[i] = rotateGlobalBox(UP_GLOBAL_SHAPE_PARTS[i], xRotation, yRotation);
        }
        return rotated;
    }

    private static VoxelShape slicePartShape(AABB[] globalParts, DirectionCube3x3PartHalf part) {
        double minX = part.getOffsetX() * 16.0;
        double minY = (part.getOffsetY() - 1) * 16.0;
        double minZ = part.getOffsetZ() * 16.0;
        double maxX = minX + 16.0;
        double maxY = minY + 16.0;
        double maxZ = minZ + 16.0;
        List<AABB> localParts = new ArrayList<>();
        for (AABB globalPart : globalParts) {
            double clippedMinX = Math.max(globalPart.minX, minX);
            double clippedMinY = Math.max(globalPart.minY, minY);
            double clippedMinZ = Math.max(globalPart.minZ, minZ);
            double clippedMaxX = Math.min(globalPart.maxX, maxX);
            double clippedMaxY = Math.min(globalPart.maxY, maxY);
            double clippedMaxZ = Math.min(globalPart.maxZ, maxZ);
            if (clippedMinX >= clippedMaxX || clippedMinY >= clippedMaxY || clippedMinZ >= clippedMaxZ) {
                continue;
            }
            localParts.add(new AABB(
                clippedMinX - minX,
                clippedMinY - minY,
                clippedMinZ - minZ,
                clippedMaxX - minX,
                clippedMaxY - minY,
                clippedMaxZ - minZ
            ));
        }
        return localParts.isEmpty() ? Shapes.empty() : ShapeUtil.merge(localParts.toArray(AABB[]::new));
    }

    private static AABB rotateGlobalBox(AABB box, float xRotation, float yRotation) {
        Vec3[] corners = new Vec3[] {
            new Vec3(box.minX, box.minY, box.minZ),
            new Vec3(box.minX, box.minY, box.maxZ),
            new Vec3(box.minX, box.maxY, box.minZ),
            new Vec3(box.minX, box.maxY, box.maxZ),
            new Vec3(box.maxX, box.minY, box.minZ),
            new Vec3(box.maxX, box.minY, box.maxZ),
            new Vec3(box.maxX, box.maxY, box.minZ),
            new Vec3(box.maxX, box.maxY, box.maxZ),
        };
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3 corner : corners) {
            Vec3 rotated = rotateAroundCenter(corner, xRotation, yRotation);
            minX = Math.min(minX, rotated.x);
            minY = Math.min(minY, rotated.y);
            minZ = Math.min(minZ, rotated.z);
            maxX = Math.max(maxX, rotated.x);
            maxY = Math.max(maxY, rotated.y);
            maxZ = Math.max(maxZ, rotated.z);
        }
        return new AABB(
            snap(minX),
            snap(minY),
            snap(minZ),
            snap(maxX),
            snap(maxY),
            snap(maxZ)
        );
    }

    private static Vec3 rotateAroundCenter(Vec3 point, float xRotation, float yRotation) {
        Vec3 translated = point.subtract(8.0, 8.0, 8.0);
        if (xRotation != 0.0F) {
            translated = translated.xRot((float) Math.toRadians(xRotation));
        }
        if (yRotation != 0.0F) {
            translated = translated.yRot((float) Math.toRadians(yRotation));
        }
        return translated.add(8.0, 8.0, 8.0);
    }

    private static double snap(double value) {
        double rounded = Math.round(value * 1000.0) / 1000.0;
        return Math.abs(rounded) < 1.0E-6 ? 0.0 : rounded;
    }
}
