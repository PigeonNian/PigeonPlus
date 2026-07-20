package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.block.MixedBiomassCauldronBlock;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;

public class AddonInteractionMap {
    public static final CauldronInteraction.InteractionMap MIXED_BIOMASS =
        CauldronInteraction.newInteractionMap("mixed_biomass");

    public static void init() {
        var mixedBiomassInteractionMap = MIXED_BIOMASS.map();
        mixedBiomassInteractionMap.put(
            Items.BUCKET,
            (state, level, pos, player, hand, stack) -> CauldronInteraction.fillBucket(
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

        CauldronInteraction.EMPTY.map().put(
            AddonItems.MIXED_BIOMASS_BUCKET.get(),
            (state, level, pos, player, hand, stack) -> CauldronInteraction.emptyBucket(
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
