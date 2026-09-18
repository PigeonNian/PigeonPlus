package dev.anvilcraft.pigeonplus.client.hud;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.util.DoomfistEnchantmentUtil;
import dev.anvilcraft.pigeonplus.util.RocketPunchManager;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 屏幕右下角的火箭重拳冷却计时器。
 *
 * <p>只在手持带铁拳附魔的铁砧锤、且冷却中时显示。
 * 读的是原版 {@code ItemCooldowns} 的剩余比例，与服务端的放行判断同源，
 * 因此不会出现“HUD 显示好了但还打不出去”的偏差。
 */
public final class RocketPunchCooldownHud {
    private static final int MARGIN = 6;
    private static final int BAR_WIDTH = 60;
    private static final int BAR_HEIGHT = 5;
    private static final int ICON_SIZE = 16;

    private RocketPunchCooldownHud() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) return;
        if (minecraft.screen != null) return;

        LocalPlayer player = minecraft.player;
        ItemStack stack = player.getMainHandItem();
        // 只有手持带铁拳附魔的铁砧锤才显示
        if (!stack.is(ModItemTags.ANVIL_HAMMER) || !DoomfistEnchantmentUtil.hasDoomfist(stack)) return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(minecraft.isPaused());
        float progress = RocketPunchManager.cooldownProgress(player, stack, partialTick);
        if (progress <= 0.0f) return;

        float remainingSeconds = RocketPunchManager.cooldownRemainingSeconds(player, stack, partialTick);
        int guiHeight = graphics.guiHeight();
        int guiWidth = graphics.guiWidth();

        // 右下角布局：图标 + 进度条 + 秒数
        int barX = guiWidth - MARGIN - BAR_WIDTH;
        int barY = guiHeight - MARGIN - BAR_HEIGHT;

        // 进度条底槽（深色）与进度（白→红随剩余时间变化）
        graphics.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, 0xC0000000);
        int filled = Math.round(BAR_WIDTH * progress);
        int color = progress > 0.5f ? 0xFFFF5555 : 0xFFFFAA00;
        graphics.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, color);

        // 剩余秒数，保留一位小数，与进度条左对齐
        String text = String.format("%.1fs", remainingSeconds);
        graphics.drawString(
            minecraft.font,
            Component.literal(text),
            barX,
            barY - minecraft.font.lineHeight - 2,
            0xFFFFFFFF,
            true
        );
    }
}
