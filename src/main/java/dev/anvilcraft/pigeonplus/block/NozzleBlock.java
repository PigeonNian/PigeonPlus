package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class NozzleBlock extends SimpleMultiPartBlock<Cube3x3PartHalf> {
    public static final MapCodec<NozzleBlock> CODEC = simpleCodec(NozzleBlock::new);
    public static final EnumProperty<Cube3x3PartHalf> PART = EnumProperty.create("part", Cube3x3PartHalf.class);

    public NozzleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, Cube3x3PartHalf.BOTTOM_CENTER));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    public EnumProperty<Cube3x3PartHalf> getPart() {
        return PART;
    }

    @Override
    public Cube3x3PartHalf[] getParts() {
        return Cube3x3PartHalf.values();
    }

    @Override
    public Vec3i getMainPartOffset() {
        return new Vec3i(0, 1, 0);
    }
}
