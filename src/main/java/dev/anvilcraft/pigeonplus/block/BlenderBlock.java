package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.pigeonplus.block.entity.BlenderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BlenderBlock extends BaseEntityBlock {
    public static final MapCodec<BlenderBlock> CODEC = simpleCodec(BlenderBlock::new);

    private static final VoxelShape SHAPE = makeShape();

    public BlenderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlenderBlockEntity(pos, state);
    }

    private static VoxelShape makeShape() {
        VoxelShape shape = Shapes.empty();
        // Base plate
        shape = Shapes.join(shape, Shapes.box(0.0, 0.0, 0.0, 1.0, 0.125, 1.0), BooleanOp.OR);
        // Body
        shape = Shapes.join(shape, Shapes.box(0.1875, 0.125, 0.1875, 0.8125, 0.875, 0.8125), BooleanOp.OR);
        // Pipe connector
        shape = Shapes.join(shape, Shapes.box(0.25, 0.25, 0.8125, 0.75, 0.75, 1.0), BooleanOp.OR);
        // Legs
        shape = Shapes.join(shape, Shapes.box(0.0, 0.1875, 0.0, 0.1875, 1.0, 0.1875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0, 0.1875, 0.8125, 0.1875, 1.0, 1.0), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.8125, 0.1875, 0.0, 1.0, 1.0, 0.1875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.8125, 0.1875, 0.8125, 1.0, 1.0, 1.0), BooleanOp.OR);
        return shape;
    }
}
