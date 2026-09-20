package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.pigeonplus.init.AddonDamageTypes;
import dev.anvilcraft.pigeonplus.init.AddonMobEffects;
import dev.anvilcraft.pigeonplus.init.AddonSounds;
import dev.anvilcraft.pigeonplus.network.SlamFlightPacket;
import dev.anvilcraft.pigeonplus.network.SlamLeapPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 裂地重拳（E）的实现：向前跃起，落地时砸击地面，
 * 对前方<strong>扇形</strong>区域造成伤害，滞空越久伤害越高。
 *
 * <p>与另外两个技能一致：伤害与冷却由服务端权威结算，玩家位移交给客户端
 * （位置客户端权威）。服务端记录起跳时刻，落地时<strong>自己算</strong>滞空时长，
 * 不采信客户端上报的数值。
 */
public final class SlamManager {
    /** 水平前跃速度（格/tick）。 */
    private static final double LEAP_SPEED = 0.45;
    /** 起跳垂直初速（格/tick）。 */
    private static final double LEAP_UP_SPEED = 0.22;
    /** 起跳时手动接管的 tick 数（之后交还原版物理，形成自然抛物线）。 */
    private static final int LEAP_CONTROL_TICKS = 4;

    /** 扇形判定半径（格）。 */
    private static final double SECTOR_RADIUS = 7.5;
    /** 扇形半角（度）。总张角为其两倍。 */
    private static final double SECTOR_HALF_ANGLE = 30.0;

    /** 最短滞空（tick）与达满伤害所需滞空（tick）。 */
    private static final int MIN_AIRTIME_TICKS = 4;
    private static final int MAX_AIRTIME_TICKS = 30;

    /** 伤害区间：滞空越久越高。 */
    private static final float MIN_DAMAGE = 20.0f;
    private static final float MAX_DAMAGE = 80.0f;

    /** 砸地命中的眩晕时长（0.6 秒 = 12 tick）。 */
    private static final int STUN_TICKS = 12;

    /** 落地判定半径（格），用于把范围内实体纳入候选。 */
    private static final double VERTICAL_RANGE = 3.0;

    /** 指向性裂地的瞄准射程（格）。 */
    private static final double TARGET_RANGE = 32.0;

    /**
     * 已起跳、等待落地的玩家：UUID → 起跳时的 gameTime。
     */
    private static final Map<UUID, Long> AIRBORNE = new HashMap<>();

    /**
     * 正在扩散的冲击波。
     *
     * <p>用列表而非 Map：同一时刻可能有多个玩家的波纹各自推进，
     * 它们的扩散进度彼此独立。
     */
    private static final List<Shockwave> WAVES = new ArrayList<>();

    /**
     * 客户端登记的「裂地位移中」标记。
     *
     * <p>为什么需要它：{@link #AIRBORNE} 只在服务端写入，而技能互斥判断要在
     * <strong>两侧</strong>都生效（客户端提前拒绝、服务端兜底）。客户端那边
     * 位移状态在 {@code SeismicSlamClientState} 里，但本类位于 common 侧，
     * 不能直接引用它（那个类依赖 {@code Minecraft}，专用服务端加载会崩）。
     * 因此由客户端在开始/结束时回调这里的登记方法，本类保持零客户端依赖。
     *
     * <p>单人生存下两侧同 JVM、UUID 相同，同一个键会被两端各写一次；
     * 但两端只在各自的开始/结束时刻增删，语义一致，不会互相干扰。
     */
    private static final Set<UUID> CLIENT_BUSY = ConcurrentHashMap.newKeySet();

    private SlamManager() {
    }

    /**
     * 客户端登记「已进入裂地位移」。由 {@code SeismicSlamClientState} 调用。
     */
    public static void markBusy(UUID playerId) {
        CLIENT_BUSY.add(playerId);
    }

    /**
     * 客户端登记「裂地位移已结束」。由 {@code SeismicSlamClientState} 调用。
     */
    public static void unmarkBusy(UUID playerId) {
        CLIENT_BUSY.remove(playerId);
    }

    /**
     * 释放裂地重拳：起跳并记录时刻。
     */
    public static void performSlam(ServerPlayer player) {
        if (player.isSpectator()) return;
        // 必须手持带铁拳附魔的铁砧锤
        if (!DoomfistEnchantmentUtil.isWieldingDoomfist(player)) return;

        ServerLevel level = player.serverLevel();
        // 冷却未走完不放行（唯一的权威判断）
        if (SkillCooldowns.isOnCooldown(level, player.getUUID(), SkillCooldowns.Skill.SEISMIC_SLAM)) return;
        // 已在空中：忽略重复触发
        if (AIRBORNE.containsKey(player.getUUID())) return;
        // 与其他技能互斥：蓄力 / 冲刺 / 上升 / 裂地位移期间不得再起手。
        // 注意 isSlamming 只在自身登记之前检查，不会自我死锁。
        // 上勾拳的「滞空」不算占用，所以「上挑 → 飞扑」连招仍然可用。
        if (SkillGate.isBusy(player)) return;

        SkillCooldowns.startServer(player, SkillCooldowns.Skill.SEISMIC_SLAM);
        AIRBORNE.put(player.getUUID(), level.getGameTime());

        // 方向取视线水平分量：朝哪看就往哪跃
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            horizontal = new Vec3(0.0, 0.0, 1.0);
        }
        horizontal = horizontal.normalize();

