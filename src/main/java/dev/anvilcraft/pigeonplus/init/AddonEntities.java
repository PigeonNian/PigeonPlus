package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.EntityEntry;
import dev.anvilcraft.pigeonplus.client.renderer.entity.LargeFireworksRocketRenderer;
import dev.anvilcraft.pigeonplus.client.renderer.entity.LargeTntRenderer;
import dev.anvilcraft.pigeonplus.entity.LargeFireworksRocketEntity;
import dev.anvilcraft.pigeonplus.entity.LargeFireworksRocketSeatEntity;
import dev.anvilcraft.pigeonplus.entity.LargeTntEntity;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.MobCategory;

import static dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus.REGISTRUM;

/**
 * 本模组的实体。
 *
 * <p>渲染器由 {@code .renderer(...)} 交给 Registrum，它内部用
 * {@code RegistrumDistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)} 注册，
 * 所以这里直接引用客户端类是安全的。
 */
public class AddonEntities {
    /** 尺寸 3×3×9，与模型轮廓一致。{@code updateInterval(1)} 是因为实体一直在加速。 */
    public static final EntityEntry<LargeFireworksRocketEntity> LARGE_FIREWORKS_ROCKET = REGISTRUM
        .<LargeFireworksRocketEntity>entity("large_fireworks_rocket", LargeFireworksRocketEntity::new, MobCategory.MISC)
        .properties(builder -> builder
            .sized(3.0F, 9.0F)
            .clientTrackingRange(16)
            .updateInterval(1)
            .fireImmune())
        .renderer(() -> LargeFireworksRocketRenderer::new)
        .register();

    /**
     * 火箭顶上的座位，让玩家能坐到火箭顶面。
     */
    public static final EntityEntry<LargeFireworksRocketSeatEntity> LARGE_FIREWORKS_ROCKET_SEAT = REGISTRUM
        .<LargeFireworksRocketSeatEntity>entity(
            "large_fireworks_rocket_seat", LargeFireworksRocketSeatEntity::new, MobCategory.MISC)
        .properties(builder -> builder
            .sized(0.5F, 0.5F)
            .clientTrackingRange(16)
            .noSummon()
            .fireImmune())
        .renderer(() -> NoopRenderer::new)
        .register();

    /** 大型TNT点燃后的实体：比方块结构小 0.1 格（2.9×2.9），避免刚生成时卡在 3×3×3 坑口的边缘方块上。 */
    public static final EntityEntry<LargeTntEntity> LARGE_TNT = REGISTRUM
        .<LargeTntEntity>entity("large_tnt", LargeTntEntity::new, MobCategory.MISC)
        .lang("Large TNT")
        .properties(builder -> builder
            .sized(LargeTntEntity.SIZE, LargeTntEntity.SIZE)
            .clientTrackingRange(16)
            // 大碰撞箱下落时一 tick 位移明显，10 tick 同步一次会让客户端看到"一跳一跳"的位移。
            .updateInterval(2)
            .fireImmune())
        .renderer(() -> LargeTntRenderer::new)
        .register();

    public static void register() {
    }
}
