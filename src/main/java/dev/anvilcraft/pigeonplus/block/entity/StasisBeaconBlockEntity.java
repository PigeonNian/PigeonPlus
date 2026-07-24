package dev.anvilcraft.pigeonplus.block.entity;

import dev.anvilcraft.pigeonplus.block.StasisBeaconBlock;
import dev.anvilcraft.pigeonplus.util.StasisTimeFreezeManager;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class StasisBeaconBlockEntity extends BlockEntity {
    private static final int MAX_LEVELS = 4;
    private static final int BLOCKS_CHECK_PER_TICK = 10;

    private int levels;
    private int beamHeight;
    private int checkingBeamHeight;
    private int lastCheckY;
    private UUID frozenEntityId;
    private int frozenEntityClientId = -1;
    private UUID forceReleasedEntityId;
    private float frozenAccumulatedDamage;
    private double frozenAccumulatedSpeed;
    private int frozenTicks;

    public StasisBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STASIS_BEACON.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StasisBeaconBlockEntity blockEntity) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int maxY = level.getMaxBuildHeight();

        if (blockEntity.lastCheckY < y) {
            blockEntity.lastCheckY = y;
            blockEntity.checkingBeamHeight = maxY;
        }

        BlockPos checkPos = new BlockPos(x, blockEntity.lastCheckY + 1, z);
        for (int i = 0; i < BLOCKS_CHECK_PER_TICK && checkPos.getY() <= maxY; i++) {
            BlockState checkState = level.getBlockState(checkPos);
            if (checkState.getBeaconColorMultiplier(level, checkPos, pos) == null
                && checkState.getLightBlock(level, checkPos) >= 15
                && !checkState.is(Blocks.BEDROCK)) {
                blockEntity.checkingBeamHeight = checkPos.getY();
                blockEntity.lastCheckY = maxY;
                break;
            }
            checkPos = checkPos.above();
            blockEntity.lastCheckY++;
        }

        if (level.getGameTime() % 80L == 0L) {
            int lastLevels = blockEntity.levels;
            blockEntity.levels = updateBase(level, x, y, z);
            if (!level.isClientSide) {
                boolean shouldLit = blockEntity.levels > 0;
                boolean isLit = state.hasProperty(StasisBeaconBlock.LIT) && state.getValue(StasisBeaconBlock.LIT);
                if (shouldLit && !isLit) {
                    setBeaconStatus(level, pos, state, blockEntity, true);
                } else if (lastLevels > 0 && !shouldLit) {
                    blockEntity.levels = 0;
                    setBeaconStatus(level, pos, state, blockEntity, false);
                }
            }
            if (blockEntity.levels > 0) {
                playSound(level, pos, SoundEvents.BEACON_AMBIENT);
            }
        }

        if (blockEntity.lastCheckY >= maxY) {
            blockEntity.lastCheckY = level.getMinBuildHeight() - 1;
            blockEntity.beamHeight = blockEntity.checkingBeamHeight;
        }

        if (!level.isClientSide) {
            blockEntity.tickStasis((ServerLevel) level, pos);
        }
    }

    public static void setBeaconStatus(
        Level level,
        BlockPos pos,
        BlockState state,
        StasisBeaconBlockEntity blockEntity,
        boolean status
    ) {
        level.setBlockAndUpdate(pos, state.setValue(StasisBeaconBlock.LIT, status));
        if (status) {
            playSound(level, pos, SoundEvents.BEACON_ACTIVATE);
            List<ServerPlayer> players = level.getEntitiesOfClass(
                ServerPlayer.class,
                new AABB(pos).inflate(0.0, -4.0, 0.0).inflate(10.0, 5.0, 10.0)
            );
            for (ServerPlayer player : players) {
                CriteriaTriggers.CONSTRUCT_BEACON.trigger(player, blockEntity.levels);
            }
        } else {
            playSound(level, pos, SoundEvents.BEACON_DEACTIVATE);
        }
    }

    private static int updateBase(Level level, int x, int y, int z) {
        int beaconLevel = 0;
        for (int layer = 1; layer <= MAX_LEVELS; beaconLevel = layer++) {
            int layerY = y - layer;
            if (layerY < level.getMinBuildHeight()) {
                break;
            }

            boolean valid = true;
            for (int layerX = x - layer; layerX <= x + layer && valid; layerX++) {
                for (int layerZ = z - layer; layerZ <= z + layer; layerZ++) {
                    if (!level.getBlockState(new BlockPos(layerX, layerY, layerZ)).is(ModBlocks.FROST_METAL_BLOCK.get())) {
                        valid = false;
                        break;
                    }
                }
            }
            if (!valid) {
                break;
            }
        }
        return beaconLevel;
    }

    private void tickStasis(ServerLevel level, BlockPos pos) {
        boolean beamActive = this.levels > 0 && this.beamHeight > pos.getY() + 1;
        if (!beamActive) {
            this.releaseFrozenEntity(level, true);
            this.forceReleasedEntityId = null;
            return;
        }

        AABB beamBounds = this.getBeamBounds(pos);
        if (this.frozenEntityId != null) {
            Entity entity = level.getEntity(this.frozenEntityId);
            if (entity == null || !entity.isAlive()) {
                StasisTimeFreezeManager.release(level, this.frozenEntityId, false);
                this.clearFrozenEntity();
                return;
            }
            if (!beamBounds.intersects(entity.getBoundingBox())
                || !StasisTimeFreezeManager.isFrozenBy(entity, pos)) {
                this.releaseFrozenEntity(level, true);
                return;
            }
            this.updateFrozenInfo(StasisTimeFreezeManager.getInfo(entity, level.getGameTime()));
            if (StasisTimeFreezeManager.shouldForceRelease(entity, level.getGameTime())
                || StasisTimeFreezeManager.shouldForceReleaseByDamage(entity)) {
                this.forceReleasedEntityId = this.frozenEntityId;
                this.releaseFrozenEntity(level, true);
            }
            return;
        }

        this.clearForceReleasedEntityIfReady(level, beamBounds);
        List<Entity> candidates = level.getEntitiesOfClass(
            Entity.class,
            beamBounds,
            entity -> StasisTimeFreezeManager.canFreeze(entity)
                && !entity.getUUID().equals(this.forceReleasedEntityId)
                && !StasisTimeFreezeManager.isFrozen(entity)
        );
        candidates.stream()
            .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(pos.getX() + 0.5, entity.getY(), pos.getZ() + 0.5)))
            .ifPresent(entity -> {
                if (StasisTimeFreezeManager.tryFreeze(entity, pos, level.getGameTime())) {
                    this.setFrozenEntity(entity);
                }
            });
    }

    private void clearForceReleasedEntityIfReady(ServerLevel level, AABB beamBounds) {
        if (this.forceReleasedEntityId == null) {
            return;
        }
        Entity entity = level.getEntity(this.forceReleasedEntityId);
        if (entity == null || !entity.isAlive() || !beamBounds.intersects(entity.getBoundingBox())) {
            this.forceReleasedEntityId = null;
        }
    }

    private AABB getBeamBounds(BlockPos pos) {
        return new AABB(pos).expandTowards(0.0, this.beamHeight - pos.getY(), 0.0);
    }

    public static boolean isInActiveBeam(Level level, Entity entity) {
        int x = (int) Math.floor(entity.getX());
        int z = (int) Math.floor(entity.getZ());
        int entityY = (int) Math.floor(entity.getY());
        for (int y = entityY; y >= level.getMinBuildHeight(); y--) {
            BlockPos beaconPos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(beaconPos);
            if (!state.hasProperty(StasisBeaconBlock.LIT) || !state.getValue(StasisBeaconBlock.LIT)) {
                continue;
            }
            if (!(level.getBlockEntity(beaconPos) instanceof StasisBeaconBlockEntity beacon)) {
                continue;
            }
            return beacon.beamHeight > beaconPos.getY() + 1
                && beacon.getBeamBounds(beaconPos).intersects(entity.getBoundingBox());
        }
        return false;
    }

    private void releaseFrozenEntity(Level level, boolean applyStoredEffects) {
        if (this.frozenEntityId == null) {
            return;
        }
        StasisTimeFreezeManager.release(level, this.frozenEntityId, applyStoredEffects);
        this.clearFrozenEntity();
    }

    private void setFrozenEntity(Entity entity) {
        UUID entityId = entity.getUUID();
        int clientId = entity.getId();
        if (entityId.equals(this.frozenEntityId) && clientId == this.frozenEntityClientId) {
            return;
        }
        this.frozenEntityId = entityId;
        this.frozenEntityClientId = clientId;
        this.updateFrozenInfo(StasisTimeFreezeManager.getInfo(entity, entity.level().getGameTime()));
        this.syncToClient();
    }

    private void clearFrozenEntity() {
        if (this.frozenEntityId == null && this.frozenEntityClientId == -1) {
            return;
        }
        this.frozenEntityId = null;
        this.frozenEntityClientId = -1;
        this.updateFrozenInfo(StasisTimeFreezeManager.StasisInfo.EMPTY);
        this.syncToClient();
    }

    private void updateFrozenInfo(StasisTimeFreezeManager.StasisInfo info) {
        float damage = info.damage();
        double speed = info.speed();
        int ticks = info.ticks();
        boolean changed = Math.abs(this.frozenAccumulatedDamage - damage) > 0.001f
            || Math.abs(this.frozenAccumulatedSpeed - speed) > 0.001
            || this.frozenTicks != ticks;
        this.frozenAccumulatedDamage = damage;
        this.frozenAccumulatedSpeed = speed;
        this.frozenTicks = ticks;
        if (changed && this.level != null && !this.level.isClientSide) {
            this.syncToClient();
        }
    }

    private void syncToClient() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null) {
            this.releaseFrozenEntity(this.level, true);
            playSound(this.level, this.worldPosition, SoundEvents.BEACON_DEACTIVATE);
        }
        super.setRemoved();
    }

    public static void playSound(Level level, BlockPos pos, SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = this.saveWithoutMetadata(registries);
        this.writeUpdateTag(tag);
        return tag;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("FrozenEntityClientId")) {
            this.frozenEntityClientId = tag.getInt("FrozenEntityClientId");
        }
        if (tag.contains("FrozenAccumulatedDamage")) {
            this.frozenAccumulatedDamage = tag.getFloat("FrozenAccumulatedDamage");
        }
        if (tag.contains("FrozenAccumulatedSpeed")) {
            this.frozenAccumulatedSpeed = tag.getDouble("FrozenAccumulatedSpeed");
        }
        if (tag.contains("FrozenTicks")) {
            this.frozenTicks = tag.getInt("FrozenTicks");
        }
        if (tag.contains("HasFrozenEntity") && tag.getBoolean("HasFrozenEntity")) {
            this.frozenEntityId = tag.getUUID("FrozenEntityId");
        } else if (tag.contains("HasFrozenEntity")) {
            this.frozenEntityId = null;
        }
    }

    private void writeUpdateTag(CompoundTag tag) {
        tag.putBoolean("HasFrozenEntity", this.frozenEntityId != null);
        tag.putInt("FrozenEntityClientId", this.frozenEntityClientId);
        tag.putFloat("FrozenAccumulatedDamage", this.frozenAccumulatedDamage);
        tag.putDouble("FrozenAccumulatedSpeed", this.frozenAccumulatedSpeed);
        tag.putInt("FrozenTicks", this.frozenTicks);
        if (this.frozenEntityId != null) {
            tag.putUUID("FrozenEntityId", this.frozenEntityId);
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        this.lastCheckY = level.getMinBuildHeight() - 1;
        this.beamHeight = level.getMaxBuildHeight();
    }

    public int getLevels() {
        return this.levels;
    }

    public int getBeamHeight() {
        return this.beamHeight;
    }

    public int getFrozenEntityClientId() {
        return this.frozenEntityClientId;
    }

    public float getFrozenAccumulatedDamage() {
        return this.frozenAccumulatedDamage;
    }

    public double getFrozenAccumulatedSpeed() {
        return this.frozenAccumulatedSpeed;
    }

    public int getFrozenTicks() {
        return this.frozenTicks;
    }
}
