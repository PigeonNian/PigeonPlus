package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.pigeonplus.block.ElectricBlenderBlock;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class HeaterCompat {
    private HeaterCompat() {
    }

    public static boolean isHeatingElectricBlender(BlockState state) {
        return state.is(AddonBlocks.ELECTRIC_BLENDER.get())
            && state.getOptionalValue(ElectricBlenderBlock.HEATING).orElse(false);
    }

    public static boolean acceptsHeater(BlockStatePredicate predicate) {
        for (Holder<Block> holder : predicate.getBlocks()) {
            if (holder.value() == ModBlocks.HEATER.get()) {
                return true;
            }
        }
        return false;
    }
}
