package dev.anvilcraft.pigeonplus.util;

import net.minecraft.world.entity.player.Player;

/**
 * 铁拳技能的互斥门禁：任一技能处于「占用阶段」时，其他技能不得起手。
 *
 * <p>为什么需要一个统一入口：三个技能原本各自在入口处零散地写了两三条 if，
 * 覆盖不全（例如蓄力时能放上勾拳、冲刺时能放裂地），而且两侧各写一份容易漏。
 * 收敛到这里之后，新增技能只需改这一处。
 *
 * <p>什么算「占用阶段」：
 * <ul>
 *   <li><strong>蓄力中</strong>（右键长按铁拳铁砧锤）；</li>
 *   <li><strong>冲刺中</strong>（火箭重拳位移）；</li>
 *   <li><strong>上升中</strong>（上勾拳位移）；</li>
 *   <li><strong>裂地位移中</strong>（前跃或指向性飞行）。</li>
 * </ul>
 *
 * <p>什么<strong>不算</strong>：
 * <ul>
 *   <li><strong>上勾拳的滞空</strong>——那只是残留的低重力状态，玩家并未在位移。
 *       刻意排除它，是为了保住「上挑 → 指向性裂地飞扑」这套连招。</li>
 * </ul>
 *
 * <p>注意：自身技能的起手瞬间不会把自己判为占用（例如蓄力开始时
 * {@code isUsingItem()} 尚为 false），因此不存在自我死锁。
 */
public final class SkillGate {
    private SkillGate() {
    }

    /**
     * 玩家当前是否正在使用某个技能（占用阶段）。为 true 时其他技能不得起手。
     *
     * <p>这个判断在客户端与服务端都可调用：各技能的状态查询内部已按侧取数据源。
     * 唯一的例外是裂地重拳的空中状态——服务端查 {@code SlamManager} 的记录，
     * 客户端需自行补上 {@code SeismicSlamClientState.isAirborne()}（见各客户端调用点）。
     */
    public static boolean isBusy(Player player) {
        return isCharging(player)
            || RocketPunchManager.isDashing(player)
            || UppercutManager.isAscending(player)
            || SlamManager.isSlamming(player);
    }

    /**
     * 是否正在蓄力（手持带铁拳附魔的铁砧锤长按右键）。
     */
    public static boolean isCharging(Player player) {
        return player.isUsingItem() && DoomfistEnchantmentUtil.hasDoomfist(player.getUseItem());
    }
}
