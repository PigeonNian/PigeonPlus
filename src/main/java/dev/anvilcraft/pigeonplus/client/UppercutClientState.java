package dev.anvilcraft.pigeonplus.client;

import dev.anvilcraft.pigeonplus.network.UppercutRequestPacket;
import dev.anvilcraft.pigeonplus.util.DoomfistEnchantmentUtil;
import dev.anvilcraft.pigeonplus.util.SkillCooldowns;
import dev.anvilcraft.pigeonplus.util.SkillGate;
import dev.anvilcraft.pigeonplus.util.UppercutAscentRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 客户端侧的上勾拳状态：Shift 单击触发 + 上升阶段。
 *
 * <p>与火箭重拳一样，位移由客户端执行（玩家位置客户端权威），服务端只做伤害结算。
 */
public final class UppercutClientState {
    /** 上一 tick Shift 是否处于按下状态，用于做「单击」边沿检测。 */
    private static boolean shiftWasDown;

    private UppercutClientState() {
    }

    /**
     * 客户端每 tick 调用：检测 Shift 单击并请求释放上勾拳。
     *
     * <p>必须是<strong>单击</strong>而不是按住：Shift 同时也是潜行键，
     * 若按住即触发，玩家平时潜行走路会不停搓出上勾拳。
     * 这里用「上一 tick 未按下、这一 tick 按下」的上升沿来判定。
     */
    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            shiftWasDown = false;
            return;
        }

        boolean shiftDown = minecraft.options.keyShift.isDown();
        boolean justPressed = shiftDown && !shiftWasDown;
        shiftWasDown = shiftDown;

        if (!justPressed) return;
        // 只在手持带铁拳附魔的铁砧锤时才请求；否则玩家每次潜行都会白发一个包。
        // 真正的校验仍在服务端（客户端判断只是省流量）。
        if (!DoomfistEnchantmentUtil.isWieldingDoomfist(player)) return;
        // 技能互斥：蓄力 / 冲刺 / 上升 / 裂地位移期间不得起手。
        // 与地面指示器、E 键拦截共用同一套判据，避免各处条件不一致。
        if (SkillGate.isBusy(player)) return;
        if (SkillCooldowns.isOnCooldownClient(SkillCooldowns.Skill.UPPERCUT)) return;

        PacketDistributor.sendToServer(new UppercutRequestPacket());
    }

    /**
     * 进入上升阶段。
     */
    public static void startAscent(double upSpeed, int ticks) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        UppercutAscentRegistry.start(player.getUUID(), upSpeed, ticks);
    }

    /**
     * 玩家是否处于上升阶段（供输入拦截与 travel 使用）。
     */
    public static boolean isAscending(LocalPlayer player) {
        return UppercutAscentRegistry.isAscending(player.getUUID());
    }
}
