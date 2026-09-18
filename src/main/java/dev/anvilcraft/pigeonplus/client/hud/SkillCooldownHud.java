package dev.anvilcraft.pigeonplus.client.hud;

import dev.anvilcraft.pigeonplus.util.SkillCooldowns;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 屏幕右下角的统一技能冷却计时器。
 *
 * <p>同时展示所有「可用技能」的冷却状态，每个技能一行：名称 + 进度条 + 剩余秒数。
 * 因为两个技能同属铁拳附魔，它们要么一起可用、要么一起不可用，
 * 所以不需要按手持物切换显示内容。
 *
 * <p>数据来自 {@link SkillCooldowns} 的客户端镜像——那是服务端下发
 * {@code SkillCooldownPacket} 后写入的，与服务端的放行判断同源，
 * 不会出现「HUD 显示好了但还打不出去」的偏差。
 *
 * <p>冷却全部走完时整层不绘制，避免平时占着屏幕。
 */
public final class SkillCooldownHud {
    /** 距屏幕边缘的留白。 */
    private static final int MARGIN = 6;
    private static final int BAR_WIDTH = 60;
    private static final int BAR_HEIGHT = 5;
    /** 行间距，留出文字高度。 */
    private static final int ROW_GAP = 14;

    private SkillCooldownHud() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) return;
        if (minecraft.screen != null) return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(minecraft.isPaused());

        // 先收集所有仍在冷却的技能；一个都没有就整层不画
        SkillCooldowns.Skill[] skills = SkillCooldowns.Skill.values();
        boolean anyActive = false;
        for (SkillCooldowns.Skill skill : skills) {
            if (SkillCooldowns.progress(skill, partialTick) > 0.0f) {
                anyActive = true;
                break;
            }
        }
        if (!anyActive) return;

        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();

        // 从右下角往上排：最后一个技能贴底，其余依次向上
        int barX = guiWidth - MARGIN - BAR_WIDTH;
        int bottomBarY = guiHeight - MARGIN - BAR_HEIGHT;

        int row = 0;
        for (int index = skills.length - 1; index >= 0; index--) {
            SkillCooldowns.Skill skill = skills[index];
            float progress = SkillCooldowns.progress(skill, partialTick);
            if (progress <= 0.0f) continue;

            int barY = bottomBarY - row * ROW_GAP;
            renderRow(graphics, minecraft, skill, progress, barX, barY, partialTick);
            row++;
        }
    }

    /**
     * 画一行：进度条底槽 + 进度 + 上方的「名称 剩余秒数」。
     */
    private static void renderRow(
        GuiGraphics graphics,
        Minecraft minecraft,
        SkillCooldowns.Skill skill,
        float progress,
        int barX,
        int barY,
        float partialTick
    ) {
        graphics.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, 0xC0000000);

        // 剩余时间越少条越短：progress 是「剩余比例」，所以从左往右消减
        int filled = Math.round(BAR_WIDTH * progress);
        graphics.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, skill.barColor());

        float seconds = SkillCooldowns.remainingSeconds(skill, partialTick);
        Component text = Component.translatable(skill.translationKey())
            .append(" ")
            .append(Component.literal(String.format("%.1fs", seconds)));

        graphics.drawString(
            minecraft.font,
            text,
            barX,
            barY - minecraft.font.lineHeight - 1,
            0xFFFFFFFF,
            true
        );
    }
}
