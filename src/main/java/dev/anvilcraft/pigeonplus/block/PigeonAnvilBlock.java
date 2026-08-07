package dev.anvilcraft.pigeonplus.block;

import dev.dubhe.anvilcraft.block.better.BetterAnvilBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 鸽子铁砧：按普通铁砧（{@link net.minecraft.world.level.block.AnvilBlock}）实现，
 * 碰撞箱沿用皇家铁砧。
 * <ul>
 *     <li>继承 {@link BetterAnvilBlock}（本体的 neoforge 铁砧 / 皇家铁砧都继承它），
 *     从而获得铁砧的朝向、修理界面，以及 {@code #minecraft:anvil} 标签接入的
 *     巨型铁砧撼地弹飞、AnvilCraft 铁砧加工等全部行为。</li>
 *     <li>鸽砧模型与皇家铁砧一致，碰撞箱重写为皇家铁砧（每轴单腿 + 顶板），
 *     与原版铁砧（两段腿 + 踏板）不同。</li>
 *     <li>温柔降落：坠落不砸伤实体。</li>
 *     <li>落地保留铁砧音效/粒子，并叠加鸽子咕咕叫 + 羽毛粒子。</li>
 * </ul>
 */
public class PigeonAnvilBlock extends BetterAnvilBlock {
    // 皇家铁砧碰撞箱：每轴一条贯通腿 + 顶板
    protected static final VoxelShape BASE = Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);
    protected static final VoxelShape X_LEG1 = Block.box(4.0, 4.0, 5.0, 12.0, 10.0, 11.0);
    protected static final VoxelShape X_TOP = Block.box(0.0, 10.0, 3.0, 16.0, 16.0, 13.0);
    protected static final VoxelShape Z_LEG1 = Block.box(5.0, 4.0, 4.0, 11.0, 10.0, 12.0);
    protected static final VoxelShape Z_TOP = Block.box(3.0, 10.0, 0.0, 13.0, 16.0, 16.0);
    protected static final VoxelShape X_AXIS_AABB = Shapes.or(BASE, X_LEG1, X_TOP);
    protected static final VoxelShape Z_AXIS_AABB = Shapes.or(BASE, Z_LEG1, Z_TOP);

    public PigeonAnvilBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? X_AXIS_AABB : Z_AXIS_AABB;
    }

    @Override
    public void falling(FallingBlockEntity entity) {
        // 温柔降落：鸽子铁砧不砸伤实体（皇家铁砧为 2.0F / 80）
        entity.setHurtsEntities(0.0F, 0);
    }

    @Override
    public void onLand(
        Level level,
        BlockPos pos,
        BlockState state,
        BlockState replacedState,
        FallingBlockEntity fallingEntity
    ) {
        // 保留普通铁砧的落地音效与粒子（levelEvent 1031）
        super.onLand(level, pos, state, replacedState, fallingEntity);
        if (level.isClientSide) {
            return;
        }
        // 鸽子咕咕叫
        level.playSound(null, pos, SoundEvents.PARROT_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
        // 鸽子撒羽毛
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                ParticleTypes.WHITE_ASH,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                12,
                0.4,
                0.5,
                0.4,
                0.03
            );
        }
    }
}
