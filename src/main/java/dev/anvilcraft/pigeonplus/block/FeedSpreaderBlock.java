package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.anvilcraft.pigeonplus.block.entity.FeedSpreaderBlockEntity;
import dev.anvilcraft.pigeonplus.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FeedSpreaderBlock extends BaseEntityBlock implements IHammerRemovable {
    public static final MapCodec<FeedSpreaderBlock> CODEC = simpleCodec(FeedSpreaderBlock::new);

    public FeedSpreaderBlock(Properties properties) {
        super(properties);
    }

    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0),
        Block.box(5.5, 3.0, 5.5, 10.5, 7.0, 10.5),
        Block.box(6.0, 2.0, 6.0, 10.0, 16.0, 10.0)
    );

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
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
    protected InteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (!FeedSpreaderBlockEntity.isFeedOrBoneMeal(stack)) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof FeedSpreaderBlockEntity feedSpreader)) {
            return InteractionResult.PASS;
        }

        ItemStack remainder = feedSpreader.insertFeed(stack.copy(), true);
        if (remainder.getCount() == stack.getCount()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack insertedRemainder = feedSpreader.insertFeed(stack.copy(), false);
            int inserted = stack.getCount() - insertedRemainder.getCount();
            stack.shrink(inserted);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
        return InteractionResult.SUCCESS;
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

    /**
     * 26.1 把 {@code onRemove} 换成了 {@code affectNeighborsAfterRemoval}：
     * 只在服务端触发，且不再给出新旧状态。原先靠
     * {@code !state.is(newState.getBlock())} 判断「真的被换成别的方块了」，
     * 现在改为在移除前直接掉落内容物——这样无论后续换成什么方块，
     * 内容物都不会丢。
     */
    @Override
    protected void affectNeighborsAfterRemoval(ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof FeedSpreaderBlockEntity feedSpreader) {
            Containers.dropContents(level, pos, feedSpreader.getDrops());
        }
        super.affectNeighborsAfterRemoval(level, pos, movedByPiston);
    }
}
