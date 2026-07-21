package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.anvilcraft.pigeonplus.block.entity.AnvilPumpBlockEntity;
import dev.anvilcraft.pigeonplus.block.entity.ModBlockEntities;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class AnvilPumpBlock extends BetterBaseEntityBlock
    implements IHammerRemovable, IHammerChangeable, IMoveableEntityBlock {
    public static final MapCodec<AnvilPumpBlock> CODEC = simpleCodec(AnvilPumpBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final AABB[] SOUTH_SHAPE_PARTS = {
        new AABB(3.0, 3.0, 0.0, 13.0, 13.0, 16.0),
        new AABB(6.0, 13.0, 6.0, 10.0, 16.0, 10.0),
    };
    private static final VoxelShape SOUTH_SHAPE = ShapeUtil.merge(SOUTH_SHAPE_PARTS);
    private static final VoxelShape WEST_SHAPE = ShapeUtil.merge(
        ShapeUtil.rotate(Direction.Axis.Y, 90.0F, SOUTH_SHAPE_PARTS)
    );
    private static final VoxelShape NORTH_SHAPE = ShapeUtil.merge(
        ShapeUtil.rotate(Direction.Axis.Y, 180.0F, SOUTH_SHAPE_PARTS)
    );
    private static final VoxelShape EAST_SHAPE = ShapeUtil.merge(
        ShapeUtil.rotate(Direction.Axis.Y, -90.0F, SOUTH_SHAPE_PARTS)
    );

    public AnvilPumpBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.SOUTH)
            .setValue(PumpBlock.POWERED, false)
            .setValue(PumpBlock.OVERLOAD, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, PumpBlock.POWERED, PumpBlock.OVERLOAD);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getHorizontalShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return getHorizontalShape(state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        Player player = context.getPlayer();
        boolean shiftDown = player != null && player.isShiftKeyDown();
        if (!shiftDown) {
            facing = facing.getOpposite();
        }
        return this.defaultBlockState()
            .setValue(FACING, facing)
            .setValue(PumpBlock.POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        BlockState state = level.getBlockState(blockPos);
        level.setBlockAndUpdate(blockPos, state.setValue(FACING, state.getValue(FACING).getOpposite()));
        return true;
    }

    @Override
    public boolean checkBlockState(BlockState blockState) {
        return true;
    }

    @Nullable
    @Override
    public Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide()) {
            return;
        }
        FluidNetworkManager.INSTANCE.addAdjacentContainers(level, pos);
        boolean hasSignal = level.hasNeighborSignal(pos);
        if (hasSignal != state.getValue(PumpBlock.POWERED)) {
            level.setBlock(pos, state.setValue(PumpBlock.POWERED, hasSignal), 2);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            FluidNetworkManager.INSTANCE.addAdjacentContainers(level, pos);
            FluidNetworkManager.INSTANCE.markDirty(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            FluidNetworkManager.INSTANCE.markDirty(level);
        }
    }

    @Nullable
    @Override
    public AnvilPumpBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnvilPumpBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        return createTickerHelper(
            blockEntityType,
            ModBlockEntities.ANVIL_PUMP.get(),
            AnvilPumpBlockEntity::tick
        );
    }

    public static boolean isConnectableFace(BlockState state, Direction faceToNeighbor) {
        return faceToNeighbor.getAxis() == getOutputDirection(state).getAxis();
    }

    public static Direction getOutputDirection(BlockState state) {
        return state.getValue(FACING);
    }

    public static int getYRotation(Direction direction) {
        return switch (direction) {
            case WEST -> 90;
            case NORTH -> 180;
            case EAST -> 270;
            default -> 0;
        };
    }

    private static VoxelShape getHorizontalShape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }
}
