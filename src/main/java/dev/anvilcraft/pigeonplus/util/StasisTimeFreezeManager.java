package dev.anvilcraft.pigeonplus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StasisTimeFreezeManager {
    public static final int MAX_FREEZE_TICKS = 30 * 20;
    public static final float MAX_STORED_DAMAGE = 10.0f;
    private static final Map<UUID, FrozenEntityState> FROZEN_ENTITIES = new HashMap<>();

    private StasisTimeFreezeManager() {
    }

    public static boolean tryFreeze(Entity entity, BlockPos beaconPos, long gameTime) {
        if (!canFreeze(entity)) {
            return false;
        }
        UUID entityId = entity.getUUID();
        FrozenEntityState current = FROZEN_ENTITIES.get(entityId);
        if (current != null) {
            return current.beaconPos.equals(beaconPos);
        }
        FROZEN_ENTITIES.put(entityId, FrozenEntityState.capture(entity, beaconPos, gameTime));
        if (entity instanceof LivingEntity living) {
            living.invulnerableTime = 0;
        }
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
        return true;
    }

    public static boolean canFreeze(Entity entity) {
        return !(entity instanceof Player) && entity.isAlive()
            && (entity instanceof LivingEntity || entity instanceof FallingBlockEntity);
    }

    public static boolean isFrozen(Entity entity) {
        return FROZEN_ENTITIES.containsKey(entity.getUUID());
    }

    public static boolean isFrozenBy(Entity entity, BlockPos beaconPos) {
        FrozenEntityState state = FROZEN_ENTITIES.get(entity.getUUID());
        return state != null && state.beaconPos.equals(beaconPos);
    }

    public static boolean shouldForceRelease(Entity entity, long gameTime) {
        FrozenEntityState state = FROZEN_ENTITIES.get(entity.getUUID());
        return state != null && gameTime - state.startGameTime >= MAX_FREEZE_TICKS;
    }

    public static boolean shouldForceReleaseByDamage(Entity entity) {
        FrozenEntityState state = FROZEN_ENTITIES.get(entity.getUUID());
        return state != null && state.accumulatedDamage >= MAX_STORED_DAMAGE;
    }

    public static boolean freezeTick(Entity entity) {
        FrozenEntityState state = FROZEN_ENTITIES.get(entity.getUUID());
        if (state == null) {
            return false;
        }
        state.accumulateMomentum(entity.getDeltaMovement());
        state.restore(entity);
        return true;
    }

    public static boolean captureDamage(LivingEntity entity, DamageSource source, float damage) {
        FrozenEntityState state = FROZEN_ENTITIES.get(entity.getUUID());
        if (state == null || damage <= 0.0f) {
            return false;
        }
        state.accumulateDamage(source, damage);
        return true;
    }

    public static boolean captureMomentum(Entity entity, Vec3 momentum) {
        FrozenEntityState state = FROZEN_ENTITIES.get(entity.getUUID());
        if (state == null || momentum.lengthSqr() <= 1.0E-8) {
            return false;
        }
        state.accumulateMomentum(momentum);
        return true;
    }

    public static boolean captureKnockback(LivingEntity entity, float strength, double ratioX, double ratioZ) {
        FrozenEntityState state = FROZEN_ENTITIES.get(entity.getUUID());
        if (state == null || strength <= 0.0f) {
            return false;
        }
        Vec3 direction = new Vec3(ratioX, 0.0, ratioZ);
        if (direction.lengthSqr() <= 1.0E-8) {
            return false;
        }
        Vec3 horizontalMomentum = direction.normalize().scale(-strength);
        double verticalMomentum = entity.onGround() ? Math.min(0.4, strength) : 0.0;
        state.accumulateMomentum(horizontalMomentum.add(0.0, verticalMomentum, 0.0));
        return true;
    }

    public static void release(Level level, UUID entityId, boolean applyStoredEffects) {
        FrozenEntityState state = FROZEN_ENTITIES.remove(entityId);
        if (state == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity entity = serverLevel.getEntity(entityId);
        if (entity == null || !entity.isAlive()) {
            return;
        }
        state.restore(entity);
        if (applyStoredEffects) {
            state.apply(entity);
        }
    }

    public static void releaseAllForBeacon(Level level, BlockPos beaconPos, boolean applyStoredEffects) {
        Iterator<Map.Entry<UUID, FrozenEntityState>> iterator = FROZEN_ENTITIES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, FrozenEntityState> entry = iterator.next();
            if (!entry.getValue().beaconPos.equals(beaconPos)) {
                continue;
            }
            UUID entityId = entry.getKey();
            FrozenEntityState state = entry.getValue();
            iterator.remove();
            if (!(level instanceof ServerLevel serverLevel)) {
                continue;
            }
            Entity entity = serverLevel.getEntity(entityId);
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            state.restore(entity);
            if (applyStoredEffects) {
                state.apply(entity);
            }
        }
    }

    private static final class FrozenEntityState {
        private final BlockPos beaconPos;
        private final long startGameTime;
        private final Vec3 frozenPos;
        private final float yRot;
        private final float xRot;
        private Vec3 accumulatedMomentum = Vec3.ZERO;
        private float accumulatedDamage;
        private final List<StoredDamage> storedDamage = new ArrayList<>();

        private FrozenEntityState(BlockPos beaconPos, long startGameTime, Vec3 frozenPos, float yRot, float xRot) {
            this.beaconPos = beaconPos.immutable();
            this.startGameTime = startGameTime;
            this.frozenPos = frozenPos;
            this.yRot = yRot;
            this.xRot = xRot;
        }

        private static FrozenEntityState capture(Entity entity, BlockPos beaconPos, long gameTime) {
            return new FrozenEntityState(
                beaconPos,
                gameTime,
                entity.position(),
                entity.getYRot(),
                entity.getXRot()
            );
        }

        private void accumulateDamage(DamageSource source, float damage) {
            this.accumulatedDamage += damage;
            this.storedDamage.add(new StoredDamage(source, damage));
        }

        private void accumulateMomentum(Vec3 momentum) {
            if (momentum.lengthSqr() > 1.0E-8) {
                this.accumulatedMomentum = this.accumulatedMomentum.add(momentum);
            }
        }

        private void restore(Entity entity) {
            entity.moveTo(this.frozenPos.x, this.frozenPos.y, this.frozenPos.z, this.yRot, this.xRot);
            entity.setDeltaMovement(Vec3.ZERO);
            entity.fallDistance = 0.0f;
            if (entity instanceof LivingEntity living) {
                living.invulnerableTime = 0;
            }
            entity.hurtMarked = true;
        }

        private void apply(Entity entity) {
            if (entity instanceof LivingEntity living) {
                for (StoredDamage damage : this.storedDamage) {
                    living.invulnerableTime = 0;
                    living.hurt(damage.source(), damage.amount());
                }
            }
            if (this.accumulatedMomentum.lengthSqr() > 1.0E-8) {
                entity.setDeltaMovement(this.accumulatedMomentum);
                entity.hurtMarked = true;
            }
        }
    }

    private record StoredDamage(DamageSource source, float amount) {
    }
}
