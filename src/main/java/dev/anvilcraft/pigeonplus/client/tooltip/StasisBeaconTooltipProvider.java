package dev.anvilcraft.pigeonplus.client.tooltip;

import dev.anvilcraft.pigeonplus.block.entity.StasisBeaconBlockEntity;
import dev.anvilcraft.pigeonplus.util.StasisTimeFreezeManager;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class StasisBeaconTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    private static final String KEY_STATUS_IDLE = "tooltip.anvilcraft_pigeon_plus.stasis_beacon.status.idle";
    private static final String KEY_STATUS_FROZEN = "tooltip.anvilcraft_pigeon_plus.stasis_beacon.status.frozen";
    private static final String KEY_DURATION = "tooltip.anvilcraft_pigeon_plus.stasis_beacon.duration";
    private static final String KEY_DAMAGE = "tooltip.anvilcraft_pigeon_plus.stasis_beacon.damage";
    private static final String KEY_SPEED = "tooltip.anvilcraft_pigeon_plus.stasis_beacon.speed";

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
        lines.add(Component.translatable("block.anvilcraft_pigeon_plus.stasis_beacon")
            .withStyle(ChatFormatting.AQUA));
        if (beacon.getFrozenEntityClientId() < 0) {
            lines.add(Component.translatable(KEY_STATUS_IDLE).withStyle(ChatFormatting.GRAY));
            return lines;
        }
        lines.add(Component.translatable(KEY_STATUS_FROZEN).withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable(
            KEY_DURATION,
            "%.2f".formatted(beacon.getFrozenTicks() / 20.0)
        ).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
            KEY_DAMAGE,
            "%.2f".formatted(beacon.getFrozenAccumulatedDamage()),
            "%.2f".formatted(StasisTimeFreezeManager.MAX_STORED_DAMAGE)
        ).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
            KEY_SPEED,
            "%.2f".formatted(beacon.getFrozenAccumulatedSpeed())
        ).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    @Override
    public int priority() {
        return -10;
    }
}
