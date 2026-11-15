package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.pigeonplus.init.AddonDamageTypes;
import dev.anvilcraft.pigeonplus.init.AddonMobEffects;
import dev.anvilcraft.pigeonplus.init.AddonSounds;
import dev.anvilcraft.pigeonplus.network.RocketPunchChargeSoundPacket;
import dev.anvilcraft.pigeonplus.network.RocketPunchDashPacket;
import dev.anvilcraft.pigeonplus.network.RocketPunchStopPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 火箭重拳（右键）的实现。
 *
 * <p>流程：长按右键蓄力 → 松开时沿视线方向高速冲出 → 撞到第一个敌人即停下并造成伤害与击退 →
 * 被击飞的敌人若在击退途中撞上方块，追加一次“撞墙”伤害与眩晕。
 *
 * <p>数值全部来自策划表：满蓄力 1.0 秒、蓄力后维持 0.6 秒、冷却 4 秒，
 * 命中伤害 25→75、撞墙伤害 10→40，自身位移 6.4→20 米。
 */
public final class RocketPunchManager {
    /** 满蓄力所需 tick（1.0 秒）。 */
    public static final int FULL_CHARGE_TICKS = 20;
    /** 满蓄力后的维持时长（0.6 秒）。 */
    public static final int CHARGE_HOLD_TICKS = 12;
    /** 一次完整蓄力的总时长：满蓄力 + 维持。 */
    public static final int TOTAL_CHARGE_TICKS = FULL_CHARGE_TICKS + CHARGE_HOLD_TICKS;
    /**
     * 冷却（4 秒），从冲刺结束开始计。
     *
     * <p>数值的唯一定义在 {@link SkillCooldowns.Skill#ROCKET_PUNCH}，这里只做转发，
     * 避免两处各写一份导致不一致。
     */
    public static final int COOLDOWN_TICKS = SkillCooldowns.Skill.ROCKET_PUNCH.cooldownTicks();

    /** 蓄力期间移动速度倍率（-50%）。 */
    private static final double CHARGE_SLOW_MULTIPLIER = 0.5;

    /** 冲刺速度（米/tick）。45 米/秒 ÷ 20 tick = 2.25。 */
    private static final double DASH_SPEED = 45.0 / 20.0;
    /** 被命中敌人的击飞速度（米/tick）。30 米/秒 ÷ 20 tick = 1.5。 */
    private static final double KNOCKBACK_SPEED = 30.0 / 20.0;
    /** 击飞时附加的上抛分量，让撞墙判定更容易出现。 */
    private static final double KNOCKBACK_LIFT = 0.35;

    /** 不蓄力的冲刺距离（米）与满蓄力的冲刺距离（米）。 */
    private static final double MIN_DASH_DISTANCE = 6.4;
    private static final double MAX_DASH_DISTANCE = 20.0;
    /** 命中判定半径（米）。 */
    private static final double HIT_RADIUS = 1.6;

    /** 不蓄力 / 满蓄力的命中伤害。 */
    private static final float MIN_HIT_DAMAGE = 5.0f;
    private static final float MAX_HIT_DAMAGE = 10.0f;
    /** 不蓄力 / 满蓄力的撞墙伤害。 */
    private static final float MIN_WALL_DAMAGE = 5.0f;
    private static final float MAX_WALL_DAMAGE = 15.0f;

    /** 撞墙眩晕时长（0.15 秒 = 3 tick）。 */
    private static final int WALL_STUN_TICKS = 3;

    /** 冲刺的活跃状态，key 为玩家 UUID。 */
    private static final Map<UUID, DashState> ACTIVE_DASHES = new HashMap<>();
    /** 正在被击飞、等待撞墙判定的敌人，key 为敌人 UUID。 */
    private static final Map<UUID, LaunchedState> LAUNCHED = new HashMap<>();

    private RocketPunchManager() {
    }

