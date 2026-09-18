package dev.anvilcraft.pigeonplus.mixin.client;

import dev.anvilcraft.pigeonplus.client.UppercutClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 上勾拳上升期间禁用玩家移动操作。
 *
 * <p>注入 {@code KeyboardInput#tick}（而不是更通用的 {@code Input}）：移动输入最终由
 * {@code leftImpulse}/{@code forwardImpulse} 表达，这两个字段正是该方法写入的。
 * 在它写完之后清零，玩家按键就完全不影响位移。
 *
 * <p>同时清掉 {@code jumping}，避免上升途中再叠加一次跳跃。
 */
@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void pigeonplus$lockInputDuringUppercut(boolean isSneaking, float sneakingSpeedMultiplier, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!UppercutClientState.isAscending(player)) return;

        Input input = (Input) (Object) this;
        input.leftImpulse = 0.0f;
        input.forwardImpulse = 0.0f;
        input.jumping = false;
        // 上升期间不潜行，避免“上升时还在慢慢挪”
        input.shiftKeyDown = false;
    }
}
