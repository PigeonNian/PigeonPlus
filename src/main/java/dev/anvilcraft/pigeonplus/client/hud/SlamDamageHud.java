package dev.anvilcraft.pigeonplus.client.hud;

import dev.anvilcraft.pigeonplus.client.SeismicSlamClientState;
import dev.anvilcraft.pigeonplus.util.SlamManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 准星下方的裂地重拳伤害指示器，形如 {@code [42]}。
 *
 * <p>只在技能进行中（已跃起、正在飞或下坠）显示，数值随滞空时长实时增长，
 * 让玩家直观看到「再飘一会儿能打更疼」。
 *
 * <p>数值直接调 {@link SlamManager#damageForAirtime}——与实际结算<strong>同一份公式</strong>。
 * 若这里另写一份，改动伤害参数时就可能出现「显示 60 实际打 45」的偏差。
 *
 * <p>滞空时长是客户端自己数的（服务端另有权威记录），因此显示值可能与最终结算
 * 相差 ±1 tick；对指示器而言这个精度足够，且不会影响实际伤害。
 */
public final class SlamDamageHud {
    /** 整组相对准心向下的偏移。避开准星本身，也避开上方的蓄力条。 */
    private static final int CROSSHAIR_OFFSET_Y = 16;
    /** 水平居中。 */
    private static final int CENTER_DIVISOR = 2;

    /** 方括号与数字的颜色（白色）。 */
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    /** 文字带阴影，保证在天空/雪地等亮背景上也能看清。 */
    private static final boolean DROP_SHADOW = true;

    private SlamDamageHud() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.screen != null) return;
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        // 只在裂地重拳进行中显示
        if (!SeismicSlamClientState.isAirborne()) return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(minecraft.isPaused());
        float airtime = SeismicSlamClientState.airtimeTicks(partialTick);
        // 与服务端同一份公式：滞空越久伤害越高，超过上限即为满伤
        float damage = SlamManager.damageForAirtime(Mth.floor(airtime));

        String text = "[" + Math.round(damage) + "]";

        int centerX = graphics.guiWidth() / CENTER_DIVISOR;
        int textWidth = minecraft.font.width(text);
        int y = graphics.guiHeight() / CENTER_DIVISOR + CROSSHAIR_OFFSET_Y;

        graphics.drawString(
            minecraft.font,
            Component.literal(text),
            centerX - textWidth / CENTER_DIVISOR,
            y,
            COLOR_TEXT,
            DROP_SHADOW
        );
    }
}
