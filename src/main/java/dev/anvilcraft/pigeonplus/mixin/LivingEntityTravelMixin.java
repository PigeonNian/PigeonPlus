package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.client.RocketPunchAnimState;
import dev.anvilcraft.pigeonplus.client.SeismicSlamClientState;
import dev.anvilcraft.pigeonplus.network.RocketPunchCancelPacket;
import dev.anvilcraft.pigeonplus.util.ObstructionUtil;
import dev.anvilcraft.pigeonplus.util.RocketPunchDashRegistry;
import dev.anvilcraft.pigeonplus.util.SlamLeapRegistry;
import dev.anvilcraft.pigeonplus.util.UppercutAscentRegistry;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 火箭重拳冲刺期间，用固定向量接管移动。
 *
 * <p>为什么注入 {@code travel} 而不是每 tick 从外面 {@code setDeltaMovement}：
 * 原版 {@code travel()} 会先按输入与摩擦算出实际位移，把 {@code deltaMovement}
 * 乘以方块摩擦系数（地面上额外 ×0.6）。外部设的速度会被这一步改写，
 * 导致实际速度只有预期的一半多、且随脚下方块变化——既不匀速也受摩擦影响。
 *
 * <p>在 {@code travel} 头部直接接管：清空速度、按恒定向量位移、取消原版逻辑。
 * 这样既没有摩擦也没有重力，每 tick 位移量恒定，是严格匀速。
 *
 * <p>命中生物时立即停止位移。这里做客户端预测：冲刺速度高达 2.25 格/tick，
 * 若只等服务端回包，一来一回（约 1~2 tick）会多冲出去 2~4 格，手感上像是“穿过去了”。
 * 伤害仍由服务端结算，客户端只负责停。
 */
@Mixin(LivingEntity.class)
public class LivingEntityTravelMixin {
    /** 客户端预测命中用的判定半径（格），与 {@code RocketPunchManager} 保持同量级。 */
    private static final double PREDICT_HIT_RADIUS = 1.6;

    /**
     * 判定为「只是擦到」的遮挡比例上限。
     *
     * <p>超过这个比例说明是正面撞墙，应该停下而不是硬翻上去——
     * 否则玩家能靠冲刺穿进厚墙，破坏技能语义。
     */
    private static final double CLIMB_MAX_OBSTRUCTION = 0.5;

