package dev.anvilcraft.pigeonplus.client.tooltip;

import dev.anvilcraft.pigeonplus.block.entity.StasisBeaconBlockEntity;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class StasisBeaconTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity value) {
        return value instanceof StasisBeaconBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        if (!(value instanceof StasisBeaconBlockEntity beacon)) {
            return List.of();
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("静滞信标").withStyle(ChatFormatting.AQUA));
        if (beacon.getFrozenEntityClientId() < 0) {
            lines.add(Component.literal("  状态：未锁定").withStyle(ChatFormatting.GRAY));
            return lines;
        }
        lines.add(Component.literal("  状态：时停中").withStyle(ChatFormatting.BLUE));
        lines.add(Component.literal("  时长：%.2f 秒".formatted(beacon.getFrozenTicks() / 20.0)).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("  累计伤害：%.2f / %.2f".formatted(
            beacon.getFrozenAccumulatedDamage(),
            10.0f
        )).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("  累计速度：%.2f b/t".formatted(beacon.getFrozenAccumulatedSpeed())).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    @Override
    public int priority() {
        return -10;
    }
}
