package dev.anvilcraft.pigeonplus.mixin.client;

import dev.anvilcraft.pigeonplus.client.UppercutClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 上勾拳上升期间禁用玩家移动操作。
 *
 * <h3>26.1 的输入模型变化</h3>
 * 旧版 {@code Input} 是带 {@code forwardImpulse}/{@code jumping} 等可变字段的类，
 * 直接改字段即可。26.1 拆成了两个概念：
 * <ul>
 *   <li>{@code net.minecraft.world.entity.player.Input} 变成了<b>不可变 record</b>
 *       （forward/backward/left/right/jump/shift/sprint 七个布尔），
 *       代表「按下了哪些键」，只读。</li>
 *   <li>移动向量由 {@code ClientInput#moveVector}({@code Vec2}) 表达，
 *       即实际位移方向与力度。</li>
 * </ul>
 * 因此「锁住移动」需要同时清掉这两者：把 {@code keyPresses} 换成
 * {@link Input#EMPTY}（顺带清掉跳跃），并把 {@code moveVector} 归零。
 *
 * <p>注入 {@code KeyboardInput#tick} 的 TAIL：该方法写完输入状态之后立刻清零，
 * 玩家这一帧的按键就完全不影响位移。
 */
@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    /** 父类 {@code ClientInput} 的字段，需 shadow 才能访问。 */
    @Shadow
    public Input keyPresses;

    @Shadow
    protected Vec2 moveVector;

    @Inject(method = "tick", at = @At("TAIL"))
    private void pigeonplus$lockInputDuringUppercut(CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (!UppercutClientState.isAscending(player)) {
            return;
        }
        // 清空按键（含跳跃与潜行），并把移动向量归零
        this.keyPresses = Input.EMPTY;
        this.moveVector = Vec2.ZERO;
    }
}
