package dev.anvilcraft.pigeonplus.client;

import dev.anvilcraft.pigeonplus.util.DoomfistEnchantmentUtil;
import dev.anvilcraft.pigeonplus.util.SkillCooldowns;
import dev.anvilcraft.pigeonplus.util.SkillGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * 裂地重拳的 E 键拦截。
 *
 * <p>E 在原版是「打开物品栏」，这里把它改造成技能键：在
 * {@code ClientTickEvent.Pre} 阶段<strong>提前消费掉这次点击</strong>，
 * 后面 {@code Minecraft#handleKeybinds} 里的 {@code keyInventory.consumeClick()}
 * 就会拿到 false，物品栏自然不会打开。
 *
 * <p>时序依据：{@code Minecraft#tick()} 中
 * {@code ClientHooks.fireClientTickPre()} 在前、{@code handleKeybinds()} 在后，
 * 所以 Pre 阶段的消费一定先于原版读取。
 *
 * <p>按需求的「仅技能可用时屏蔽」：冷却中、正在蓄力/冲刺/升空、
 * 未手持铁拳铁砧锤，这些情况下都<strong>不消费</strong>，E 仍然正常打开物品栏——
 * 玩家在冷却期间想整理背包不会被拦住。
 */
public final class SlamKeyHandler {
    private SlamKeyHandler() {
    }

    /**
     * 在 ClientTickEvent.Pre 调用。
     */
    public static void handleInventoryKey() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        // 复刻原版的调用条件：有界面/覆盖层时 handleKeybinds 根本不会执行，
        // 此时若把点击消费掉，玩家在 GUI 里按 E 就会被静默吞掉
        if (minecraft.screen != null || minecraft.getOverlay() != null) return;

        if (!canCast(player)) return;

        // 只有确实要释放技能时才消费这次点击
        if (minecraft.options.keyInventory.consumeClick()) {
            SeismicSlamClientState.requestIfAvailable(player);
        }
    }

    /**
     * 技能当前是否可用。不可用时返回 false，E 保持原版行为。
     */
    private static boolean canCast(LocalPlayer player) {
        if (!DoomfistEnchantmentUtil.isWieldingDoomfist(player)) return false;
        // 冷却中不抢 E
        if (SkillCooldowns.isOnCooldownClient(SkillCooldowns.Skill.SEISMIC_SLAM)) return false;
        // 技能互斥：蓄力 / 冲刺 / 上升 / 裂地位移期间不抢 E。
        // 注意上勾拳的「滞空」不算占用，所以悬空时 E 仍可触发指向性裂地（连招）。
        // 不可用时返回 false → E 保持原版「打开物品栏」行为。
        return !SkillGate.isBusy(player);
    }
}