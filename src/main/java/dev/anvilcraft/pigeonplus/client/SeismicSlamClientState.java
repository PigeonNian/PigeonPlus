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

    /**
     * 指向性飞行的每 tick 位移上限，避免瞬移被服务端判为作弊。
     *
     * <p>同时也决定了「飞过去」这个过程有多快：步长越大越像瞬移。
     * 0.96 格/tick 下，飞满射程（{@code SlamManager.TARGET_RANGE} = 15 格）
     * 约需 16 tick（0.8 秒），过程看得出来但不会拖沓。
     */
    private static final double FLIGHT_MAX_STEP = 0.96;

    /**
     * 指向性飞行的最长持续 tick 数（2.5 秒），超过即强制转下砸。
     *
     * <p>兜底用：正常最多十几 tick 就该抵达（步长 0.96 格/tick，射程 15 格 → 约 16 tick）。
     * 若中途被方块卡住（头顶有天花板、落点被填、被活塞推动等），
     * 抵达判定永远不成立，{@code travel} 会一直施加位移，玩家永久悬空。
     * 有这条上限就能保证技能一定会结束。
     */
    private static final int FLIGHT_TIMEOUT_TICKS = 50;

    /**
     * 连续多少 tick 没有实际位移就判定为「卡住」，转下砸。
     *
     * <p>比超时更早生效：被墙顶住时位移为 0，不必干等 2.5 秒。
     * 阈值取 8 tick 是给正常的边角减速留余量（贴近目标时步长会变小）。
     */
    private static final int FLIGHT_STUCK_TICKS = 8;
    /** 判定「本 tick 几乎没有移动」的位移平方阈值。 */
    private static final double FLIGHT_STUCK_EPSILON_SQR = 0.0016;

    /** 进入指向性飞行时的 tick 计数与上次位置，用于兜底判定。 */
    private static int flightTicks;
    private static Vec3 flightLastPos;
    private static int flightStuckTicks;

    /** 兜底触发时，剩余的下落阶段是否已开始（避免重复触发）。 */
    private static boolean flightAborting;

    /**
     * 兜底下落的速度（格/tick）。直接向下，不再朝目标推进。
     */
    private static final double ABORT_FALL_SPEED = 1.2;

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
        // 兜底判定的初始状态
        flightTicks = 0;
        flightLastPos = player.position();
        flightStuckTicks = 0;
        flightAborting = false;
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

        // 兜底下砸阶段：travel mixin 会直直向下推，这里只等触地结算。
        // 必须放在 flightTarget 判断**之外**：进入该阶段时 flightTarget 已被清空，
        // 若写在里面就永远进不来，玩家会一直下落却始终不结算。
        if (flightAborting) {
            if (player.onGround()) {
                triggerImpact();
            }
            return;
        }

        // 指向性飞行：由 travel mixin 施加位移，这里只判断是否抵达
        if (flightTarget != null) {
            double distanceSqr = player.position().distanceToSqr(flightTarget);
            // 足够近，或已经落到地面高度附近 → 视为抵达
            if (distanceSqr <= 0.35 * 0.35 || player.getY() <= flightTarget.y + 0.1) {
                triggerImpact();
                return;
            }

            // 兜底判定：超时 或 连续多 tick 没有实际位移（被卡住）
            flightTicks++;
            double movedSqr = player.position().distanceToSqr(flightLastPos);
            if (movedSqr < FLIGHT_STUCK_EPSILON_SQR) {
                flightStuckTicks++;
            } else {
                flightStuckTicks = 0;
            }
            flightLastPos = player.position();

            if (flightTicks >= FLIGHT_TIMEOUT_TICKS || flightStuckTicks >= FLIGHT_STUCK_TICKS) {
                // 转入下砸：清空 flightTarget，位移改由「直直向下」接管
                flightAborting = true;
                flightTarget = null;
            }
            return;
        }

        if (ignoreGroundTicks > 0) {
            ignoreGroundTicks--;
            return;
        }

        // 已离开地面 → 之后一旦重新触地就是「砸向地面」的时刻
        if (player.onGround()) {
            triggerImpact();
        }
    }

    /**
     * 砸地结算的统一入口。
     *
     * <p>三条路径（指向性抵达、兜底下砸触地、普通裂地触地）都走这里，
     * 保证「请求服务端结算」与「播下砸动画」两件事不会漏掉任何一条。
     */
    private static void triggerImpact() {
        PacketDistributor.sendToServer(new SlamImpactPacket());
        // 先记下此刻举到哪，下砸才能从这个姿态平滑抡下去（否则会瞬时跳变）
        slamStartRaise = raiseProgress(0.0f);
        // 再起手动画并 reset：reset 会清掉 airborne，但下砸计时是独立的，
        // 所以动作能继续播完（见 slamSwingTicks 的说明）
        slamSwingTicks = SLAM_SWING_DURATION;
        reset();
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
     * 是否处于「飞行兜底、正主动下砸」阶段。
     *
     * <p>此阶段 {@link #flightTarget()} 已返回 null，位移由 travel mixin 改为直直向下推，
     * 保证玩家一定会落到地面并完成结算，而不是卡在半空。
     */
    public static boolean isFlightAborting() {
        return flightAborting;
    }

    /**
     * 下砸动作的剩余 tick。
     *
     * <p>刻意与 {@code airborne} 分开：砸地结算时会立刻 {@code reset()}，
     * 若下砸进度也跟着清零，最后一帧的抡击动作根本来不及画出来。
     * 这个计数独立倒计时，让动作播完。
     */
    private static int slamSwingTicks;

    /** 下砸动作时长（tick）。约 0.4 秒，够看清抡下去再收回。 */
    private static final int SLAM_SWING_DURATION = 8;

    /**
     * 下砸开始时锤子已经举到的角度（度）。
     *
     * <p>记下它，下砸才能从「当前举到哪」平滑抡下去。
     * 若固定从举满的角度起手，短促的裂地（滞空很短、锤子还没举起来）会先
     * 突兀地跳到举满姿态再往下抡——那正是「瞬时跳变」的观感来源。
     */
    private static float slamStartRaise;

    /**
     * 举锤进度：0 = 未开始，1 = 举到最高。
     *
     * <p>由滞空时长驱动，所以「飞得越久举得越高」，与伤害随滞空增长一致——
     * 玩家能从手上看出这一发大概有多疼。
     */
    public static float raiseProgress(float partialTick) {
        if (!airborne) return 0.0f;
        float value = (airtimeTicks + partialTick) / RAISE_FULL_TICKS;
        return Math.min(1.0f, Math.max(0.0f, value));
    }

    /** 举锤到达最高所需的滞空 tick 数。 */
    private static final float RAISE_FULL_TICKS = 16.0f;

    /**
     * 下砸动作的归一化进度：0 = 刚起手（仍举着），0.4 = 抡到最低点，1 = 收回原状。
     *
     * <p>分两段而不是「瞬间到位再回落」：后者会让锤子在一帧内从举满跳到砸地，
     * 看着像瞬移而不是挥击。
     */
    public static float slamSwingProgress(float partialTick) {
        if (slamSwingTicks <= 0) return 0.0f;
        float elapsed = SLAM_SWING_DURATION - (slamSwingTicks - partialTick);
        float t = elapsed / SLAM_SWING_DURATION;
        return Math.min(1.0f, Math.max(0.0f, t));
    }

    /**
     * 下砸起手时锤子所在的角度。
     */
    public static float slamStartRaise() {
        return slamStartRaise;
    }

    /**
     * 是否正在播放下砸动作（供手部动画判断）。
     */
    public static boolean isSlamSwinging() {
        return slamSwingTicks > 0;
    }

    /**
     * 客户端每 tick 递减下砸动作计时。
     */
    public static void tickSlamSwing() {
        if (slamSwingTicks > 0) slamSwingTicks--;
    }

    /**
     * 兜底下落速度（格/tick）。
     */
    public static double abortFallSpeed() {
        return ABORT_FALL_SPEED;
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
        flightAborting = false;
        flightTicks = 0;
        flightStuckTicks = 0;
        flightLastPos = null;
        airtimeTicks = 0;
    }
}
