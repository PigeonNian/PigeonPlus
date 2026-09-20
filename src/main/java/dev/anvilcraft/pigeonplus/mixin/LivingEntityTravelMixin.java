package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.client.SeismicSlamClientState;
import dev.anvilcraft.pigeonplus.util.RocketPunchDashRegistry;
import dev.anvilcraft.pigeonplus.util.SlamLeapRegistry;
import dev.anvilcraft.pigeonplus.util.UppercutAscentRegistry;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
            ci.cancel();
            return;
        }

        // 清空速度，避免摩擦/重力残留影响下一 tick
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0f;
        // 恒定位移：不经过摩擦与重力，严格匀速
        player.move(MoverType.SELF, dash.direction().scale(dash.speed()));
        RocketPunchDashRegistry.tick(player.getUUID());

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
