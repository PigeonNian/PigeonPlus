package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.init.AddonDamageTypes;
import dev.anvilcraft.pigeonplus.init.AddonMobEffects;
import dev.anvilcraft.pigeonplus.init.AddonSounds;
import dev.anvilcraft.pigeonplus.network.UppercutAscentPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

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
    private static final float DAMAGE = 5.0f;

    /** 敌人被上抛的速度（格/tick）。 */
    private static final double ENEMY_UP_SPEED = 1.2;
    /** 玩家自身起跳速度（格/tick）。略低于敌人，让敌人真的被“打飞”。 */
    private static final double PLAYER_UP_SPEED = 0.8;
    /** 上升阶段持续 tick 数。 */
    private static final int ASCENT_TICKS = 10;

    /** 上升结束后的滞空时长（1 秒 = 20 tick）。 */
    private static final int HOVER_TICKS = 5;

    /**
     * 滞空期间的重力倍率（-75% → 实际为重力的 25%）。
     *
     * <p>用 {@link Attributes#GRAVITY} 的瞬时修饰符实现，不需要 mixin：
     * {@code LivingEntity#getDefaultGravity} 读的正是这个属性，
     * 而 {@code travel} 每次都通过 {@code getGravity()} 取值。
     */
    private static final double HOVER_GRAVITY_REDUCTION = -0.25;

    /** 滞空重力修饰符的固定 id，便于精确移除。 */
    private static final ResourceLocation HOVER_GRAVITY_ID =
        AnvilCraftPigeonPlus.of("uppercut_hover_gravity");

    /** 前方判定距离（格）。 */
    private static final double RANGE = 3.0;
    /** 判定盒的横向/纵向扩张量。 */
    private static final double HIT_INFLATE = 1.2;

    /** 敌人被击飞后的眩晕时长（0.5 秒 = 10 tick）。 */
    private static final int STUN_TICKS = 10;

    /**
     * 正在滞空的玩家：UUID → 剩余 tick。
     *
     * <p>放在服务端：重力属性本身是 client-syncable 的（注册时 {@code setSyncable(true)}），
     * 服务端加修饰符会自动同步到客户端，客户端不需要也不能再加一次——
     * 两侧都加会变成 25% × 25% = 6.25%。
     */
    private static final Map<UUID, Integer> HOVERING = new HashMap<>();

    /**
     * 正在上升的玩家：UUID → 剩余 tick（<strong>服务端</strong>记录）。
     *
     * <p>为什么服务端要单独记一份：{@link UppercutAscentRegistry} 只由客户端写入
     * （服务端下发上升包后客户端才登记），专用服务端上它恒为空。
     * 若互斥判断只依赖那张表，服务端就拦不住「上升途中再放别的技能」，
     * 客户端一旦被绕过（或另一名玩家视角不同）就会叠加两套位移。
     */
    private static final Map<UUID, Integer> ASCENDING = new HashMap<>();

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
        // 技能互斥：蓄力 / 冲刺 / 上升 / 裂地位移期间不得再起手。
        // 自身不在占用阶段（调用发生在登记之前），所以不会自我死锁。
        // 滞空刻意不算占用，以保住「上挑 → 指向性裂地飞扑」的连招。
        if (SkillGate.isBusy(player)) return;

        ServerLevel level = player.serverLevel();
        // 冷却改用统一的 SkillCooldowns，与火箭重拳互不影响
        if (SkillCooldowns.isOnCooldown(level, player.getUUID(), SkillCooldowns.Skill.UPPERCUT)) return;

        SkillCooldowns.startServer(player, SkillCooldowns.Skill.UPPERCUT);
        // 登记服务端的上升状态，供技能互斥使用
        ASCENDING.put(player.getUUID(), ASCENT_TICKS);

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
        // 上升 + 滞空：降低重力，让玩家在空中停留一段时间
        startHover(player);

        level.playSound(
            null, player.getX(), player.getY(), player.getZ(),
            AddonSounds.UPPERCUT_CAST.get(), SoundSource.PLAYERS, 1.0f, 1.4f
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
     * 玩家是否处于上勾拳的上升阶段。
     *
     * <p>两侧数据源都查：服务端看 {@link #ASCENDING}（权威），
     * 客户端看 {@link UppercutAscentRegistry}（收到上升包后写入）。
     */
    public static boolean isAscending(Player player) {
        return ASCENDING.containsKey(player.getUUID())
            || UppercutAscentRegistry.isAscending(player.getUUID());
    }

    /**
     * 推进服务端的上升倒计时。由事件监听器在 LevelTick 上调用。
     *
     * <p>与 {@code tickHover} 同理：{@code LevelTickEvent} 会为每个维度各触发一次，
     * 不在本维度的玩家必须跳过，否则会被别的维度误判为「已结束」。
     */
    public static void tickAscent(ServerLevel level) {
        if (ASCENDING.isEmpty()) return;
        Iterator<Map.Entry<UUID, Integer>> iterator = ASCENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (player.serverLevel() != level) continue;
            if (entry.getValue() <= 1) {
                iterator.remove();
            } else {
                entry.setValue(entry.getValue() - 1);
            }
        }
    }

    /**
     * 上勾拳的冷却时长（tick）。数值统一定义在 {@link SkillCooldowns.Skill#UPPERCUT}。
     */
    public static int cooldownTicks() {
        return SkillCooldowns.Skill.UPPERCUT.cooldownTicks();
    }

    /**
     * 开始滞空：施加低重力修饰符，并登记倒计时。
     *
     * <p>倒计时长度为「上升 + 滞空」：上升阶段 {@code travel} 被完全接管
     * （见 {@code LivingEntityTravelMixin}），重力在那几 tick 里不起作用，
     * 所以把上升时间一起算进来，玩家实际感受到的低重力就是 {@link #HOVER_TICKS}。
     * 这样不需要客户端在上升结束时再回一个包，少一次往返。
     */
    private static void startHover(ServerPlayer player) {
        applyHoverGravity(player);
        HOVERING.put(player.getUUID(), ASCENT_TICKS + HOVER_TICKS);
    }

    /**
     * 每 tick 推进滞空倒计时，到期移除低重力。
     *
     * <p>{@code LevelTickEvent} 会为<strong>每个维度</strong>各触发一次，
     * 因此这里必须只处理属于本维度的玩家：若把「不在本维度」当成需要清理，
     * 那么玩家在别的维度的 tick 里就会被误判并立刻取消滞空。
     * 换维度的情况由「玩家当前所在维度的那次 tick」自然接续处理。
     */
    public static void tickHover(ServerLevel level) {
        if (HOVERING.isEmpty()) return;
        Iterator<Map.Entry<UUID, Integer>> iterator = HOVERING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            // 玩家离线：清掉记录（实体已不存在，无需也不必移除修饰符）
            if (player == null) {
                iterator.remove();
                continue;
            }
            // 不在本维度：跳过，交给该玩家所在维度的 tick 处理
            if (player.serverLevel() != level) continue;

            if (entry.getValue() <= 1) {
                removeHoverGravity(player);
                iterator.remove();
            } else {
                entry.setValue(entry.getValue() - 1);
            }
        }
    }

    /**
     * 玩家登出等场景下强制结束滞空。
     */
    public static void clearHover(Player player) {
        if (HOVERING.remove(player.getUUID()) != null) {
            removeHoverGravity(player);
        }
    }

    private static void applyHoverGravity(Player player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity == null) return;
        // 用 addOrUpdateTransientModifier 而非 addTransientModifier：
        // 后者在 id 已存在时会抛异常，重复触发上勾拳就会把服务端打崩
        gravity.addOrUpdateTransientModifier(new AttributeModifier(
            HOVER_GRAVITY_ID,
            HOVER_GRAVITY_REDUCTION,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }

    private static void removeHoverGravity(Player player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity != null) {
            gravity.removeModifier(HOVER_GRAVITY_ID);
        }
    }
}
