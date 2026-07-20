package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.util.GasEscapeUtil;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishTankBlockEntity.class)
public class FishTankBlockEntityMixin {
    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void pigeonplus$escapeGas(
        Level level,
        BlockPos pos,
        BlockState state,
        FishTankBlockEntity entity,
        CallbackInfo ci
    ) {
        GasEscapeUtil.escapeFishTankGas(level, pos, entity.getFluidHandler());
    }
}
