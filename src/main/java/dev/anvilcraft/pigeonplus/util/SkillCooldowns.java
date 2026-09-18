package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.pigeonplus.network.SkillCooldownPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 铁拳技能的统一冷却系统。
 *
 * <p>为什么不再用原版 {@code ItemCooldowns}：那是<strong>按物品</strong>计冷却的，
 * 而火箭重拳与上勾拳同属一把铁砧锤，共用一份数据会让两个技能互相锁死
 * （打完重拳 4 秒内放不出上勾拳）。这里的键是<strong>技能</strong>，彼此独立。
 *
 * <p>权威在服务端：{@link #startServer} 记录截止时刻并下发
 * {@link SkillCooldownPacket}；客户端只维护一份用于 HUD 的倒数镜像，
 * 不参与放行判断（放行一律由服务端决定，客户端判断仅用于提前拒绝、省掉多余发包）。
 *
 * <p>服务端与客户端的数据分开存放在两个字段里：单人生存中客户端与集成服务端同 JVM，
 * 若共用一个 Map，两端的写入会互相覆盖。
 */
public final class SkillCooldowns {
    /**
     * 可冷却的技能。冷却时长集中在此，新增技能只需加一项。
     */
    public enum Skill {
        /** 火箭重拳：冷却 4 秒，从冲刺结束开始计。 */
        ROCKET_PUNCH(80, 0xFF35A7FF),
        /** 上勾拳：冷却 6 秒，从释放瞬间开始计。 */
        UPPERCUT(120, 0xFFFFAA00);

        private final int cooldownTicks;
        private final int barColor;

        Skill(int cooldownTicks, int barColor) {
            this.cooldownTicks = cooldownTicks;
            this.barColor = barColor;
        }

        public int cooldownTicks() {
            return this.cooldownTicks;
        }

        /** HUD 进度条颜色。 */
        public int barColor() {
            return this.barColor;
        }

        /** 语言文件键，形如 {@code skill.anvilcraft_pigeon_plus.rocket_punch}。 */
        public String translationKey() {
            return "skill.anvilcraft_pigeon_plus." + this.name().toLowerCase(java.util.Locale.ROOT);
        }

        /** 按下标取技能，越界返回 null（网络包解码用，避免恶意下标抛异常）。 */
        public static Skill byOrdinal(int ordinal) {
            Skill[] values = values();
            if (ordinal < 0 || ordinal >= values.length) return null;
            return values[ordinal];
        }
    }

    /** 服务端权威截止时刻（绝对 gameTime）。 */
    private static final Map<UUID, Map<Skill, Long>> SERVER_DEADLINES = new HashMap<>();
    /** 客户端 HUD 用的剩余 tick 镜像。 */
    private static final Map<Skill, Integer> CLIENT_REMAINING = new EnumMap<>(Skill.class);

    private SkillCooldowns() {
    }

    // ---------------------------------------------------------------- 服务端

    /**
     * 开始冷却并同步给客户端。
     */
    public static void startServer(ServerPlayer player, Skill skill) {
        startServer(player.serverLevel(), player.getUUID(), skill);
    }

    /**
     * 开始冷却并同步给客户端（按 UUID，便于在没有 ServerPlayer 实例时调用）。
     */
    public static void startServer(ServerLevel level, UUID playerId, Skill skill) {
        long deadline = level.getGameTime() + skill.cooldownTicks();
        SERVER_DEADLINES
            .computeIfAbsent(playerId, key -> new EnumMap<>(Skill.class))
            .put(skill, deadline);

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        if (player != null) {
            PacketDistributor.sendToPlayer(player, new SkillCooldownPacket(skill.ordinal(), skill.cooldownTicks()));
        }
    }

    /**
     * 服务端判断冷却是否未走完。这是唯一的放行依据。
     */
    public static boolean isOnCooldown(ServerLevel level, UUID playerId, Skill skill) {
        Map<Skill, Long> skills = SERVER_DEADLINES.get(playerId);
        if (skills == null) return false;
        Long deadline = skills.get(skill);
        if (deadline == null) return false;
        if (level.getGameTime() >= deadline) {
            skills.remove(skill);
            return false;
        }
        return true;
    }

    /**
     * 按玩家所在侧自动选择数据源的冷却查询。
     *
     * <p>用于客户端与服务端共用的代码路径（例如物品 {@code use} 的门禁判断）：
     * 服务端读权威截止时刻，客户端读本地镜像。两侧的结论在正常网络下一致。
     */
    public static boolean isOnCooldown(Player player, Skill skill) {
        if (player.level() instanceof ServerLevel serverLevel) {
            return isOnCooldown(serverLevel, player.getUUID(), skill);
        }
        return isOnCooldownClient(skill);
    }

    /**
     * 清理过期与离线玩家的记录，避免静态表无限增长。
     */
    public static void prune(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, Map<Skill, Long>>> players = SERVER_DEADLINES.entrySet().iterator();
        while (players.hasNext()) {
            Map.Entry<UUID, Map<Skill, Long>> entry = players.next();
            entry.getValue().entrySet().removeIf(skill -> now >= skill.getValue());
            if (entry.getValue().isEmpty()) {
                players.remove();
            }
        }
    }

    /**
     * 玩家登出时清理其冷却记录。
     */
    public static void clear(UUID playerId) {
        SERVER_DEADLINES.remove(playerId);
    }

    // ---------------------------------------------------------------- 客户端

    /**
     * 接收服务端下发的冷却，写入本地镜像。
     */
    public static void applySync(Skill skill, int ticks) {
        CLIENT_REMAINING.put(skill, ticks);
    }

    /**
     * 客户端每 tick 递减所有技能的剩余时间。
     */
    public static void clientTick() {
        CLIENT_REMAINING.entrySet().removeIf(entry -> entry.getValue() <= 1);
        CLIENT_REMAINING.replaceAll((skill, remaining) -> remaining - 1);
    }

    /**
     * 客户端判断是否仍在冷却（仅用于提前拒绝，不参与权威放行）。
     */
    public static boolean isOnCooldownClient(Skill skill) {
        return CLIENT_REMAINING.getOrDefault(skill, 0) > 0;
    }

    /**
     * 剩余 tick 数（已按 {@code partialTick} 插值）。
     */
    public static float remainingTicks(Skill skill, float partialTick) {
        int remaining = CLIENT_REMAINING.getOrDefault(skill, 0);
        return Math.max(0.0f, remaining - partialTick);
    }

    /**
     * 剩余比例（1.0 = 刚开始，0.0 = 已结束）。HUD 画进度条用。
     */
    public static float progress(Skill skill, float partialTick) {
        int total = skill.cooldownTicks();
        if (total <= 0) return 0.0f;
        return Math.min(1.0f, remainingTicks(skill, partialTick) / total);
    }

    /**
     * 剩余秒数，保留一位小数展示用。
     */
    public static float remainingSeconds(Skill skill, float partialTick) {
        return remainingTicks(skill, partialTick) / 20.0f;
    }

    /**
     * 清空客户端镜像（断线重连时用，避免残留旧冷却）。
     */
    public static void clearClient() {
        CLIENT_REMAINING.clear();
    }
}
