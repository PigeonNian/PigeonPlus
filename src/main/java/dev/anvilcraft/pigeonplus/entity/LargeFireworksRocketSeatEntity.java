package dev.anvilcraft.pigeonplus.entity;

import dev.anvilcraft.pigeonplus.block.LargeFireworksRocketBlock;
import dev.anvilcraft.pigeonplus.init.AddonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class LargeFireworksRocketSeatEntity extends Entity {
    public LargeFireworksRocketSeatEntity(EntityType<? extends LargeFireworksRocketSeatEntity> type, Level level) {
        super(type, level);

        this.noPhysics = true;
    }

    public LargeFireworksRocketSeatEntity(Level level, double x, double y, double z) {
        this(AddonEntities.LARGE_FIREWORKS_ROCKET_SEAT.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (!this.isVehicle() || !this.isOnRocket()) {
            this.discard();
        }
    }

    private boolean isOnRocket() {
        BlockPos below = BlockPos.containing(this.getX(), this.getY() - 1.0E-3, this.getZ());
        return this.level().getBlockState(below).getBlock() instanceof LargeFireworksRocketBlock;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return Vec3.ZERO;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return false;
    }
}
