package dev.anvilcraft.pigeonplus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.Map;

public final class GasLiquefactionTracker {
    private static final Map<Key, Integer> PROGRESS = new HashMap<>();

    private GasLiquefactionTracker() {
    }

    public static int addGasInput(Level level, BlockPos pos, Fluid gas, Fluid output, int amount, int ratio) {
        if (amount <= 0 || ratio <= 0) {
            return 0;
        }
        Key key = new Key(level.dimension(), pos.immutable(), gas, output);
        int progress = PROGRESS.getOrDefault(key, 0) + amount;
        int liquidAmount = progress / ratio;
        int remainder = progress % ratio;
        if (remainder > 0) {
            PROGRESS.put(key, remainder);
        } else {
            PROGRESS.remove(key);
        }
        return liquidAmount;
    }

    public static void clear(Level level, BlockPos pos, Fluid gas) {
        PROGRESS.keySet().removeIf(
            key -> key.dimension().equals(level.dimension())
                && key.pos().equals(pos.immutable())
                && key.gas().isSame(gas)
        );
    }

    private record Key(ResourceKey<Level> dimension, BlockPos pos, Fluid gas, Fluid output) {
    }
}
