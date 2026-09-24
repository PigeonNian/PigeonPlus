package dev.anvilcraft.pigeonplus.client;

import dev.anvilcraft.pigeonplus.network.UppercutRequestPacket;
import dev.anvilcraft.pigeonplus.util.DoomfistEnchantmentUtil;
import dev.anvilcraft.pigeonplus.util.SkillCooldowns;
import dev.anvilcraft.pigeonplus.util.SkillGate;
import dev.anvilcraft.pigeonplus.util.UppercutAscentRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 客户端侧的上勾拳状态：Shift 单击触发 + 上升阶段。
 *
 * <p>与火箭重拳一样，位移由客户端执行（玩家位置客户端权威），服务端只做伤害结算。
 */
public final class UppercutClientState {
    /**
     * 挥击动作的时长（tick）。
     *
     * <p><strong>必须等于上升阶段时长</strong>（服务端的 {@code UppercutManager.ASCENT_TICKS} = 10），
     * 不能更长。原因：指向性裂地重拳的互斥判据只排除「上升」阶段
     * （为了保住「上挑 → 飞扑」连招），一旦上升结束它就解禁。
     * 若挥击动作比上升活得久，那多出来的几 tick 里玩家就能放出裂地重拳，
     * 两套手部动画同时作用到同一个 PoseStack，姿态会叠加扭曲。
     *
     * <p>所以这里对齐时长，而不是反过来为了动画去改裂地的放行条件——
     * 连招手感是玩法设计，不该被动画牵着走。
     */
    private static final int SWING_DURATION = 10;

    /** 上一 tick Shift 是否处于按下状态，用于做「单击」边沿检测。 */
    private static boolean shiftWasDown;

    /** 挥击动作剩余 tick。与 {@link RocketPunchAnimState} 同理：命中/挥击是一瞬间的事件，需要自己记时长。 */
    private static int swingTicks;

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
        // 递减挥击计时。放在最前：即使玩家为 null（切换世界）也要继续走完，
        // 否则会残留一个永不结束的动作，下次进入世界时闪一下。
        if (swingTicks > 0) swingTicks--;

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
     *
     * <p>同时触发挥击动作：这个包的唯一来源就是「服务端确认上勾拳生效」，
     * 所以这里就是客户端最早的起手时机。
     */
    public static void startAscent(double upSpeed, int ticks) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        UppercutAscentRegistry.start(player.getUUID(), upSpeed, ticks);
        // 与命中动作不同，这里不做去重：上勾拳有冷却，不可能在一次动作内被再次触发。
        swingTicks = SWING_DURATION;
    }

    /**
     * 挥击动作的进度：0.0 = 刚起手，1.0 = 播放完毕。
     *
     * <p>用剩余 tick 反推已用时长，再按 {@code partialTick} 插值，保证与渲染帧对齐。
     */
    public static float swingProgress(float partialTick) {
        if (swingTicks <= 0) return 0.0f;
        float elapsed = SWING_DURATION - (swingTicks - partialTick);
        return Mth.clamp(elapsed / SWING_DURATION, 0.0f, 1.0f);
    }

    /**
     * 断线或换维度时清空，避免残留动作在下次进入世界时闪一下。
     */
    public static void reset() {
        swingTicks = 0;
    }

    /**
     * 玩家是否处于上升阶段（供输入拦截与 travel 使用）。
     */
    public static boolean isAscending(LocalPlayer player) {
        return UppercutAscentRegistry.isAscending(player.getUUID());
    }
}
