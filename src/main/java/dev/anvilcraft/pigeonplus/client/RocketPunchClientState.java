package dev.anvilcraft.pigeonplus.client;

import dev.anvilcraft.pigeonplus.network.RocketPunchCancelPacket;
import dev.anvilcraft.pigeonplus.util.RocketPunchDashRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 客户端侧的火箭重拳冲刺状态：匀速位移 + 视角锁定。
 *
 * <p>玩家位置与朝向由客户端权威决定，因此位移和视角夹紧都必须在这里做，
 * 服务端只下发参数（见 {@code RocketPunchDashPacket}）。
 */
public final class RocketPunchClientState {
    /** 冲刺期间允许的视角偏移上限（度）。 */
    public static final float MAX_LOOK_OFFSET_DEGREES = 45.0f;

    /**
     * 跳跃取消后继承的动量（格/tick）。
     *
     * <p>注意这与冲刺速度（45 米/秒 = 2.25 格/tick）不同：取消后只保留 1.25 格/tick，
     * 相当于 25 米/秒。
     */
    public static final double CANCEL_MOMENTUM = 1.25;

    private RocketPunchClientState() {
    }

    /**
     * 进入冲刺：登记位移参数，由 {@code LivingEntityTravelMixin} 每 tick 套用。
     */
    public static void startDash(Vec3 direction, double speed, int ticks, float baseYaw) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        RocketPunchDashRegistry.start(player.getUUID(), direction, speed, ticks, baseYaw);
    }

    /**
     * 立即中断冲刺（撞到生物时由服务端下发）。
     */
    public static void stopDash() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            RocketPunchDashRegistry.stop(player.getUUID());
        }
    }

    /**
     * 客户端每 tick 调用：先夹视角，再处理跳跃取消。
     *
     * <p>位移本身由 {@code LivingEntityTravelMixin} 处理——放在 travel 阶段才不会
     * 被随后的摩擦力与重力改写。
     */
    public static void clientTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        RocketPunchDashRegistry.Dash dash = RocketPunchDashRegistry.get(player.getUUID());
        if (dash == null) return;

        // 先夹视角再做其它判断：这样取消时取到的视线一定落在 ±45° 锥形内，
        // 不会因为这一 tick 的大幅甩鼠标而让动量方向超出范围。
        clampLook(player, dash.baseYaw());

        // 跳跃取消：按下跳跃键就中断冲刺，并把动量交还给常规物理
        if (Minecraft.getInstance().options.keyJump.isDown()) {
            cancelDash(player);
        }
    }

    /**
     * 把视角夹回冲刺方向左右 {@value #MAX_LOOK_OFFSET_DEGREES}° 的锥形内。
     *
     * <p>只改 yaw 不动 pitch，保留玩家上下看的自由。
     */
    private static void clampLook(LocalPlayer player, float baseYaw) {
        float delta = Mth.wrapDegrees(player.getYRot() - baseYaw);
        float clamped = Mth.clamp(delta, -MAX_LOOK_OFFSET_DEGREES, MAX_LOOK_OFFSET_DEGREES);
        if (clamped == delta) return;
        float target = Mth.wrapDegrees(baseYaw + clamped);
        player.setYRot(target);
        // 同步上一帧角度，避免渲染插值把镜头甩出去
        player.yRotO = target;
    }

    /**
     * 跳跃取消：断开冲刺状态，把动量转成普通速度并带上向上跳跃。
     *
     * <p>两步缺一不可：
     * <ol>
     *   <li><strong>先移除</strong>冲刺记录——否则 {@code LivingEntityTravelMixin} 下一 tick
     *       仍会接管移动并覆盖掉我们刚设的速度；</li>
     *   <li>再设置速度，交给原版 {@code travel()} 处理摩擦与重力，
     *       这样“继承的动能”会自然衰减，而不是被硬写成恒定值。</li>
     * </ol>
     *
     * <p>动量方向取<strong>玩家当前视角</strong>的水平朝向，而不是冲刺方向：
     * 冲刺期间视角本来就可以在冲刺方向左右 45° 内微调，按视角给动能才能让这个微调有意义
     * （想让取消后往哪飞，就在冲刺时把镜头往哪偏）。因为视角已被夹在锥形内，
     * 取视线方向不会产生突兀的大角度转向。
     */
    private static void cancelDash(LocalPlayer player) {
        if (RocketPunchDashRegistry.get(player.getUUID()) == null) return;

        // 1) 先断开冲刺，让下一次 travel 走回原版逻辑
        RocketPunchDashRegistry.stop(player.getUUID());
        // 同步给服务端，否则它仍会继续做命中判定（玩家已停下却还在打人）
        PacketDistributor.sendToServer(new RocketPunchCancelPacket());

        // 2) 按当前视角的水平朝向保留动量；视线几乎垂直时退化为“仅按 yaw 求水平朝向”，
        //    避免 normalize 出现零向量。用 directionFromRotation 而不是手写三角函数，
        //    免得角度符号与游戏约定不一致。
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            horizontal = Vec3.directionFromRotation(0.0f, player.getYRot());
        }
        horizontal = horizontal.normalize().scale(CANCEL_MOMENTUM);
        player.setDeltaMovement(horizontal.x, player.getDeltaMovement().y, horizontal.z);
        // 3) 带上向上跳跃（jumpFromGround 会保留已有的水平速度）
        player.jumpFromGround();
        player.hurtMarked = true;
    }
}
