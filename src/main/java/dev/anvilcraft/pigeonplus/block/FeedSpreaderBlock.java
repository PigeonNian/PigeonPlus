package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.pigeonplus.block.entity.FeedSpreaderBlockEntity;
import dev.anvilcraft.pigeonplus.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class FeedSpreaderBlock extends BaseEntityBlock {
    public static final MapCodec<FeedSpreaderBlock> CODEC = simpleCodec(FeedSpreaderBlock::new);

    public FeedSpreaderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FeedSpreaderBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos) instanceof FeedSpreaderBlockEntity feedSpreader)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack extracted = feedSpreader.extractFirstItem();
        if (extracted.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!player.addItem(extracted)) {
            Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), extracted);
        }
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (!FeedSpreaderBlockEntity.isFeedOrBoneMeal(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof FeedSpreaderBlockEntity feedSpreader)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        ItemStack remainder = feedSpreader.insertFeed(stack.copy(), true);
        if (remainder.getCount() == stack.getCount()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            ItemStack insertedRemainder = feedSpreader.insertFeed(stack.copy(), false);
            int inserted = stack.getCount() - insertedRemainder.getCount();
            if (!player.getAbilities().instabuild) {
                stack.shrink(inserted);
            }
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        return createTickerHelper(
            blockEntityType,
            ModBlockEntities.FEED_SPREADER.get(),
            FeedSpreaderBlockEntity::tick
        );
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock())
            && level.getBlockEntity(pos) instanceof FeedSpreaderBlockEntity feedSpreader) {
            Containers.dropContents(level, pos, feedSpreader.getDrops());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
