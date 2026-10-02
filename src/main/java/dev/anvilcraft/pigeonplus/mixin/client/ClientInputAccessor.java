package dev.anvilcraft.pigeonplus.mixin.client;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@link ClientInput} 的字段访问器。
 *
 * <h3>为什么需要它</h3>
 * 26.1 把输入状态的两个字段 {@code keyPresses} 与 {@code moveVector}
 * 声明在父类 {@link ClientInput} 上，而 {@code KeyboardInput#tick()}
 * <b>并不调用 {@code super.tick()}</b>，只是直接写入这两个继承字段。
 *
 * <p>于是想「锁住移动」就必须：
 * <ol>
 *   <li>注入 {@code KeyboardInput#tick}——字段是在那里被写入的；</li>
 *   <li>但 {@code @Shadow} <b>无法解析继承字段</b>
 *       （会报 {@code @Shadow field ... was not located in the target class}）；</li>
 *   <li>{@code keyPresses} 虽是 public，{@code moveVector} 却是 protected，
 *       跨包的 mixin 无法直接赋值。</li>
 * </ol>
 *
 * <p>标准做法是给<b>字段真正的声明处</b>（即 {@code ClientInput}）加一个
 * accessor mixin 暴露 setter，再在 {@code KeyboardInputMixin} 里
 * 把自己转型成该接口调用。这样既不改动目标类的可见性，
 * 也不需要反射。
 */
@Mixin(ClientInput.class)
public interface ClientInputAccessor {
    /** 写入按键状态（前进/后退/跳跃/潜行等）。 */
    @Accessor("keyPresses")
    void pigeonplus$setKeyPresses(Input keyPresses);

    /** 写入移动向量（实际位移方向与力度）。 */
    @Accessor("moveVector")
    void pigeonplus$setMoveVector(Vec2 moveVector);
}
