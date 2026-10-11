package dev.anvilcraft.pigeonplus.entity;

import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class LargeFireworksRocketLeashKnotEntity extends BlockAttachedEntity {
    private static final double ROPE_HEIGHT = 1.0;
    private static final double ROPE_HALF_WIDTH = 1.0;

    private static final double BOX_HALF_WIDTH = 0.1875;
    private static final double BOX_HEIGHT = 0.5;

    public LargeFireworksRocketLeashKnotEntity(
        EntityType<? extends LargeFireworksRocketLeashKnotEntity> type,
        Level level
    ) {
        super(type, level);
    }

    public LargeFireworksRocketLeashKnotEntity(Level level, BlockPos mainPos) {
        this(AddonEntities.LARGE_FIREWORKS_ROCKET_LEASH_KNOT.get(), level);
        this.setPos(mainPos.getX(), mainPos.getY(), mainPos.getZ());
    }

    public static Optional<LargeFireworksRocketLeashKnotEntity> getKnot(Level level, BlockPos mainPos) {
        return level.getEntitiesOfClass(
                LargeFireworksRocketLeashKnotEntity.class, new AABB(mainPos).inflate(2.0))
            .stream()
            .filter(knot -> knot.getPos().equals(mainPos))
            .findFirst();
    }

    public static LargeFireworksRocketLeashKnotEntity getOrCreateKnot(Level level, BlockPos mainPos) {
        return getKnot(level, mainPos).orElseGet(() -> {
            LargeFireworksRocketLeashKnotEntity knot =
                new LargeFireworksRocketLeashKnotEntity(level, mainPos);
            level.addFreshEntity(knot);
            return knot;
        });
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void recalculateBoundingBox() {
        double centerX = this.pos.getX() + 0.5;
        double anchorY = this.pos.getY() + ROPE_HEIGHT;
        double centerZ = this.pos.getZ() + 0.5;
        this.setPosRaw(centerX, anchorY, centerZ);
        this.setBoundingBox(
            new AABB(
                centerX - BOX_HALF_WIDTH, anchorY, centerZ - BOX_HALF_WIDTH,
                centerX + BOX_HALF_WIDTH, anchorY + BOX_HEIGHT, centerZ + BOX_HALF_WIDTH
            )
        );
    }

    @Override
    public boolean survives() {
        return this.level().getBlockState(this.pos).is(AddonBlocks.LARGE_FIREWORKS_ROCKET.get());
    }

    @Override
    public void dropItem(ServerLevel level, @Nullable Entity causedBy) {
        this.playSound(SoundEvents.LEAD_UNTIED, 1.0F, 1.0F);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean supportQuadLeashAsHolder() {
        return true;
    }

    @Override
    public Vec3[] getQuadLeashHolderOffsets() {
        return new Vec3[]{
            new Vec3(-ROPE_HALF_WIDTH, 0.0, ROPE_HALF_WIDTH),
            new Vec3(-ROPE_HALF_WIDTH, 0.0, -ROPE_HALF_WIDTH),
            new Vec3(ROPE_HALF_WIDTH, 0.0, -ROPE_HALF_WIDTH),
            new Vec3(ROPE_HALF_WIDTH, 0.0, ROPE_HALF_WIDTH)
        };
    }

    @Override
    public void notifyLeasheeRemoved(Leashable entity) {
        if (Leashable.leashableLeashedTo(this).isEmpty()) {
            this.discard();
        }
    }
}
