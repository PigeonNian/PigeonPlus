package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.block.MixedBiomassCauldronBlock;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;

/**
 * 混合生物质炼药锅的交互表。
 *
 * <h3>26.1 的变化</h3>
 * 空炼药锅（{@code Items.CAULDRON}）的交互表不再是全局静态表：
 * {@code CauldronInteraction.EMPTY} 被移除，改为按方块实例持有
 * {@link CauldronInteraction.Dispatcher}；相应的静态辅助方法（如
 * {@code fillBucket}/{@code emptyBucket}）也从 {@code CauldronInteraction}
 * 移到了 {@link CauldronInteractions}。
 *
 * <p>因此本类的 {@code MIXED_BIOMASS} 直接改成 {@code Dispatcher} 实例，
 * 由 {@link dev.anvilcraft.pigeonplus.block.MixedBiomassCauldronBlock}
 * 的构造器传给父类。往空锅里倒混合生物质的那条交互现在也注册在
 * 同一个 Dispatcher 上（锅本身就是「空锅 + 混合生物质」两种状态的载体）。
 */
public final class AddonInteractionMap {
    /** 混合生物质炼药锅的交互表，由方块构造器持有。 */
    public static final CauldronInteraction.Dispatcher MIXED_BIOMASS =
        new CauldronInteraction.Dispatcher();

    private AddonInteractionMap() {
    }

    public static void init() {
        // 用桶舀出：满锅时把桶换成混合生物质桶
        MIXED_BIOMASS.put(
            Items.BUCKET,
            (state, level, pos, player, hand, stack) -> CauldronInteractions.fillBucket(
                state,
                level,
                pos,
                player,
                hand,
                stack,
                AddonItems.MIXED_BIOMASS_BUCKET.asStack(),
                s -> AddonBlocks.MIXED_BIOMASS_CAULDRON.get().isFull(state),
                SoundEvents.BUCKET_FILL
            )
        );

        // 用桶倒入：把混合生物质桶倒进锅，锅变为满液位
        MIXED_BIOMASS.put(
            AddonItems.MIXED_BIOMASS_BUCKET.get(),
            (state, level, pos, player, hand, stack) -> CauldronInteractions.emptyBucket(
                level,
                pos,
                player,
                hand,
                stack,
                AddonBlocks.MIXED_BIOMASS_CAULDRON.get()
                    .defaultBlockState()
                    .setValue(MixedBiomassCauldronBlock.LEVEL, MixedBiomassCauldronBlock.MAX_LEVEL),
                SoundEvents.BUCKET_EMPTY
            )
        );
    }
}
