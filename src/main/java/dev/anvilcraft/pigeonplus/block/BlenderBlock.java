package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.pigeonplus.block.entity.BlenderBlockEntity;
import dev.anvilcraft.pigeonplus.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BlenderBlock extends BaseEntityBlock {
    public static final MapCodec<BlenderBlock> CODEC = simpleCodec(BlenderBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    public BlenderBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(WORKING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WORKING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(WORKING, false);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(WORKING)) {
            return;
        }

        Direction inputSide = state.getValue(FACING).getOpposite();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction == inputSide || random.nextFloat() > 0.65f) {
                continue;
            }

            double x = pos.getX() + 0.5 + direction.getStepX() * 0.56 + (random.nextDouble() - 0.5) * 0.22;
            double y = pos.getY() + 0.45 + random.nextDouble() * 0.35;
            double z = pos.getZ() + 0.5 + direction.getStepZ() * 0.56 + (random.nextDouble() - 0.5) * 0.22;
            double xSpeed = direction.getStepX() * 0.045 + (random.nextDouble() - 0.5) * 0.012;
            double ySpeed = 0.006 + random.nextDouble() * 0.01;
            double zSpeed = direction.getStepZ() * 0.045 + (random.nextDouble() - 0.5) * 0.012;

            level.addParticle(ParticleTypes.CLOUD, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlenderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
            blockEntityType,
            ModBlockEntities.BLENDER.get(),
            BlenderBlockEntity::serverTick
        );
    }
}
