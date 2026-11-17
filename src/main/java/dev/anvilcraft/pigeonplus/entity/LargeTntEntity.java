package dev.anvilcraft.pigeonplus.entity;

import dev.anvilcraft.pigeonplus.block.LargeTntBlock;
import dev.anvilcraft.pigeonplus.init.AddonEntities;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;


public class LargeTntEntity extends Entity {
    public static final int DEFAULT_FUSE = 80;
    /** 碰撞箱边长：比方块结构小 0.1 格，刚生成时不会卡在 3×3×3 坑口的边缘方块上。 */
    public static final float SIZE = 2.8F;
    public static final int BLAST_RADIUS = 11;
    public static final int PROBABILITY_RADIUS = 13;
    public static final int MELTING_RADIUS = 15;

    private static final EntityDataAccessor<Integer> DATA_FUSE =
        SynchedEntityData.defineId(LargeTntEntity.class, EntityDataSerializers.INT);

    @Nullable
    private LivingEntity owner;

    public LargeTntEntity(EntityType<? extends LargeTntEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public LargeTntEntity(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
        this(AddonEntities.LARGE_TNT.get(), level);
        this.setPos(x, y, z);
        double angle = level.random.nextDouble() * Math.PI * 2.0;
        this.setDeltaMovement(-Math.sin(angle) * 0.02, 0.2, -Math.cos(angle) * 0.02);
        this.setFuse(DEFAULT_FUSE);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.owner = owner;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FUSE, DEFAULT_FUSE);
    }

    @Override
    public void tick() {
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        if (this.onGround()) {
            Vec3 velocity = this.getDeltaMovement();
            this.setDeltaMovement(velocity.x * 0.7, velocity.y * -0.5, velocity.z * 0.7);
        }
        int fuse = this.getFuse() - 1;
        this.setFuse(fuse);
        if (fuse <= 0) {
            if (!this.level().isClientSide) {
                this.explode();
            }
            this.discard();
        }
    }

    private void explode() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double centerX = this.getX();
        double centerY = this.getY() + 1.5;
        double centerZ = this.getZ();
        BlockPos center = BlockPos.containing(centerX, centerY, centerZ);
        // 先把波及范围内的大型TNT连锁点燃，再开炸；被点掉的方块也就不必等爆炸去清了。
        this.igniteNeighbouringTnt(serverLevel, center);
        LargeTntExplosion.detonate(serverLevel, this, centerX, centerY, centerZ);
        serverLevel.playSound(
            null,
            center,
            SoundEvents.GENERIC_EXPLODE.value(),
            SoundSource.BLOCKS,
            4.0F,
            (1.0F + (serverLevel.random.nextFloat() - serverLevel.random.nextFloat()) * 0.2F) * 0.7F
        );
        serverLevel.sendParticles(
            ParticleTypes.EXPLOSION_EMITTER, centerX, centerY, centerZ, 1, 0.0, 0.0, 0.0, 0.0);
    }

    private void igniteNeighbouringTnt(ServerLevel level, BlockPos center) {
        int radius = MELTING_RADIUS;
        int radiusSqr = radius * radius;
        for (BlockPos cursor : BlockPos.betweenClosed(
            center.offset(-radius, -radius, -radius),
            center.offset(radius, radius, radius)
        )) {
            if (cursor.distSqr(center) > radiusSqr) {
                continue;
            }
            // 先确认区块已加载，避免扫描到未加载区域时顺手把区块加载/生成出来。
            if (!level.isLoaded(cursor)) {
                continue;
            }
            BlockState state = level.getBlockState(cursor);
            if (!(state.getBlock() instanceof LargeTntBlock largeTnt)) {
                continue;
            }
            if (state.getValue(LargeTntBlock.PART) != Cube3x3PartHalf.BOTTOM_CENTER) {
                continue;
            }
            largeTnt.igniteByExplosion(level, cursor);
        }
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE);
    }

    public void setFuse(int fuse) {
        this.entityData.set(DATA_FUSE, fuse);
    }

    @Nullable
    public LivingEntity getOwner() {
        return this.owner;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("Fuse", (short) this.getFuse());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setFuse(tag.getShort("Fuse"));
    }
}
