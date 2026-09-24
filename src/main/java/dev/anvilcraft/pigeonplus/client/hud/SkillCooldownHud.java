package dev.anvilcraft.pigeonplus.client.hud;

import dev.anvilcraft.pigeonplus.util.DoomfistEnchantmentUtil;
import dev.anvilcraft.pigeonplus.util.SkillCooldowns;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * 屏幕右下角的技能列表：每行「按键提示 + 技能名 + 剩余秒数」。
 *
 * <p>三个技能<strong>常驻</strong>显示，就绪时数字留空（行高固定，不会跳动），
 * 用<strong>颜色</strong>区分状态：就绪为亮白、冷却中为暗灰。
 *
 * <p>按键提示取自 {@code KeyMapping.getTranslatedKeyMessage()}，而不是硬编码「E / Shift / 右键」：
 * 玩家改键位后提示会自动跟着变，也不会在别的语言下露出中文。
 *
 * <p>只在手持带铁拳附魔的铁砧锤时显示：技能本就依附这把武器，
 * 空手或换别的物品时没必要占着屏幕。
 *
 * <p>数据来自 {@link SkillCooldowns} 的客户端镜像——服务端下发
 * {@code SkillCooldownPacket} 后写入，与服务端放行判断同源，
 * 不会出现「HUD 显示好了但还打不出去」的偏差。
 */
public final class SkillCooldownHud {
    /** 距屏幕右下角的留白。 */
    private static final int MARGIN = 6;
    /** 行高。固定值，保证数字有无都不跳动。 */
    private static final int ROW_HEIGHT = 11;

    /** 列间距。 */
    private static final int GAP = 4;

    /**
     * 就绪（可释放）时的颜色：亮白。
     *
     * <p>整行统一取色而不是只染数字：就绪时数字是空的，
     * 只染数字会让最需要区分的状态反而没有着色载体。
     */
    private static final int COLOR_READY = 0xFFFFFFFF;
    /** 冷却中的颜色：暗灰。与就绪的亮白形成明度对比。 */
    private static final int COLOR_COOLDOWN = 0xFF5A5A5A;

    /** 按键提示的颜色：两种状态下都保持中性灰，避免与状态色抢视觉。 */
    private static final int COLOR_KEY = 0xFF9A9A9A;

    private SkillCooldownHud() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.screen != null) return;
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        // 只在手持带铁拳附魔的铁砧锤时显示
        if (!DoomfistEnchantmentUtil.isWieldingDoomfist(player)) return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(minecraft.isPaused());

        SkillCooldowns.Skill[] skills = SkillCooldowns.Skill.values();
        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();

        // 从下往上排：最后一个技能贴底，其余依次向上。
        // 用固定行高乘以总数定位首行，整体高度恒定。
        int firstRowY = guiHeight - MARGIN - skills.length * ROW_HEIGHT;

        // 右侧对齐：先量出最宽的一行，再统一左边界，各列才能对齐
        int widest = 0;
        for (SkillCooldowns.Skill skill : skills) {
            widest = Math.max(widest, rowWidth(minecraft, skill, partialTick));
        }
        int left = guiWidth - MARGIN - widest;

        for (int i = 0; i < skills.length; i++) {
            SkillCooldowns.Skill skill = skills[i];
            int y = firstRowY + i * ROW_HEIGHT;
            renderRow(graphics, minecraft, skill, left, y, partialTick);
        }
    }

    /**
     * 一行的宽度：按键提示 + 间距 + 技能名 + 间距 + 数字（就绪时数字为空）。
     *
     * <p>间距统一用 {@link #GAP}，保证「量宽度」与「画文字」是同一套布局，
     * 否则右对齐会差几个像素。
     */
    private static int rowWidth(Minecraft minecraft, SkillCooldowns.Skill skill, float partialTick) {
        return minecraft.font.width(keyTextFor(minecraft, skill))
            + GAP
            + minecraft.font.width(labelFor(skill))
            + GAP
            + minecraft.font.width(cooldownText(skill, partialTick));
    }

    /**
     * 按键提示文本，例如 {@code [右键]}。
     *
     * <p>若该键未绑定（玩家清空了绑定），退化为空串而不是显示 "unknown"。
     */
    private static String keyTextFor(Minecraft minecraft, SkillCooldowns.Skill skill) {
        KeyMapping mapping = mappingFor(minecraft, skill.keyHint());
        if (mapping == null || mapping.isUnbound()) return "";
        return "[" + mapping.getTranslatedKeyMessage().getString() + "]";
    }

    /**
     * 把技能的按键标识解析成实际的 {@code KeyMapping}。
     *
     * <p>这一步放在客户端做：{@code SkillCooldowns} 位于 common 侧，
     * 不能引用需要客户端环境的 {@code Options}。
     */
    private static KeyMapping mappingFor(Minecraft minecraft, SkillCooldowns.KeyHint hint) {
        return switch (hint) {
            case USE -> minecraft.options.keyUse;
            case SHIFT -> minecraft.options.keyShift;
            case INVENTORY -> minecraft.options.keyInventory;
        };
    }

    /**
     * 冷却文本：冷却中为剩余秒数，就绪时为空串。
     *
     * <p>就绪时留空，但行高已固定，所以布局不会抖动；
     * 「是否就绪」由整行颜色表达。
     */
    private static String cooldownText(SkillCooldowns.Skill skill, float partialTick) {
        int seconds = SkillCooldowns.remainingSecondsCeil(skill, partialTick);
        return seconds > 0 ? Integer.toString(seconds) : "";
    }

    private static Component labelFor(SkillCooldowns.Skill skill) {
        return Component.translatable(skill.translationKey());
    }

    private static void renderRow(
        GuiGraphics graphics,
        Minecraft minecraft,
        SkillCooldowns.Skill skill,
        int left,
        int y,
        float partialTick
    ) {
        String cooldown = cooldownText(skill, partialTick);
        // 整行同一个状态色：就绪=亮白、冷却中=暗灰，扫一眼就能判断
        int color = cooldown.isEmpty() ? COLOR_READY : COLOR_COOLDOWN;

        int x = left;

        // 按键提示：中性灰，不随状态变色，避免与状态色抢视觉
        String key = keyTextFor(minecraft, skill);
        if (!key.isEmpty()) {
            graphics.drawString(minecraft.font, key, x, y, COLOR_KEY, true);
            x += minecraft.font.width(key) + GAP;
        }

        Component label = labelFor(skill);
        graphics.drawString(minecraft.font, label, x, y, color, true);
        x += minecraft.font.width(label) + GAP;

        if (!cooldown.isEmpty()) {
            graphics.drawString(minecraft.font, cooldown, x, y, color, true);
        }
    }
}
