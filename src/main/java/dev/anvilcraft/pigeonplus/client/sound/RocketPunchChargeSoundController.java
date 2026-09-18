package dev.anvilcraft.pigeonplus.client.sound;

import dev.anvilcraft.pigeonplus.util.DoomfistEnchantmentUtil;
import net.minecraft.client.Minecraft;

/**
 * 客户端持有的蓄力音效实例控制器。
 *
 * <p>只保留一个实例：重复开始会先掐断旧的，避免快速点按右键时叠出多条音轨。
 */
public final class RocketPunchChargeSoundController {
    /** 当前正在播放的实例，null 表示没有。 */
    private static RocketPunchChargeSoundInstance current;

    private RocketPunchChargeSoundController() {
    }

    /**
     * 开始播放。已在播放时先停掉旧的，保证同一时刻只有一条。
     */
    public static void start() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        // 先掐断旧的，避免叠加
        stop();
        RocketPunchChargeSoundInstance instance =
            new RocketPunchChargeSoundInstance(minecraft.player.getId());
        current = instance;
        minecraft.getSoundManager().play(instance);
    }

    /**
     * 立即停止。冲刺开始、松开右键、取消等所有「蓄力结束」的路径都会调用。
     */
    public static void stop() {
        if (current == null) return;
        current.stopNow();
        Minecraft.getInstance().getSoundManager().stop(current);
        current = null;
    }

    /**
     * 客户端每 tick 的兜底校准。
     *
     * <p>判据是「玩家是否还在蓄力」，而<strong>不是</strong>
     * {@code SoundManager#isActive}：后者查的是 {@code SoundEngine.soundDeleteTime}，
     * 那张表要等声音引擎下一次 tick 把排队中的声音提升上来才会写入。
     * 若在播放后的下一 tick 就去查，会得到 false，于是刚拿到的句柄被清空，
     * 冲刺时的 {@code stop()} 变成空操作——蓄力音会一直响下去。
     *
     * <p>用 {@code isUsingItem} 作判据则没有这个时序问题，而且它天然覆盖了
     * 「以任何方式中断蓄力」的情况，不依赖服务端把每个路径都发一遍停止包。
     */
    public static void clientTick() {
        if (current == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !current.belongsToCurrentPlayer()) {
            stop();
            return;
        }
        // 不再蓄力（松开、蓄满自动释放、中途被中断）→ 立刻掐断
        boolean stillCharging = minecraft.player.isUsingItem()
            && DoomfistEnchantmentUtil.hasDoomfist(minecraft.player.getUseItem());
        if (!stillCharging) {
            stop();
        }
    }
}
