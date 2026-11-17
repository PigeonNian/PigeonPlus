package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.pigeonplus.entity.LargeTntEntity;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LargeTntBlock extends SimpleMultiPartBlock<Cube3x3PartHalf> implements IHammerRemovable {
    public static final MapCodec<LargeTntBlock> CODEC = simpleCodec(LargeTntBlock::new);
    public static final EnumProperty<Cube3x3PartHalf> PART =
        EnumProperty.create("part", Cube3x3PartHalf.class);

    public LargeTntBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(PART, Cube3x3PartHalf.BOTTOM_CENTER));
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
    public Property<Cube3x3PartHalf> getPart() {
        return PART;
    }

    @Override
    public Cube3x3PartHalf[] getParts() {
        return Cube3x3PartHalf.values();
    }

    @Override
    public BlockState getModelHolderState(BlockState original) {
        return original.setValue(PART, Cube3x3PartHalf.MID_CENTER);
    }

    public static String getModelName(BlockState state) {
        return state.hasProperty(PART) && state.getValue(PART) == Cube3x3PartHalf.MID_CENTER
            ? "large_tnt"
            : "large_tnt_part";
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
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
        ItemStack held = player.getItemInHand(hand);
        boolean flint = held.is(Items.FLINT_AND_STEEL);
        if (!flint && !held.is(Items.FIRE_CHARGE)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        this.prime(level, pos, state, player, -1);
        if (!player.getAbilities().instabuild) {
            if (flint) {
                held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            } else {
                held.consume(1, player);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(held.getItem()));
        return InteractionResult.CONSUME;
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        if (level.isClientSide() || !state.is(this) || !level.hasNeighborSignal(pos)) {
            return;
        }
        this.prime(level, pos, state, null, -1);
    }

    public void igniteByExplosion(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) {
            return;
        }
        this.prime(level, pos, state, null, level.random.nextInt(20) + 10);
    }

    private void prime(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity igniter, int fuse) {
        // 拆结构之前先把主部件位置算出来：拆完就读不到 PART 了。
        BlockPos mainPos = this.getMainPartPos(pos, state);
        this.removePartsAndUpdate(level, pos);
        LargeTntEntity tnt = new LargeTntEntity(
            level, mainPos.getX() + 0.5, mainPos.getY(), mainPos.getZ() + 0.5, igniter);
        if (fuse > 0) {
            tnt.setFuse(fuse);
        }
        level.addFreshEntity(tnt);
        level.playSound(null, mainPos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public void removePartsAndUpdate(Level level, BlockPos pos) {
        BlockState baseState = level.getBlockState(pos);
        if (!baseState.is(this)) {
            return;
        }
        List<BlockPos> parts = new ArrayList<>(Cube3x3PartHalf.values().length);
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            parts.add(pos.offset(this.offsetFrom(baseState, part)));
        }

        for (BlockPos partPos : parts) {
            if (level.getBlockState(partPos).is(this)) {
                level.setBlock(partPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS, 0);
            }
        }

        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos partPos : parts) {
            air.updateNeighbourShapes(level, partPos, Block.UPDATE_CLIENTS, 512);
            level.updateNeighborsAt(partPos, this);
        }
    }
}
