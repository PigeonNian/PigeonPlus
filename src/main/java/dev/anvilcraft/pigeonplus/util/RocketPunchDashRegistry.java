package dev.anvilcraft.pigeonplus.util;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 火箭重拳冲刺的位移真值表（客户端与服务端共用）。
 *
 * <p>为什么位移要放在客户端：玩家位置由<strong>客户端权威</strong>决定并每 tick 上报，
 * 服务端单方面 {@code move()} 会被客户端下一个位置包覆盖，表现为回弹、拉扯。
 * 因此真正的位移在 {@code LivingEntityTravelMixin} 里执行，服务端则依赖客户端上报的位置
 * 做命中判定（这正是原版对玩家移动的处理方式）。
 *
 * <p>用 UUID 而非实体 id 作键：实体 id 在客户端与服务端并不一致。
 */
public final class RocketPunchDashRegistry {
    private static final Map<UUID, Dash> DASHES = new HashMap<>();

    private RocketPunchDashRegistry() {
    }

    /**
     * 一次冲刺的位移参数。
     *
     * @param direction      水平单位方向向量
     * @param speed          每 tick 位移量（米），恒定即为匀速
     * @param baseYaw        释放瞬间朝向，视角锁定以此为基准
     * @param remainingTicks 剩余 tick 数
     */
    public record Dash(Vec3 direction, double speed, float baseYaw, int remainingTicks) {
    }

    public static void start(UUID uuid, Vec3 direction, double speed, int ticks, float baseYaw) {
        DASHES.put(uuid, new Dash(direction, speed, baseYaw, ticks));
    }

    public static Dash get(UUID uuid) {
        return DASHES.get(uuid);
    }

    public static boolean isDashing(UUID uuid) {
        return DASHES.containsKey(uuid);
    }

    public static void stop(UUID uuid) {
        DASHES.remove(uuid);
    }

    /**
     * 递减剩余 tick，归零即移除。由位移 mixin 每次套用位移后调用。
     */
    public static void tick(UUID uuid) {
        Dash dash = DASHES.get(uuid);
        if (dash == null) return;
        if (dash.remainingTicks() <= 1) {
            DASHES.remove(uuid);
        } else {
            DASHES.put(
                uuid,
                new Dash(dash.direction(), dash.speed(), dash.baseYaw(), dash.remainingTicks() - 1)
            );
        }
    }
}
