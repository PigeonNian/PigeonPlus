package dev.anvilcraft.pigeonplus.util;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 裂地重拳「向前跃起」的初速真值表（客户端侧）。
 *
 * <p>与火箭重拳冲刺、上勾拳上升同源：玩家位置由客户端权威决定，位移在客户端执行，
 * 服务端只负责伤害结算与冷却。
 *
 * <p>与另外两个技能的区别：这里<strong>只接管起跳的那一瞬</strong>，随后把速度交还给原版物理。
 * 原因：站在地面上直接 {@code setDeltaMovement}，原版 {@code travel} 会先按方块摩擦
 * 把水平速度乘掉 0.6（这正是冲刺当初必须接管 travel 的理由）。但整段接管又会失去
 * 空中操控与自然抛物线。折中做法是只在起跳那几 tick 手动位移，把人抬离地面，
 * 之后 {@code onGround} 为 false，摩擦不再作用，原版物理自然接续抛物线，
 * 玩家也能左右微调。
 */
public final class SlamLeapRegistry {
    private static final Map<UUID, Leap> LEAPS = new HashMap<>();

    private SlamLeapRegistry() {
    }

    /**
     * 一次前跃的初速。
     *
     * @param direction 水平单位方向
     * @param speed     水平速度（格/tick）
     * @param upSpeed   垂直初速（格/tick）
     * @param ticksLeft 还需手动接管的 tick 数（用完后交还原版物理）
     */
    public record Leap(Vec3 direction, double speed, double upSpeed, int ticksLeft) {
    }

    public static void start(UUID uuid, Vec3 direction, double speed, double upSpeed, int ticks) {
        LEAPS.put(uuid, new Leap(direction, speed, upSpeed, ticks));
    }

    public static Leap get(UUID uuid) {
        return LEAPS.get(uuid);
    }

    public static boolean isLeaping(UUID uuid) {
        return LEAPS.containsKey(uuid);
    }

    public static void stop(UUID uuid) {
        LEAPS.remove(uuid);
    }

    /**
     * 递减剩余接管 tick，归零即移除（此后由原版物理接续）。
     */
    public static void tick(UUID uuid) {
        Leap leap = LEAPS.get(uuid);
        if (leap == null) return;
        if (leap.ticksLeft() <= 1) {
            LEAPS.remove(uuid);
        } else {
            LEAPS.put(uuid, new Leap(leap.direction(), leap.speed(), leap.upSpeed(), leap.ticksLeft() - 1));
        }
    }

    /**
     * 该 tick 的位移量（恒定，不受摩擦影响）。
     */
    public static Vec3 leapVelocity(Leap leap) {
        return new Vec3(
            leap.direction().x * leap.speed(),
            leap.upSpeed(),
            leap.direction().z * leap.speed()
        );
    }
}