        PacketDistributor.sendToPlayer(
            player,
            SlamLeapPacket.of(horizontal, LEAP_SPEED, LEAP_UP_SPEED, LEAP_CONTROL_TICKS)
        );

        level.playSound(
            null, player.getX(), player.getY(), player.getZ(),
            AddonSounds.SEISMIC_SLAM_LEAP.get(), SoundSource.PLAYERS, 1.0f, 1.0f
        );
    }

    /**
     * 指向性裂地重拳：玩家悬空时，直接飞向准星指到的地面并砸下。
     *
     * <p>与普通裂地的区别是不做抛物线前跃，而是沿一条直线飞向目标点，
     * 到点即结算。落点由服务端射线求得，客户端只收到「飞向哪」这一个参数。
     *
     * @return true 表示已切换为指向性；false 表示当前条件不满足（调用方应回退为普通裂地）
     */
    public static boolean performTargetedSlam(ServerPlayer player) {
        if (player.isSpectator()) return false;
        if (!DoomfistEnchantmentUtil.isWieldingDoomfist(player)) return false;

        ServerLevel level = player.serverLevel();
        if (SkillCooldowns.isOnCooldown(level, player.getUUID(), SkillCooldowns.Skill.SEISMIC_SLAM)) return false;
        if (AIRBORNE.containsKey(player.getUUID())) return false;
        // 与其他技能互斥，但**只排除「上升」这一位移阶段**：
        // 上勾拳的滞空是本技能的设计前提（「上挑 → 飞扑」连招），
        // 若用 SkillGate.isBusy 会把滞空也算进去，连招就废了。
        if (RocketPunchManager.isDashing(player)) return false;
        if (UppercutManager.isAscending(player)) return false;
        if (SkillGate.isCharging(player)) return false;
        // 只有悬空才能用指向性
        if (!AirborneUtil.isAirborne(player)) return false;

        Vec3 landing = findLandingSpot(level, player);
        if (landing == null) return false;

        SkillCooldowns.startServer(player, SkillCooldowns.Skill.SEISMIC_SLAM);
        AIRBORNE.put(player.getUUID(), level.getGameTime());

        PacketDistributor.sendToPlayer(player, SlamFlightPacket.of(landing));
        level.playSound(
            null, player.getX(), player.getY(), player.getZ(),
            AddonSounds.SEISMIC_SLAM_LEAP.get(), SoundSource.PLAYERS, 1.0f, 1.2f
        );
        return true;
    }

    /**
     * 求准星所指的地面落点。
     *
     * <p>沿视线找第一个可站立的方块，取其上方一格中心作为落点。
     * 找不到（看向天空/超出距离）或超出射程时返回 null。
     */
    private static Vec3 findLandingSpot(ServerLevel level, ServerPlayer player) {
        BlockHitResult hit = (BlockHitResult) player.pick(TARGET_RANGE, 0.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) return null;

        BlockPos hitPos = hit.getBlockPos();
        // 瞄准的是方块顶面 → 站在它上面；否则站在命中面的外侧一格
        BlockPos landingPos = hit.getDirection() == Direction.UP
            ? hitPos.above()
            : hitPos.relative(hit.getDirection());

        // 落点必须是可站立的地面，且上方两格不能有阻挡
        if (!Block.canSupportRigidBlock(level, landingPos.below())) return null;
        if (!level.getBlockState(landingPos).getCollisionShape(level, landingPos).isEmpty()) return null;
        if (!level.getBlockState(landingPos.above()).getCollisionShape(level, landingPos.above()).isEmpty()) return null;

        return new Vec3(
            landingPos.getX() + 0.5,
            landingPos.getY(),
            landingPos.getZ() + 0.5
        );
    }

    /**
     * 落地结算：按滞空时长计算伤害，并对前方扇形区域生效。
     */
    public static void impact(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Long startTime = AIRBORNE.remove(player.getUUID());
        if (startTime == null) return;

        int airtime = (int) (level.getGameTime() - startTime);
        float damage = damageForAirtime(airtime);

        applySectorDamage(level, player, damage);
        spawnSectorEffect(level, player);

        level.playSound(
            null, player.getX(), player.getY(), player.getZ(),
            AddonSounds.SEISMIC_SLAM_IMPACT.get(), SoundSource.PLAYERS, 1.2f, 1.0f
        );
    }

    /**
     * 滞空时长 → 伤害。低于最短时长按最短算，超过上限按满伤算。
     *
     * <p>公开给 HUD 使用：准星下方的伤害指示器必须和实际结算<strong>同源</strong>，
     * 各写一份公式迟早会漂移，出现「显示 60 实际打 45」这种对不上的情况。
     */
    public static float damageForAirtime(int airtimeTicks) {
        double ratio = (double) (airtimeTicks - MIN_AIRTIME_TICKS) / (MAX_AIRTIME_TICKS - MIN_AIRTIME_TICKS);
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        return MIN_DAMAGE + (MAX_DAMAGE - MIN_DAMAGE) * (float) ratio;
    }

    /**
     * 扇形砸地特效：砸击瞬间先炸开一片扇形尘埃，随后一道冲击波沿扇形向外扩散。
     *
     * <p>扩散动画需要逐 tick 推进，因此登记到 {@link #WAVES}，由 {@link #tickWaves}
     * 每 tick 画一环。这样波纹能「扫过」实际伤害范围，玩家一眼就能看出打到了哪。
     */
    private static void spawnSectorEffect(ServerLevel level, ServerPlayer player) {
        Vec3 origin = player.position();
        Vec3 forward = horizontalLook(player);

        // 冲击点：脚下的地面层，避免粒子悬在半空
        double groundY = player.getY() + 0.1;

        // 1) 砸击瞬间的扇形尘埃：沿扇形撒一圈，越远越稀
        int rubbleCount = 60;
        for (int i = 0; i < rubbleCount; i++) {
            // 角度在 ±半角 内均匀取，半径带随机以免看起来像同心圆
            double angle = Math.toRadians((level.random.nextDouble() * 2.0 - 1.0) * SECTOR_HALF_ANGLE);
            double radius = SECTOR_RADIUS * Math.sqrt(level.random.nextDouble());
            Vec3 offset = rotateY(forward, angle).scale(radius);

            level.sendParticles(
                ParticleTypes.CLOUD,
                origin.x + offset.x, groundY, origin.z + offset.z,
                1, 0.1, 0.05, 0.1, 0.02
            );
        }

        // 2) 贴地的尘土环：用 DUST 画一圈偏土黄的颜色，强化“裂地”观感
        DustParticleOptions dust = new DustParticleOptions(
            new Vector3f(0.55f, 0.45f, 0.32f), 1.6f
        );
        for (int i = 0; i < 40; i++) {
            double angle = Math.toRadians((level.random.nextDouble() * 2.0 - 1.0) * SECTOR_HALF_ANGLE);
            Vec3 offset = rotateY(forward, angle).scale(SECTOR_RADIUS * level.random.nextDouble());
            level.sendParticles(
                dust,
                origin.x + offset.x, groundY, origin.z + offset.z,
                1, 0.05, 0.02, 0.05, 0.0
            );
        }

        // 3) 中心爆开一下
        level.sendParticles(
            ParticleTypes.EXPLOSION_EMITTER, origin.x, groundY, origin.z, 1, 0.0, 0.0, 0.0, 0.0
        );

        // 4) 登记向外扩散的冲击波
        WAVES.add(new Shockwave(level, origin, forward, groundY));
    }

    /**
     * 把注视方向投影成水平单位向量，退化时回退到 +Z。
     */
    private static Vec3 horizontalLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0, look.z);
        if (forward.lengthSqr() < 1.0E-6) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }
        return forward.normalize();
    }

    /**
     * 绕 Y 轴旋转一个水平向量。
     *
     * <p>用旋转而不是「先算角度再取 cos/sin」，是为了避开 Minecraft 的 yaw 符号约定
     * （yaw 增大是顺时针），手写容易把扇形画到镜子对面去。
     */
    private static Vec3 rotateY(Vec3 vec, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(
            vec.x * cos + vec.z * sin,
            0.0,
            -vec.x * sin + vec.z * cos
        );
    }

    /**
     * 每 tick 推进所有冲击波。由事件监听器在 LevelTick 上调用。
     */
    public static void tickWaves(ServerLevel level) {
        if (WAVES.isEmpty()) return;
        Iterator<Shockwave> iterator = WAVES.iterator();
        while (iterator.hasNext()) {
            Shockwave wave = iterator.next();
            if (wave.level != level) continue;

            if (wave.age >= Shockwave.LIFETIME_TICKS) {
                iterator.remove();
                continue;
            }

            // 半径由 0 线性扩到最大，形成「扫出去」的观感
            double progress = (double) wave.age / Shockwave.LIFETIME_TICKS;
            double radius = SECTOR_RADIUS * progress;

            // 沿扇形弧线画这一环
            int points = 28;
            for (int i = 0; i <= points; i++) {
                double angle = Math.toRadians(
                    -SECTOR_HALF_ANGLE + (2.0 * SECTOR_HALF_ANGLE) * i / points
                );
                Vec3 offset = rotateY(wave.forward, angle).scale(radius);
                level.sendParticles(
                    ParticleTypes.CLOUD,
                    wave.origin.x + offset.x, wave.groundY, wave.origin.z + offset.z,
                    1, 0.0, 0.02, 0.0, 0.0
                );
                // 每隔几个点补一点碎屑，让弧线更实
                if (i % 4 == 0) {
                    level.sendParticles(
                        ParticleTypes.CRIT,
                        wave.origin.x + offset.x, wave.groundY + 0.15, wave.origin.z + offset.z,
                        1, 0.0, 0.05, 0.0, 0.0
                    );
                }
            }
            wave.age++;
        }
    }

    /**
     * 一道向外扩散的冲击波。
     */
    private static final class Shockwave {
        /** 扩散动画持续 tick 数（0.5 秒）。 */
        private static final int LIFETIME_TICKS = 10;

        private final ServerLevel level;
        private final Vec3 origin;
        private final Vec3 forward;
        private final double groundY;
        private int age;

        private Shockwave(ServerLevel level, Vec3 origin, Vec3 forward, double groundY) {
            this.level = level;
            this.origin = origin;
            this.forward = forward;
            this.groundY = groundY;
        }
    }

    /**
     * 对玩家前方扇形区域内的敌人造成伤害。
     *
     * <p>判定分两步：先用一个球形/盒形范围取出候选，再按水平方向与视线的夹角
     * 过滤出「扇形」内的目标。只按距离会打到自己背后，不符合技能语义。
     */
    private static void applySectorDamage(ServerLevel level, ServerPlayer player, float damage) {
        Vec3 origin = player.position();
        AABB box = new AABB(origin, origin)
            .inflate(SECTOR_RADIUS, VERTICAL_RANGE, SECTOR_RADIUS);

        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0, look.z);
        if (forward.lengthSqr() < 1.0E-6) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }
        forward = forward.normalize();
        double cosLimit = Math.cos(Math.toRadians(SECTOR_HALF_ANGLE));

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (target == player || !target.isAlive() || target.isSpectator()) continue;
            if (target instanceof Player other && !player.canHarmPlayer(other)) continue;
            if (player.isPassengerOfSameVehicle(target)) continue;

            Vec3 toTarget = target.position().subtract(origin);
            Vec3 horizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
            // 垂直重叠但水平几乎重合（脚下/头顶）也算命中
            if (horizontal.lengthSqr() > 1.0E-6) {
                double distance = horizontal.length();
                if (distance > SECTOR_RADIUS) continue;
                // 夹角过滤：点积与阈值比较，等价于角度比较但无需开方/反三角
                if (horizontal.normalize().dot(forward) < cosLimit) continue;
            }

            target.invulnerableTime = 0;
            target.hurt(AddonDamageTypes.seismicSlam(level, player), damage);

            // 砸地以击退为主：从玩家指向目标的方向给一个水平冲量，并略微上抬
            Vec3 push = new Vec3(toTarget.x, 0.0, toTarget.z);
            if (push.lengthSqr() < 1.0E-6) {
                push = forward;
            }
            push = push.normalize().scale(0.9).add(0.0, 0.35, 0.0);
            target.setDeltaMovement(push);
            target.hurtMarked = true;
            target.fallDistance = 0.0f;

            target.addEffect(new MobEffectInstance(AddonMobEffects.STUN, STUN_TICKS, 0, false, true), null);
            if (target instanceof Mob mob) {
                mob.setNoAi(true);
            }
        }
    }

    /**
     * 玩家是否正在裂地重拳的位移过程中（前跃或指向性飞行）。
     *
     * <p>两侧数据源都查：服务端看 {@link #AIRBORNE}（权威），
     * 客户端看由 {@code SeismicSlamClientState} 登记的 {@link #CLIENT_BUSY}。
     */
    public static boolean isSlamming(Player player) {
        return AIRBORNE.containsKey(player.getUUID())
            || CLIENT_BUSY.contains(player.getUUID());
    }

    /**
     * 玩家在落地前离线/死亡时清理记录。
     */
    public static void clear(Player player) {
        AIRBORNE.remove(player.getUUID());
    }

    public static int cooldownTicks() {
        return SkillCooldowns.Skill.SEISMIC_SLAM.cooldownTicks();
    }
}
