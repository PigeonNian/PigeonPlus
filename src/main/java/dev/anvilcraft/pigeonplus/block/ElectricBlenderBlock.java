package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.pigeonplus.block.entity.ElectricBlenderBlockEntity;
import dev.anvilcraft.pigeonplus.block.entity.ModBlockEntities;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public class ElectricBlenderBlock extends BlenderBlock {
    public static final MapCodec<ElectricBlenderBlock> CODEC = simpleCodec(ElectricBlenderBlock::new);
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;
    public static final BooleanProperty HEATING = BooleanProperty.create("heating");

    public ElectricBlenderBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(WORKING, false)
            .setValue(HEAD, BlenderHead.NONE)
            .setValue(OVERLOAD, true)
            .setValue(HEATING, false));
    }

    @Override
    protected MapCodec<? extends BlenderBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OVERLOAD, HEATING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(OVERLOAD, true).setValue(HEATING, false);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricBlenderBlockEntity(pos, state);
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
            ModBlockEntities.ELECTRIC_BLENDER.get(),
            ElectricBlenderBlockEntity::serverTick
        );
    }
}
