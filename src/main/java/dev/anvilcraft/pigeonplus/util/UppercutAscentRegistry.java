package dev.anvilcraft.pigeonplus.util;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 上勾拳「上升阶段」的真值表。
 *
 * <p>与 {@link RocketPunchDashRegistry} 同源：玩家位置由客户端权威决定，
 * 位移必须在客户端执行，服务端负责伤害与击飞判定。
 *
 * <p>与冲刺的区别：上升是**垂直**位移，且上升期间玩家不能操作移动
 * （由 {@code KeyboardInputMixin} 清零输入实现）。
 */
public final class UppercutAscentRegistry {
    private static final Map<UUID, Ascent> ASCENTS = new HashMap<>();

    private UppercutAscentRegistry() {
    }

    /**
     * 一次上升的位移参数。
     *
     * @param upSpeed        每 tick 上升量（格/tick）
     * @param remainingTicks 剩余 tick 数
     */
    public record Ascent(double upSpeed, int remainingTicks) {
    }

    public static void start(UUID uuid, double upSpeed, int ticks) {
        ASCENTS.put(uuid, new Ascent(upSpeed, ticks));
    }

    public static Ascent get(UUID uuid) {
        return ASCENTS.get(uuid);
    }

    public static boolean isAscending(UUID uuid) {
        return ASCENTS.containsKey(uuid);
    }

    public static void stop(UUID uuid) {
        ASCENTS.remove(uuid);
    }

    /**
     * 递减剩余 tick，归零即移除。
     */
    public static void tick(UUID uuid) {
        Ascent ascent = ASCENTS.get(uuid);
        if (ascent == null) return;
        if (ascent.remainingTicks() <= 1) {
            ASCENTS.remove(uuid);
        } else {
            ASCENTS.put(uuid, new Ascent(ascent.upSpeed(), ascent.remainingTicks() - 1));
        }
    }

    /**
     * 上升阶段的垂直位移向量。
     */
    public static Vec3 ascentVelocity(Ascent ascent) {
        return new Vec3(0.0, ascent.upSpeed(), 0.0);
    }
}
