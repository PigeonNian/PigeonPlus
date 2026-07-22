package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.util.GasEscapeUtil;
import dev.anvilcraft.pigeonplus.util.NozzlePlasmaJetUtil;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LargeCauldronBlockEntity.class)
public class LargeCauldronBlockEntityMixin {
    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void pigeonplus$escapeGas(
        Level level,
        BlockPos pos,
        BlockState state,
        LargeCauldronBlockEntity entity,
        CallbackInfo ci
    ) {
        if (entity.isMainPart()) {
            GasEscapeUtil.escapeLargeCauldronGas(level, pos, entity.getFluids());
            NozzlePlasmaJetUtil.trySpawn(level, entity);
        }
    }
}
