package dev.anvilcraft.pigeonplus.mixin.client;

import dev.anvilcraft.pigeonplus.client.UppercutClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 上勾拳上升期间禁用玩家移动操作。
 *
 * <h3>26.1 的输入模型</h3>
 * 旧版 {@code Input} 是带 {@code forwardImpulse}/{@code jumping} 等可变字段的类，
 * 直接改字段即可。26.1 拆成两个概念：
 * <ul>
 *   <li>{@code Input}（不可变 record）——「按下了哪些键」；</li>
 *   <li>{@code ClientInput#moveVector}——实际位移方向与力度。</li>
 * </ul>
 * 因此锁住移动要同时清掉这两者。
 *
 * <h3>为什么注入点选在 KeyboardInput</h3>
 * {@code KeyboardInput#tick()} 覆写父类且<b>不调用 {@code super.tick()}</b>，
 * 两个字段正是在该方法末尾被写入的。注入 {@code @At("RETURN")}
 * 才能在其写完之后清零。
 *
 * <p>字段本身声明在父类 {@link ClientInput} 上，{@code @Shadow} 无法解析继承字段，
 * 故通过 {@link ClientInputAccessor} 写入。
 */
@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private void pigeonplus$lockInputDuringUppercut(CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (!UppercutClientState.isAscending(player)) {
            return;
        }
        // 清空按键（含跳跃与潜行），并把移动向量归零
        ClientInputAccessor accessor = (ClientInputAccessor) (Object) this;
        accessor.pigeonplus$setKeyPresses(Input.EMPTY);
        accessor.pigeonplus$setMoveVector(Vec2.ZERO);
    }
}
