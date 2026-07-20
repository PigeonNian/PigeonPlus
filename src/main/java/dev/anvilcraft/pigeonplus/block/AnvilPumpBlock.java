package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.pigeonplus.block.entity.AnvilPumpBlockEntity;
import dev.anvilcraft.pigeonplus.block.entity.ModBlockEntities;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class AnvilPumpBlock extends PumpBlock {
    @SuppressWarnings("unchecked")
    public static final MapCodec<PumpBlock> CODEC = (MapCodec<PumpBlock>) (MapCodec<?>) simpleCodec(AnvilPumpBlock::new);
    private static final VoxelShape FULL_SHAPE = box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public AnvilPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<PumpBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return FULL_SHAPE;
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
}
