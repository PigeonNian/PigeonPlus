package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.event.BiogasDrainSpawningManager;
import dev.anvilcraft.pigeonplus.util.GasEscapeUtil;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.entity.fluid.DrainBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrainBlockEntity.class)
public class DrainBlockEntityMixin implements IHasAffectRange {
    @Inject(method = "tick", at = @At("TAIL"))
    private static void pigeonplus$escapeStoredGas(
        Level level,
        BlockPos pos,
        BlockState state,
        DrainBlockEntity entity,
        CallbackInfo ci
    ) {
        if (!level.isClientSide() && GasEscapeUtil.hasStoredBiogas(entity.getFluidHandler())) {
            BiogasDrainSpawningManager.addBiogasDrain(level, pos);
        }
        GasEscapeUtil.escapeDrainGas(level, pos, entity.getFluidHandler());
    }

    @Override
    public AABB shape() {
        DrainBlockEntity entity = (DrainBlockEntity) (Object) this;
        if (!GasEscapeUtil.hasStoredBiogas(entity.getFluidHandler())) {
            return null;
        }
        return BiogasDrainSpawningManager.blockingArea(entity.getBlockPos());
    }
}
