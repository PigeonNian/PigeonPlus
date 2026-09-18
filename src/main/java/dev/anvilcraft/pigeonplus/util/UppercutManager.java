package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.pigeonplus.init.AddonDamageTypes;
import dev.anvilcraft.pigeonplus.init.AddonMobEffects;
import dev.anvilcraft.pigeonplus.network.UppercutAscentPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 上勾拳（Shift）的实现。
 *
 * <p>把面前的敌人向上击飞，同时玩家自身也向上跃起；上升期间玩家无法操作移动。
 *
 * <p>架构与火箭重拳一致：伤害与击飞由服务端权威结算，玩家自身的上升位移交给客户端执行
 * （玩家位置客户端权威，服务端位移会被位置包覆盖）。
 *
 * <p><strong>冷却与火箭重拳相互独立</strong>：两者都走统一的 {@link SkillCooldowns}，
 * 但按技能分别计时，互不影响。
 */
public final class UppercutManager {
    /** 命中伤害（占位值，待调）。 */
    private static final float DAMAGE = 50.0f;

    /** 敌人被上抛的速度（格/tick）。 */
    private static final double ENEMY_UP_SPEED = 1.2;
    /** 玩家自身起跳速度（格/tick）。略低于敌人，让敌人真的被“打飞”。 */
    private static final double PLAYER_UP_SPEED = 1.0;
    /** 上升阶段持续 tick 数。 */
    private static final int ASCENT_TICKS = 10;

    /** 前方判定距离（格）。 */
    private static final double RANGE = 3.0;
    /** 判定盒的横向/纵向扩张量。 */
    private static final double HIT_INFLATE = 1.2;

    /** 敌人被击飞后的眩晕时长（0.5 秒 = 10 tick）。 */
    private static final int STUN_TICKS = 10;

    private UppercutManager() {
    }

    /**
     * 释放一次上勾拳。
     */
    public static void performUppercut(ServerPlayer player) {
        if (player.isSpectator()) return;
        // 必须持有带铁拳附魔的铁砧锤：这是铁拳的技能，赤手或别的武器不该触发。
        // 服务端校验是必须的——客户端只负责读按键，不能信任。
        if (!DoomfistEnchantmentUtil.isWieldingDoomfist(player)) return;
        // 冲刺与上升互斥：冲刺途中不该叠一次上勾拳（两套位移会互相打架）
        if (RocketPunchManager.isDashing(player)) return;
        if (UppercutAscentRegistry.isAscending(player.getUUID())) return;

        ServerLevel level = player.serverLevel();
        // 冷却改用统一的 SkillCooldowns，与火箭重拳互不影响
        if (SkillCooldowns.isOnCooldown(level, player.getUUID(), SkillCooldowns.Skill.UPPERCUT)) return;

        SkillCooldowns.startServer(player, SkillCooldowns.Skill.UPPERCUT);

        // 面前的敌人：以视线水平方向为准，在玩家前方开一个判定盒
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0, look.z);
        if (forward.lengthSqr() < 1.0E-6) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }
        forward = forward.normalize();

        Vec3 center = player.position().add(forward.scale(RANGE / 2.0));
        AABB area = new AABB(center, center).inflate(RANGE / 2.0 + HIT_INFLATE, HIT_INFLATE, RANGE / 2.0 + HIT_INFLATE);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == player || !target.isAlive() || target.isSpectator()) continue;
            if (target instanceof Player other && !player.canHarmPlayer(other)) continue;
            if (player.isPassengerOfSameVehicle(target)) continue;
            // 只打身前：目标相对玩家的方向需与视线同侧
            Vec3 toTarget = target.position().subtract(player.position());
            Vec3 toTargetHorizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
            if (toTargetHorizontal.lengthSqr() > 1.0E-6
                && toTargetHorizontal.normalize().dot(forward) < 0.0) {
                continue;
            }
            launchUpward(level, player, target);
        }

        // 玩家自身跃起：位移交给客户端
        PacketDistributor.sendToPlayer(player, new UppercutAscentPacket(PLAYER_UP_SPEED, ASCENT_TICKS));

        level.playSound(
            null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0f, 1.4f
        );
        level.sendParticles(
            ParticleTypes.CLOUD,
            player.getX(), player.getY(), player.getZ(),
            12, 0.3, 0.1, 0.3, 0.05
        );
    }

    /**
     * 把目标向上击飞并造成伤害与短暂眩晕。
     */
    private static void launchUpward(ServerLevel level, ServerPlayer player, LivingEntity target) {
        target.invulnerableTime = 0;
        target.hurt(AddonDamageTypes.uppercut(level, player), DAMAGE);

        // 只给向上的速度：上勾拳的语义是“打上天”，不做水平击退
        Vec3 movement = target.getDeltaMovement();
        target.setDeltaMovement(movement.x, ENEMY_UP_SPEED, movement.z);
        target.hurtMarked = true;
        target.fallDistance = 0.0f;

        target.addEffect(
            new MobEffectInstance(AddonMobEffects.STUN, STUN_TICKS, 0, false, true), null
        );
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
        }

        level.sendParticles(
            ParticleTypes.CRIT,
            target.getX(), target.getY() + 0.5, target.getZ(),
            24, 0.3, 0.4, 0.3, 0.25
        );
    }

    /**
     * 上勾拳的冷却时长（tick）。数值统一定义在 {@link SkillCooldowns.Skill#UPPERCUT}。
     */
    public static int cooldownTicks() {
        return SkillCooldowns.Skill.UPPERCUT.cooldownTicks();
    }
}
