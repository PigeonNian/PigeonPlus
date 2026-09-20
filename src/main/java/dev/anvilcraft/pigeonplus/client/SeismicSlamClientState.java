package dev.anvilcraft.pigeonplus.client;

import dev.anvilcraft.pigeonplus.network.SlamImpactPacket;
import dev.anvilcraft.pigeonplus.network.SlamRequestPacket;
import dev.anvilcraft.pigeonplus.util.AirborneUtil;
import dev.anvilcraft.pigeonplus.util.DoomfistEnchantmentUtil;
import dev.anvilcraft.pigeonplus.util.RocketPunchManager;
import dev.anvilcraft.pigeonplus.util.SkillCooldowns;
import dev.anvilcraft.pigeonplus.util.SlamLeapRegistry;
import dev.anvilcraft.pigeonplus.util.SlamManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 客户端侧的裂地重拳状态：E 键触发 + 前跃 + 落地检测。
 *
 * <p>落地必须由客户端检测：玩家位置客户端权威，只有客户端知道何时真正触地。
 * 检测到落地后请求服务端结算伤害（伤害与冷却在服务端）。
 */
public final class SeismicSlamClientState {
    /**
     * 起跳后需要忽略落地判定的 tick 数。
     *
     * <p>起跳的第一 tick 玩家可能仍被判定为在地面上，若不忽略会立刻触发砸地，
     * 整个技能退化成原地一跺脚。
     */
    private static final int IGNORE_GROUND_TICKS = 3;

    /** 起跳后的剩余忽略 tick。 */
    private static int ignoreGroundTicks;
    /** 是否正处于「已跃起、等待落地」的状态。 */
    private static boolean airborne;

    /**
     * 指向性飞行的目标落点，null 表示当前不是指向性。
     */
    private static Vec3 flightTarget;

    /**
     * 已滞空的 tick 数。
     *
     * <p>仅用于准星下方的伤害指示器：数值随滞空增长，让玩家知道「再飘一会儿能打更疼」。
     * 实际结算时的伤害由服务端按<strong>它自己记录的</strong>起跳时刻计算，不采信这个值。
     */
    private static int airtimeTicks;

    /** 指向性飞行的每 tick 位移上限，避免瞬移被服务端判为作弊。 */
    private static final double FLIGHT_MAX_STEP = 1.6;

    private SeismicSlamClientState() {
    }

    /**
     * E 键按下时调用：判断能否释放，可以就请求服务端。
     *
     * <p>这里的判据与 {@code SlamKeyHandler.canCast} 一致（那里是主门禁，
     * 不可用时 E 会退回原版开背包行为），此处再查一遍是防御。
     *
     * <p>刻意<strong>不</strong>排除上勾拳的滞空：那是指向性裂地的设计前提
     * （「上挑 → 飞扑」连招）。只排除上升这一位移阶段。
     */
    public static void requestIfAvailable(LocalPlayer player) {
        if (!DoomfistEnchantmentUtil.isWieldingDoomfist(player)) return;
        // 位移类技能进行中不叠加
        if (SlamLeapRegistry.isLeaping(player.getUUID())) return;
        if (UppercutClientState.isAscending(player)) return;
        if (RocketPunchManager.isDashing(player)) return;
        if (player.isUsingItem()) return;
        if (SkillCooldowns.isOnCooldownClient(SkillCooldowns.Skill.SEISMIC_SLAM)) return;

        // 悬空时请求指向性；服务端会再校验一次，不满足则回退为普通裂地
        boolean requested = AirborneUtil.isAirborne(player);
        PacketDistributor.sendToServer(new SlamRequestPacket(requested));
    }

    /**
     * 服务端放行后开始前跃。
     */
    public static void startLeap(Vec3 direction, double speed, double upSpeed, int ticks) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        SlamLeapRegistry.start(player.getUUID(), direction, speed, upSpeed, ticks);
        ignoreGroundTicks = IGNORE_GROUND_TICKS;
        airborne = true;
        flightTarget = null;
        airtimeTicks = 0;
        // 告知 common 侧的门禁：客户端已进入裂地位移
        SlamManager.markBusy(player.getUUID());
    }

    /**
     * 服务端放行后开始指向性飞行：朝目标点直线推进，抵达即砸地。
     */
    public static void startTargetedFlight(Vec3 target) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        // 不走 SlamLeapRegistry：那条路径是固定速度的抛物线，
        // 这里要的是「朝一个点持续逼近」，方向每 tick 都要重算（目标固定，位置在变）
        flightTarget = target;
        ignoreGroundTicks = 0;
        airborne = true;
        airtimeTicks = 0;
        // 告知 common 侧的门禁：客户端已进入裂地位移
        SlamManager.markBusy(player.getUUID());
    }

    /**
     * 客户端每 tick：维护状态，检测落地并请求结算。
     */
    public static void clientTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            reset();
            return;
        }
        if (!airborne) return;

        // 累加滞空时间：伤害指示器随它增长
        airtimeTicks++;

        // 指向性飞行：由 travel mixin 施加位移，这里只判断是否抵达
        if (flightTarget != null) {
            double distanceSqr = player.position().distanceToSqr(flightTarget);
            // 足够近，或已经落到地面高度附近 → 视为抵达
            if (distanceSqr <= 0.35 * 0.35 || player.getY() <= flightTarget.y + 0.1) {
                PacketDistributor.sendToServer(new SlamImpactPacket());
                reset();
            }
            return;
        }

        if (ignoreGroundTicks > 0) {
            ignoreGroundTicks--;
            return;
        }

        // 已离开地面 → 之后一旦重新触地就是「砸向地面」的时刻
        if (player.onGround()) {
            PacketDistributor.sendToServer(new SlamImpactPacket());
            reset();
        }
    }

    /**
     * 指向性飞行的目标落点；null 表示当前不是指向性模式。
     */
    public static Vec3 flightTarget() {
        return flightTarget;
    }

    /**
     * 指向性飞行的每 tick 位移上限。
     */
    public static double flightMaxStep() {
        return FLIGHT_MAX_STEP;
    }

    /**
     * 是否正在裂地重拳的过程中（供 HUD 等使用）。
     */
    public static boolean isAirborne() {
        return airborne;
    }

    /**
     * 已滞空的 tick 数（含 {@code partialTick} 插值），供伤害指示器显示。
     */
    public static float airtimeTicks(float partialTick) {
        return airborne ? airtimeTicks + partialTick : 0.0f;
    }

    /**
     * 清空本地状态（断线、死亡、结算完成）。
     */
    public static void reset() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            SlamLeapRegistry.stop(player.getUUID());
            // 解除 common 侧的门禁标记
            SlamManager.unmarkBusy(player.getUUID());
        }
        ignoreGroundTicks = 0;
        airborne = false;
        flightTarget = null;
        airtimeTicks = 0;
    }
}
