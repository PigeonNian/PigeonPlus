package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;

public class PigeonAnvilBlock extends FallingBlock implements Fallable {
    public static final MapCodec<PigeonAnvilBlock> CODEC = simpleCodec(PigeonAnvilBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // 碰撞箱照搬皇家铁砧（RoyalAnvilBlock）：每轴一条腿 + 顶板，与原版铁砧（两段腿+踏板）不同
    protected static final VoxelShape BASE = Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);
    protected static final VoxelShape X_LEG1 = Block.box(4.0, 4.0, 5.0, 12.0, 10.0, 11.0);
    protected static final VoxelShape X_TOP = Block.box(0.0, 10.0, 3.0, 16.0, 16.0, 13.0);
    protected static final VoxelShape Z_LEG1 = Block.box(5.0, 4.0, 4.0, 11.0, 10.0, 12.0);
    protected static final VoxelShape Z_TOP = Block.box(3.0, 10.0, 0.0, 13.0, 16.0, 16.0);
    protected static final VoxelShape X_AXIS_AABB = Shapes.or(BASE, X_LEG1, X_TOP);
    protected static final VoxelShape Z_AXIS_AABB = Shapes.or(BASE, Z_LEG1, Z_TOP);

    public PigeonAnvilBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 与原版/皇家铁砧一致：FACING = 玩家朝向顺时针旋转 90°
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? X_AXIS_AABB : Z_AXIS_AABB;
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return 0x9A9A9A;
    }

    @Override
    protected void falling(FallingBlockEntity entity) {
        // 温柔降落：鸽子铁砧不砸伤实体（皇家铁砧为 2.0F / 80）
        entity.setHurtsEntities(0.0F, 0);
    }

    @Override
    public DamageSource getFallDamageSource(Entity entity) {
        return entity.damageSources().fall();
    }

    @Override
    public void onLand(
        Level level,
        BlockPos pos,
        BlockState state,
        BlockState replacedState,
        FallingBlockEntity fallingEntity
    ) {
        // 铁砧落地音效与粒子（原版 AnvilBlock.onLand 的行为）
        if (!fallingEntity.isSilent()) {
            level.levelEvent(1031, pos, 0);
        }
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
        // 接入 AnvilCraft 铁砧加工（只跑加工，不砸人）
        AnvilEvent.OnLand event = new AnvilEvent.OnLand(level, pos, fallingEntity, fallingEntity.fallDistance);
        event.setAnvilDamage(false);
        NeoForge.EVENT_BUS.post(event);
    }
}