    /** 翻越时的最小垂直分速度（格/tick）。挡得越多用得越小。 */
    private static final double CLIMB_MIN_SPEED = 0.35;
    /** 翻越时的最大垂直分速度（格/tick）。只是轻轻蹭到时使用。 */
    private static final double CLIMB_MAX_SPEED = 0.8;

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$rocketPunchDash(Vec3 input, CallbackInfo ci) {
        // 只在该位移的权威侧执行。
        //
        // 这是必须的：位移由客户端权威决定，服务端不该移动玩家；而且这些注册表是 static，
        // 单人生存（客户端与集成服务端同 JVM）下两端会共享同一条记录——若不限定侧，
        // 两端都会套用位移并各自调用 tick()，倒数速度翻倍，最终距离只剩设计值的一半。
        //
        // 判定用「是否为客户端玩家类」而不是 isClientSide()/isLocalPlayer()：
        // 集成服务端的 ServerPlayer 两个标志都不满足，无法与主客户端区分。
        if (!((Object) this instanceof LocalPlayer player)) return;

        // 指向性裂地的兜底下砸：飞行超时或被卡住后转为此阶段，直直向下推，
        // 保证一定会落到地面完成结算，不会卡在半空。
        if (SeismicSlamClientState.isFlightAborting()) {
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0f;
            player.move(MoverType.SELF, new Vec3(0.0, -SeismicSlamClientState.abortFallSpeed(), 0.0));
            ci.cancel();
            return;
        }

        // 指向性裂地：朝固定落点直线逼近。
        // 不用 SlamLeapRegistry，因为方向每 tick 都要重算（目标不动、人在动）。
        Vec3 flightTarget = SeismicSlamClientState.flightTarget();
        if (flightTarget != null) {
            Vec3 toTarget = flightTarget.subtract(player.position());
            double distance = toTarget.length();
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0f;
            if (distance > 1.0E-4) {
                // 每 tick 走固定步长，且不超过剩余距离，避免越过目标来回抖
                double step = Math.min(SeismicSlamClientState.flightMaxStep(), distance);
                player.move(MoverType.SELF, toTarget.normalize().scale(step));
            }
            ci.cancel();
            return;
        }

        // 裂地重拳前跃：只在起跳的头几 tick 接管，把人抬离地面后交还原版物理。
        // 若整段接管，空中就无法左右微调；若不接管，地面的方块摩擦会把水平速度乘掉 0.6。
        SlamLeapRegistry.Leap leap = SlamLeapRegistry.get(player.getUUID());
        if (leap != null) {
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0f;
            player.move(MoverType.SELF, SlamLeapRegistry.leapVelocity(leap));
            SlamLeapRegistry.tick(player.getUUID());
            // 交还物理时把当前速度写回，让原版自然接续抛物线
            if (!SlamLeapRegistry.isLeaping(player.getUUID())) {
                player.setDeltaMovement(SlamLeapRegistry.leapVelocity(leap));
                player.hurtMarked = true;
            }
            ci.cancel();
            return;
        }

        // 上勾拳上升：垂直位移，同样接管原版逻辑
        UppercutAscentRegistry.Ascent ascent = UppercutAscentRegistry.get(player.getUUID());
        if (ascent != null) {
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0f;
            player.move(MoverType.SELF, UppercutAscentRegistry.ascentVelocity(ascent));
            UppercutAscentRegistry.tick(player.getUUID());
            ci.cancel();
            return;
        }

        RocketPunchDashRegistry.Dash dash = RocketPunchDashRegistry.get(player.getUUID());
        if (dash == null) return;

        // 客户端预测：前方一旦有可命中的生物就立刻停下
        if (pigeonplus$hasTargetAhead(player)) {
            RocketPunchDashRegistry.stop(player.getUUID());
            player.setDeltaMovement(Vec3.ZERO);
            // 预测命中也要播命中动作：等到服务端回包再播会晚 1~2 tick，
            // 而冲刺速度高达 2.25 格/tick，那时玩家已经停下、动作却姗姗来迟。
            RocketPunchAnimState.triggerHit();
            ci.cancel();
            return;
        }

        // 清空速度，避免摩擦/重力残留影响下一 tick
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0f;

        // 翻越判定：前方只被方块挡住一小部分（擦到边角）时，转入斜向上冲刺，
        // 并且**保持这个上升势头直到冲刺结束**——不是越过障碍就停止上升，
        // 否则只有刚起跳的一小段是斜的，后面又贴回地面，手感很断。
        // 遮挡比例越高说明越是正面撞墙，就老老实实停下。
        // 探测距离取本 tick 的实际位移，这样能在撞上之前就发现障碍。
        if (!dash.climbing()) {
            double blocked = ObstructionUtil.obstructionRatio(player, dash.direction(), dash.speed());
            if (blocked > 0.0 && blocked < CLIMB_MAX_OBSTRUCTION) {
                // 遮挡越少、抬得越高：轻微蹭到就大角度翻越，挡得多就小角度勉强蹭上去
                double t = 1.0 - blocked / CLIMB_MAX_OBSTRUCTION;
                double climbSpeed = CLIMB_MIN_SPEED + (CLIMB_MAX_SPEED - CLIMB_MIN_SPEED) * t;
                RocketPunchDashRegistry.climb(player.getUUID(), climbSpeed);
                dash = RocketPunchDashRegistry.get(player.getUUID());
                if (dash == null) {
                    ci.cancel();
                    return;
                }
            }
        }

        // 恒定位移：水平按 direction 走，垂直按 climbSpeed（未翻越时为 0）。
        // climbSpeed 一经设定就保持到冲刺结束，因此上升势头会一直延续。
        Vec3 motion = dash.direction().scale(dash.speed());
        if (dash.climbing()) {
            motion = motion.add(0.0, dash.climbSpeed(), 0.0);
        }
        player.move(MoverType.SELF, motion);
        RocketPunchDashRegistry.tick(player.getUUID());

        // 撞墙：水平位移被方块彻底挡住 → 立刻结束冲刺，与打到生物的处理保持一致
        // （停位移、清权威状态、开始计冷却），而不是顶着墙把剩余 tick 磨完。
        //
        // 这里读 move() 之后的 horizontalCollision，而不是把前面的射线结果再判一次：
        // 它是原版权威的「本 tick 水平位移确实被挡住」标志，还能捕获射线采样可能漏掉的
        // 细薄障碍（铁栏杆、栅栏），比按比例估算可靠。
        // climbing 期间不停：那是正在沿墙爬升翻越，本就该贴着墙继续走。
        if (!dash.climbing() && player.horizontalCollision) {
            RocketPunchDashRegistry.stop(player.getUUID());
            player.setDeltaMovement(Vec3.ZERO);
            // 撞墙也算「打到了」：触发同一套命中动作，让手感一致
            RocketPunchAnimState.triggerHit();
            // 让服务端收尾：移除权威冲刺状态并开始计冷却。
            // 与跳跃取消复用同一个包——两者对服务端而言都是「冲刺在客户端提前结束了」。
            PacketDistributor.sendToServer(new RocketPunchCancelPacket());
        }

        ci.cancel();
    }

    /**
     * 判定前方是否有可命中的生物（仅客户端预测用，不影响服务端结算）。
     */
    @Unique
    private boolean pigeonplus$hasTargetAhead(Player player) {
        AABB box = player.getBoundingBox().inflate(PREDICT_HIT_RADIUS);
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (candidate == player || !candidate.isAlive() || candidate.isSpectator()) continue;
            if (candidate instanceof Player other && !player.canHarmPlayer(other)) continue;
            if (player.isPassengerOfSameVehicle(candidate)) continue;
            return true;
        }
        return false;
    }
}
