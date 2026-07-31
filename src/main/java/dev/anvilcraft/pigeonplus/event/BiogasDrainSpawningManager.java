package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.util.GasEscapeUtil;
import dev.dubhe.anvilcraft.block.entity.fluid.DrainBlockEntity;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class BiogasDrainSpawningManager {
    public static final double BLOCKING_RANGE = 15.0;

    private static final Map<Level, BiogasDrainSpawningManager> INSTANCES = new HashMap<>();

    private final Set<BlockPos> biogasDrains = Collections.synchronizedSet(new ObjectOpenHashSet<>());
    private final Level level;

    private BiogasDrainSpawningManager(Level level) {
        this.level = level;
    }

    public static void addBiogasDrain(Level level, BlockPos pos) {
        getInstance(level).biogasDrains.add(pos.immutable());
    }

    private static BiogasDrainSpawningManager getInstance(Level level) {
        return INSTANCES.computeIfAbsent(level, BiogasDrainSpawningManager::new);
    }

    public static AABB blockingArea(BlockPos pos) {
        return AABB.ofSize(pos.getCenter(), BLOCKING_RANGE, BLOCKING_RANGE, BLOCKING_RANGE);
    }

    @SubscribeEvent
    private static void blockMonsterSpawn(MobSpawnEvent.PositionCheck event) {
        MobSpawnType spawnType = event.getSpawnType();
        if (!spawnType.equals(MobSpawnType.NATURAL)
            && !spawnType.equals(MobSpawnType.CHUNK_GENERATION)
            && !spawnType.equals(MobSpawnType.PATROL)) {
            return;
        }
        Mob mob = event.getEntity();
        if (!(mob instanceof Enemy)) {
            return;
        }
        Entity entity = event.getEntity();
        Level level = entity.level();
        BiogasDrainSpawningManager manager = getInstance(level);
        Iterator<BlockPos> iterator = manager.biogasDrains.iterator();
        while (iterator.hasNext()) {
            BlockPos drainPos = iterator.next();
            if (!level.isLoaded(drainPos)) {
                continue;
            }
            if (!(level.getBlockEntity(drainPos) instanceof DrainBlockEntity drain)
                || !GasEscapeUtil.hasStoredBiogas(drain.getFluidHandler())) {
                iterator.remove();
                continue;
            }
            if (blockingArea(drainPos).contains(entity.position())) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                return;
            }
        }
    }
}
