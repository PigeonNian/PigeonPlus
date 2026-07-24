package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.block.entity.AnvilPumpBlockEntity;
import dev.anvilcraft.pigeonplus.block.entity.StasisBeaconBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
public class FallingBlockEntityMixin {
    private static final double ANVIL_PUMP_NOTIFY_HEIGHT = 0.5D;

    @Unique
    private boolean pigeonplus$notifiedAnvilPump;
    @Unique
    private boolean pigeonplus$recordedInitialFallingPosition;
    @Unique
    private double pigeonplus$initialFallingX;
    @Unique
    private double pigeonplus$initialFallingY;
    @Unique
    private double pigeonplus$initialFallingZ;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$holdStasisFallingBlockAtInitialHeight(CallbackInfo ci) {
        FallingBlockEntity entity = (FallingBlockEntity) (Object) this;
        if (!this.pigeonplus$recordedInitialFallingPosition) {
            this.pigeonplus$recordedInitialFallingPosition = true;
            this.pigeonplus$initialFallingX = entity.getX();
            this.pigeonplus$initialFallingY = entity.getY();
            this.pigeonplus$initialFallingZ = entity.getZ();
        }

        Level level = entity.level();
        if (!level.isClientSide() || !StasisBeaconBlockEntity.isInActiveBeam(level, entity)) {
            return;
        }

        entity.moveTo(
            this.pigeonplus$initialFallingX,
            this.pigeonplus$initialFallingY,
            this.pigeonplus$initialFallingZ,
            entity.getYRot(),
            entity.getXRot()
        );
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0.0f;
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void pigeonplus$startAnvilPumpPistonAnimation(CallbackInfo ci) {
        if (this.pigeonplus$notifiedAnvilPump) {
            return;
        }
        FallingBlockEntity entity = (FallingBlockEntity) (Object) this;
        if (!(entity.getBlockState().getBlock() instanceof AnvilBlock)) {
            return;
        }
        Level level = entity.level();
        if (!level.isClientSide()) {
            return;
        }

        int x = (int) Math.floor(entity.getX());
        int z = (int) Math.floor(entity.getZ());
        int minY = (int) Math.floor(entity.getY() - 1.0D - ANVIL_PUMP_NOTIFY_HEIGHT);
        int maxY = (int) Math.floor(entity.getY() - 1.0D);
        for (int y = maxY; y >= minY; y--) {
            BlockPos pumpPos = new BlockPos(x, y, z);
            if (
                isWithinNotifyHeight(entity, pumpPos)
                    && level.getBlockEntity(pumpPos) instanceof AnvilPumpBlockEntity pump
            ) {
                pump.startPistonPressAnimation();
                this.pigeonplus$notifiedAnvilPump = true;
                return;
            }
        }
    }

    private static boolean isWithinNotifyHeight(FallingBlockEntity entity, BlockPos pumpPos) {
        double distanceFromPumpTop = entity.getY() - (pumpPos.getY() + 1.0D);
        return distanceFromPumpTop >= 0.0D && distanceFromPumpTop <= ANVIL_PUMP_NOTIFY_HEIGHT;
    }
}
