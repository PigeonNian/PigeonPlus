package dev.anvilcraft.pigeonplus.entity;

import dev.anvilcraft.lib.v2.explosion.AnvilLibExplosion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LargeTntExplosion {
    private static final int BLOCKS_PER_TICK = 512;
    private static final float DROP_CHANCE = 0.3F;
    private static final double MAX_PROBABILITY = 0.8;

    private LargeTntExplosion() {
    }

    public static void detonate(ServerLevel level, Entity source, double centerX, double centerY, double centerZ) {
        List<BlockPos> targets = collect(BlockPos.containing(centerX, centerY, centerZ));
        damageEntities(level, source, centerX, centerY, centerZ);
        new Session(level, source, centerX, centerY, centerZ, targets).start();
    }

    private static List<BlockPos> collect(BlockPos center) {
        int radius = LargeTntEntity.MELTING_RADIUS;
        int radiusSqr = radius * radius;
        List<BlockPos> targets = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSqr) {
                        continue;
                    }
                    targets.add(center.offset(dx, dy, dz));
                }
            }
        }
        targets.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(center.getX() + 0.5, center.getY() + 0.5,
            center.getZ() + 0.5)));
        return targets;
    }

    private static void damageEntities(ServerLevel level, Entity source, double x, double y, double z) {
        int radius = LargeTntEntity.BLAST_RADIUS;
        AABB area = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        Vec3 center = new Vec3(x, y, z);
        float damage = radius * 0.5F;
        for (Entity entity : level.getEntities(source, area)) {
            if (entity.position().distanceTo(center) > radius) {
                continue;
            }
            entity.hurt(level.damageSources().explosion(source, source), damage);
        }
    }

    private static double probability(double distance, double inner, double outer) {
        if (outer <= inner) {
            return 0.0;
        }
        if (distance <= inner) {
            return MAX_PROBABILITY;
        }
        if (distance >= outer) {
            return 0.0;
        }
        return MAX_PROBABILITY * (1.0 - (distance - inner) / (outer - inner));
    }

    private static final class Session {
        private final ServerLevel level;
        private final Entity source;
        private final double centerX;
        private final double centerY;
        private final double centerZ;
        private final List<BlockPos> targets;
        private int index;

        private Session(ServerLevel level, Entity source, double centerX, double centerY, double centerZ,
                        List<BlockPos> targets) {
            this.level = level;
            this.source = source;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.targets = targets;
        }

        private void start() {
            NeoForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public void onServerTick(ServerTickEvent.Post event) {
            int processed = 0;
            while (this.index < this.targets.size() && processed < BLOCKS_PER_TICK) {
                BlockPos pos = this.targets.get(this.index++);
                processed++;
                this.process(pos);
            }
            if (this.index >= this.targets.size()) {
                NeoForge.EVENT_BUS.unregister(this);
            }
        }

        private void process(BlockPos pos) {
            if (!this.level.isLoaded(pos)) {
                return;
            }
            BlockState state = this.level.getBlockState(pos);
            if (state.isAir()) {
                return;
            }
            double distance = Math.sqrt(pos.distToCenterSqr(this.centerX, this.centerY, this.centerZ));
            if (distance <= LargeTntEntity.BLAST_RADIUS) {
                this.destroy(pos, state);
                return;
            }
            // 外圈只侵蚀暴露在外的方块，避免深处被随机掏洞。
            if (!isExposed(this.level, pos)) {
                return;
            }
            if (distance <= LargeTntEntity.PROBABILITY_RADIUS) {
                double chance = probability(distance, LargeTntEntity.BLAST_RADIUS,
                    LargeTntEntity.PROBABILITY_RADIUS);
                if (this.level.random.nextDouble() < chance) {
                    this.destroy(pos, state);
                }
                return;
            }
            if (distance <= LargeTntEntity.MELTING_RADIUS) {
                double chance = probability(distance, LargeTntEntity.PROBABILITY_RADIUS,
                    LargeTntEntity.MELTING_RADIUS);
                if (this.level.random.nextDouble() < chance) {
                    this.melt(pos, state);
                }
            }
        }

        private void destroy(BlockPos pos, BlockState state) {
            // 基岩这类负硬度方块不处理。
            if (state.getDestroySpeed(this.level, pos) < 0) {
                return;
            }
            // 只有 30% 的方块掉落战利品，避免一次爆炸在地面堆出上千个物品实体。
            if (this.level.random.nextFloat() < DROP_CHANCE) {
                BlockEntity blockEntity = state.hasBlockEntity() ? this.level.getBlockEntity(pos) : null;
                Block.dropResources(state, this.level, pos, blockEntity, this.source, ItemStack.EMPTY);
            }
            FluidState fluid = this.level.getFluidState(pos);
            this.level.setBlock(pos, fluid.createLegacyBlock(), Block.UPDATE_ALL, 512);
            this.level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(this.source, state));
        }

        private void melt(BlockPos pos, BlockState state) {
            Block melted = AnvilLibExplosion.MELTING_CACHE.get(state.getBlock());
            if (melted == null || state.getDestroySpeed(this.level, pos) < 0) {
                return;
            }
            this.level.setBlock(pos, melted.defaultBlockState(), Block.UPDATE_ALL, 512);
            this.level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(this.source, state));
        }
    }

    private static boolean isExposed(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).isAir()) {
                return true;
            }
        }
        return false;
    }
}
