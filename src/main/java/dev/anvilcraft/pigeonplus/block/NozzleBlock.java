package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class NozzleBlock extends FlexibleMultiPartBlock<DirectionCube3x3PartHalf, DirectionProperty, Direction> {
    public static final MapCodec<NozzleBlock> CODEC = simpleCodec(NozzleBlock::new);
    public static final EnumProperty<DirectionCube3x3PartHalf> PART =
        EnumProperty.create("part", DirectionCube3x3PartHalf.class);
    public static final DirectionProperty FACING = DirectionalBlock.FACING;

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
}
