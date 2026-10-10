package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.EntityEntry;
import dev.anvilcraft.pigeonplus.client.renderer.entity.LargeFireworksRocketRenderer;
import dev.anvilcraft.pigeonplus.entity.LargeFireworksRocketEntity;
import dev.anvilcraft.pigeonplus.entity.LargeFireworksRocketSeatEntity;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.MobCategory;

import static dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus.REGISTRUM;

public class AddonEntities {

    public static final EntityEntry<LargeFireworksRocketEntity> LARGE_FIREWORKS_ROCKET = REGISTRUM
        .<LargeFireworksRocketEntity>entity("large_fireworks_rocket", LargeFireworksRocketEntity::new, MobCategory.MISC)
        .properties(builder -> builder
            .sized(3.0F, 9.0F)
            .clientTrackingRange(16)
            .updateInterval(1)
            .noLootTable()
            .fireImmune())
        .renderer(() -> LargeFireworksRocketRenderer::new)
        .register();

    public static final EntityEntry<LargeFireworksRocketSeatEntity> LARGE_FIREWORKS_ROCKET_SEAT = REGISTRUM
        .<LargeFireworksRocketSeatEntity>entity(
            "large_fireworks_rocket_seat", LargeFireworksRocketSeatEntity::new, MobCategory.MISC)
        .properties(builder -> builder
            .sized(0.5F, 0.5F)
            .clientTrackingRange(16)
            .noSummon()
            .noLootTable()
            .fireImmune())
        .renderer(() -> NoopRenderer::new)
        .register();

    public static void register() {
    }
}
