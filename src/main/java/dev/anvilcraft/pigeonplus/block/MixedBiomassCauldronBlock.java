package dev.anvilcraft.pigeonplus.block;

import dev.anvilcraft.pigeonplus.init.AddonInteractionMap;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.Layered4LevelCauldronBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class MixedBiomassCauldronBlock extends Layered4LevelCauldronBlock implements IHammerRemovable {
    public MixedBiomassCauldronBlock(Properties properties) {
        super(properties, AddonInteractionMap.MIXED_BIOMASS);
    }

    @Override
    public ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        CauldronInteraction interaction = this.interactions.map().get(stack.getItem());
        if (interaction == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return interaction.interact(state, level, pos, player, hand, stack);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    }

    @Override
    public ItemStack getCloneItemStack(
        BlockState state,
        HitResult target,
        LevelReader level,
        BlockPos pos,
        Player player
    ) {
        return new ItemStack(Items.CAULDRON);
    }
}
