package dev.anvilcraft.pigeonplus.mixin.client;

import dev.anvilcraft.pigeonplus.client.sound.NozzleJetSoundController;
import dev.anvilcraft.pigeonplus.util.NozzlePlasmaJetUtil;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlasmaJetsBlockEntity.class)
public abstract class PlasmaJetsBlockEntityClientMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private static void pigeonplus$tickNozzleJetSound(
        Level level,
        BlockPos ignored,
        BlockState ignoredState,
        PlasmaJetsBlockEntity entity,
        CallbackInfo ci
    ) {
        if (level instanceof ClientLevel clientLevel) {
            BlockPos pos = entity.getBlockPos();
            LargeCauldronBlockEntity cauldron = NozzlePlasmaJetUtil.getStructuralCauldron(clientLevel, pos);
            if (cauldron != null) {
                NozzleJetSoundController.tick(pos);
            }
            NozzleJetSoundController.cleanup();
        }
    }
}