    /**
     * 冷却是否已经走完。
     *
     * <p>用统一的 {@link SkillCooldowns} 而不是原版 {@code ItemCooldowns}：
     * 后者按<strong>物品</strong>计冷却，而火箭重拳与上勾拳同属一把铁砧锤，
     * 共用一份数据会让两个技能互相锁死（打完重拳 4 秒内放不出上勾拳）。
     */
    public static boolean isOnCooldown(Player player, ItemStack stack) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return false;
        return SkillCooldowns.isOnCooldown(serverLevel, player.getUUID(), SkillCooldowns.Skill.ROCKET_PUNCH);
    }

    /**
     * 玩家当前是否处于火箭重拳冲刺中。
     *
     * <p>两侧的数据源不同，必须都查：
     * <ul>
     *   <li>服务端看 {@link #ACTIVE_DASHES}（冲刺的权威状态）；</li>
     *   <li>客户端看 {@link RocketPunchDashRegistry}——那是服务端下发冲刺包后才写入的，
     *       服务端自己并不写它（否则位移逻辑会在服务端也生效，与客户端权威的位置互相打架）。</li>
     * </ul>
     */
    public static boolean isDashing(Player player) {
        return ACTIVE_DASHES.containsKey(player.getUUID())
            || RocketPunchDashRegistry.isDashing(player.getUUID());
    }

    private static void startCooldown(ServerPlayer player) {
        // 统一冷却：服务端记截止时刻并自动同步给客户端（见 SkillCooldownPacket）
        SkillCooldowns.startServer(player, SkillCooldowns.Skill.ROCKET_PUNCH);
    }

    /**
     * 由 mixin 在铁砧锤松开右键时调用，执行一次火箭重拳。
     *
     * @param chargeTicks 已经蓄力的 tick 数
     */
    public static void performRocketPunch(ServerPlayer player, int chargeTicks) {
        // 无论这次释放是否真的打出去，蓄力都已经结束 → 先把蓄力音效掐掉。
        // 必须放在所有 return 之前：旁观、冲刺中、冷却中这几条路径都会提前返回，
        // 若把停止写在后面，这些情况下蓄力音会一直响到音频自然播完。
        PacketDistributor.sendToPlayer(player, new RocketPunchChargeSoundPacket(false));

        if (player.isSpectator()) return;
        ServerLevel level = player.serverLevel();
        // 已经在冲刺中：直接忽略。防止同一次蓄力被两条路径（finishUsingItem 与
        // LivingEntityUseItemEvent.Stop）各触发一次而打出两拳。
        if (ACTIVE_DASHES.containsKey(player.getUUID())) return;
        // 冷却未走完则不放行（客户端已由原版拦过一次，这里是服务端兜底）
        if (isOnCooldown(player, player.getMainHandItem())) return;

        double ratio = Math.min(1.0, (double) chargeTicks / FULL_CHARGE_TICKS);
        double distance = MIN_DASH_DISTANCE + (MAX_DASH_DISTANCE - MIN_DASH_DISTANCE) * ratio;
        float hitDamage = MIN_HIT_DAMAGE + (MAX_HIT_DAMAGE - MIN_HIT_DAMAGE) * (float) ratio;
        float wallDamage = MIN_WALL_DAMAGE + (MAX_WALL_DAMAGE - MIN_WALL_DAMAGE) * (float) ratio;

        Vec3 direction = player.getLookAngle();
        // 只保留水平方向，避免玩家低头时冲进地里
        Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            horizontal = new Vec3(0.0, 0.0, 1.0);
        }
        horizontal = horizontal.normalize();

        int totalTicks = Math.max(1, (int) Math.round(distance / DASH_SPEED));
        ACTIVE_DASHES.put(
            player.getUUID(),
            new DashState(level, totalTicks, hitDamage, wallDamage, horizontal)
        );
        spawnDashStartBurst(level, player, horizontal);
        // 位移与视角锁定都交给客户端：玩家位置由客户端权威决定，
        // 服务端位移会被下一个位置包覆盖，且 45m/s 会被 "moved too quickly" 拉回。
        PacketDistributor.sendToPlayer(
            player,
            new RocketPunchDashPacket(horizontal.x, horizontal.z, DASH_SPEED, totalTicks, player.getYRot())
        );
        level.playSound(
            null, player.getX(), player.getY(), player.getZ(),
            AddonSounds.ROCKET_PUNCH_CAST.get(), SoundSource.PLAYERS, 1.0f, 1.0f
        );
    }

    /**
     * 玩家是否正在蓄力（用于施加减速）。
     */
    public static double chargeSlowMultiplier(Player player) {
        return player.isUsingItem() ? CHARGE_SLOW_MULTIPLIER : 1.0;
    }

    /**
     * 中断冲刺（跳跃取消时由客户端通知）。
     *
     * <p>移除服务端状态并照常进入冷却——取消也算“用掉了”这次重拳，
     * 否则玩家可以靠跳跃取消规避冷却、无限连冲。
     */
    public static void abortDash(ServerPlayer player) {
        DashState dash = ACTIVE_DASHES.remove(player.getUUID());
        if (dash == null) return;
        startCooldown(player);
    }

    /**
     * 主 tick：推进所有冲刺与击飞中的撞墙判定。
     */
    public static void tick(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        Iterator<Map.Entry<UUID, DashState>> dashes = ACTIVE_DASHES.entrySet().iterator();
        while (dashes.hasNext()) {
            Map.Entry<UUID, DashState> entry = dashes.next();
            DashState dash = entry.getValue();
            if (dash.level != serverLevel) continue;

            Entity raw = serverLevel.getPlayerByUUID(entry.getKey());
            if (!(raw instanceof ServerPlayer player) || !player.isAlive()) {
                dashes.remove();
                continue;
            }

            if (dash.remainingTicks-- <= 0) {
                // 冲刺自然结束：此刻才开始计冷却（“冷却在冲刺结束后计算”）
                spawnDashEndPuff(serverLevel, player);
                dashes.remove();
                startCooldown(player);
                continue;
            }

            // 位移由客户端在 travel 阶段完成（见 LivingEntityTravelMixin）：
            // 服务端只在这里做倒计时、粒子与命中判定。
            // 服务端不再 setDeltaMovement/move，否则会与客户端上报的位置互相打架。
            player.fallDistance = 0.0f;

            spawnDashTrail(serverLevel, player, dash);

            // 命中检测
            AABB box = player.getBoundingBox().inflate(HIT_RADIUS);
            for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, box)) {
                if (target == player || !target.isAlive() || target.isSpectator()) continue;
                if (target instanceof Player other && !player.canHarmPlayer(other)) continue;
                if (player.isPassengerOfSameVehicle(target)) continue;
                launchTarget(serverLevel, player, target, dash);
                // 命中导致冲刺提前结束：同样从这一刻开始计冷却
                dashes.remove();
                startCooldown(player);
                // 立刻让客户端停止位移：位移归客户端管，仅移除服务端状态不会让他停下
                PacketDistributor.sendToPlayer(player, new RocketPunchStopPacket());
                break;
            }
        }

        tickLaunched(serverLevel);
    }

    /**
     * 冲刺起步的爆开：脚下的尘土、身后的蒸汽与一圈压缩波。
     *
     * <p>冲刺本身只有 9 tick 左右，只靠逐 tick 尾迹会显得「凭空出现」。
     * 起步这一炸既交代了动力来源，也给尾迹定下起点。
     */
    private static void spawnDashStartBurst(ServerLevel level, ServerPlayer player, Vec3 direction) {
        double x = player.getX() - direction.x * 0.6;
        double y = player.getY();
        double z = player.getZ() - direction.z * 0.6;
        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, x, y + 0.8, z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.CLOUD, x, y + 0.35, z, 18, 0.55, 0.35, 0.55, 0.12);
        level.sendParticles(ParticleTypes.POOF, x, y + 0.8, z, 10, 0.45, 0.45, 0.45, 0.09);
    }

    /**
     * 逐 tick 的尾迹：把云团铺在「上一 tick 位置 → 当前位置」这条线段上。
     *
     * <p>不能只落在脚下一个点：冲刺速度 45 m/s，单 tick 位移 2.25 格，
     * 按点发射会变成一串断开的圆斑，看起来像瞬移而不是高速运动。
     * 这里把散布范围沿冲刺方向拉长，让相邻 tick 的云带首尾相接成一条隧道。
     *
     * <p>云团中心压在身后——粒子一旦落到拳锋前面，第一人称下会糊住视野。
     */
    private static void spawnDashTrail(ServerLevel level, ServerPlayer player, DashState dash) {
        Vec3 current = player.position();
        Vec3 from = dash.lastPos == null ? current : dash.lastPos;
        dash.lastPos = current;

        // 冲刺允许在初始方向 ±45° 内微调视角，所以优先用本 tick 的真实位移方向
        Vec3 direction = dash.direction;
        Vec3 horizontalDelta = new Vec3(current.x - from.x, 0.0, current.z - from.z);
        if (horizontalDelta.lengthSqr() > 1.0E-4) {
            direction = horizontalDelta.normalize();
        }

        Vec3 center = from.lerp(current, 0.5).subtract(direction.scale(0.55));
        // 上限压在 1.6：服务端位置偶尔会隔几 tick 才追上客户端，不封顶会让某一 tick 的
        // 散布范围突然覆盖十几格，看起来像凭空炸了一团而不是尾迹
        double along = Math.min(from.distanceTo(current) * 0.5 + 0.25, 1.6);
        double spreadX = Math.abs(direction.x) * along + 0.22;
        double spreadZ = Math.abs(direction.z) * along + 0.22;

        // 1) 主体白汽：沿位移段拉长，构成连续云带
        level.sendParticles(
            ParticleTypes.CLOUD,
            center.x, center.y + 0.95, center.z,
            6, spreadX, 0.38, spreadZ, 0.02
        );
        // 2) 核心浓烟：范围小、密度高，给云带一根实心轴
        level.sendParticles(
            ParticleTypes.POOF,
            center.x, center.y + 0.7, center.z,
            3, 0.2, 0.3, 0.2, 0.03
        );
        // 3) 细白雾：更轻更散，负责把云带边缘化开
        level.sendParticles(
            ParticleTypes.WHITE_SMOKE,
            center.x, center.y + 0.85, center.z,
            2, spreadX * 0.8, 0.4, spreadZ * 0.8, 0.01
        );
        // 4) 每 3 tick 留一团压缩波，形成「一路炸过去」的节奏
        if (dash.tick % 3 == 0) {
            level.sendParticles(
                ParticleTypes.GUST,
                center.x, center.y + 0.9, center.z,
                1, 0.0, 0.0, 0.0, 0.0
            );
        }
        dash.tick++;
    }

    /**
     * 冲刺自然结束：补一小团余烟，避免尾迹在最密的时候被硬切断。
     */
    private static void spawnDashEndPuff(ServerLevel level, ServerPlayer player) {
        level.sendParticles(
            ParticleTypes.CLOUD,
            player.getX(), player.getY() + 0.9, player.getZ(),
            10, 0.35, 0.35, 0.35, 0.06
        );
    }

    /**
     * 命中敌人：造成伤害、按视线方向击飞，并登记等待撞墙判定。
     */
    private static void launchTarget(ServerLevel level, ServerPlayer player, LivingEntity target, DashState dash) {
        target.invulnerableTime = 0;
        target.hurt(AddonDamageTypes.rocketPunch(level, player), dash.hitDamage);

        // 击飞方向取玩家当前水平视线：冲刺中视角被限制在初始方向 ±45° 内，
        // 因此把敌人往“看得见的方向”打出去，和玩家预期一致。
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            horizontal = new Vec3(0.0, 0.0, 1.0);
        }
        Vec3 knockback = horizontal.normalize().scale(KNOCKBACK_SPEED).add(0.0, KNOCKBACK_LIFT, 0.0);
        target.setDeltaMovement(knockback);
        target.hurtMarked = true;
        target.fallDistance = 0.0f;

        LAUNCHED.put(target.getUUID(), new LaunchedState(level, player.getUUID(), dash.wallDamage));

        level.playSound(
            null, target.getX(), target.getY(), target.getZ(),
            AddonSounds.ROCKET_PUNCH_HIT.get(), SoundSource.PLAYERS, 1.0f, 1.2f
        );
        level.sendParticles(
            ParticleTypes.EXPLOSION,
            target.getX(), target.getY() + 0.5, target.getZ(),
            1, 0.0, 0.0, 0.0, 0.0
        );
        // 命中处的白色爆云：与尾迹同色系，读起来像「整条云带撞上了目标」
        level.sendParticles(
            ParticleTypes.CLOUD,
            target.getX(), target.getY() + 0.6, target.getZ(),
            14, 0.4, 0.4, 0.4, 0.14
        );
        level.sendParticles(
            ParticleTypes.GUST_EMITTER_SMALL,
            target.getX(), target.getY() + 0.5, target.getZ(),
            1, 0.0, 0.0, 0.0, 0.0
        );
    }

    /**
     * 推进被击飞敌人的撞墙判定。
     *
     * <p>只检测水平碰撞：{@code horizontalCollision} 在实体试图水平移动却被方块挡住时为 true，
     * 这正好对应“撞上墙壁”。
     */
    private static void tickLaunched(ServerLevel level) {
        Iterator<Map.Entry<UUID, LaunchedState>> iterator = LAUNCHED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, LaunchedState> entry = iterator.next();
            LaunchedState state = entry.getValue();
            if (state.level != level) continue;

            Entity raw = level.getEntity(entry.getKey());
            if (!(raw instanceof LivingEntity target) || !target.isAlive()) {
                iterator.remove();
                continue;
            }

            if (--state.remainingTicks <= 0) {
                iterator.remove();
                continue;
            }

            // 撞墙：水平方向被挡住
            if (target.horizontalCollision) {
                iterator.remove();
                // 清除无敌帧，保证撞墙伤害必定结算
                target.invulnerableTime = 0;
                target.hurt(AddonDamageTypes.rocketPunchWallSlam(level, level.getEntity(state.attackerId)), state.wallDamage);
                applyStun(target);
                level.playSound(
                    null, target.getX(), target.getY(), target.getZ(),
                    AddonSounds.ROCKET_PUNCH_WALL_SLAM.get(), SoundSource.PLAYERS, 1.0f, 0.5f
                );
                level.sendParticles(
                    ParticleTypes.CRIT,
                    target.getX(), target.getY() + 0.5, target.getZ(),
                    20, 0.3, 0.3, 0.3, 0.2
                );
            }
        }
    }

    private static void applyStun(LivingEntity target) {
        target.addEffect(new MobEffectInstance(AddonMobEffects.STUN, WALL_STUN_TICKS, 0, false, true), null);
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
        }
    }

    /**
     * 眩晕结束时恢复 AI。由事件监听器在每个 tick 调用。
     */
    public static void refreshStun(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        boolean stunned = entity.hasEffect(AddonMobEffects.STUN);
        if (mob.isNoAi() != stunned) {
            mob.setNoAi(stunned);
        }
    }

    /**
     * 服务端侧的一次冲刺状态。
     *
     * <p>只保留命中判定与倒计时所需的信息；位移参数已经下发给客户端
     * （见 {@code RocketPunchDashPacket}），服务端不再保存方向与距离。
     *
     * <p>不再记录「释放瞬间手持的物品」：统一冷却按<strong>技能</strong>计，
     * 与手持物无关，因此玩家中途换物品也不会影响冷却归属。
     */
    private static final class DashState {
        private final ServerLevel level;
        private final float hitDamage;
        private final float wallDamage;
        /** 起步时的水平冲刺方向；尾迹在拿不到真实位移时用它兜底。 */
        private final Vec3 direction;
        /** 上一 tick 的位置，用于把尾迹铺满整个位移段。 */
        private Vec3 lastPos;
        /** 已经推进的 tick 数，用于给压缩波打节奏。 */
        private int tick;
        private int remainingTicks;

        private DashState(
            ServerLevel level, int remainingTicks, float hitDamage, float wallDamage, Vec3 direction
        ) {
            this.level = level;
            this.remainingTicks = remainingTicks;
            this.hitDamage = hitDamage;
            this.wallDamage = wallDamage;
            this.direction = direction;
        }
    }

    private static final class LaunchedState {
        private final ServerLevel level;
        private final UUID attackerId;
        private final float wallDamage;
        private int remainingTicks = 40;

        private LaunchedState(ServerLevel level, UUID attackerId, float wallDamage) {
            this.level = level;
            this.attackerId = attackerId;
            this.wallDamage = wallDamage;
        }
    }
}
