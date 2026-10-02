package dev.anvilcraft.pigeonplus.block;

import dev.anvilcraft.pigeonplus.init.AddonInteractionMap;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.cauldron.Layered4LevelCauldronBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
    public InteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        // 26.1 的 Dispatcher 直接按物品栈查询交互
        CauldronInteraction interaction = this.interactions.get(stack);
        if (interaction == null) {
            return InteractionResult.PASS;
        }
        return interaction.interact(state, level, pos, player, hand, stack);
    }

    /**
     * 26.1 的 {@code entityInside} 新增 {@code InsideBlockEffectApplier} 与 boolean 两个参数。
     * 本模组的炼药锅不需要方块内效果派发，故忽略它们。
     */
    @Override
    public void entityInside(
        BlockState state,
        Level level,
        BlockPos pos,
        Entity entity,
        net.minecraft.world.entity.InsideBlockEffectApplier effectApplier,
        boolean isInside
    ) {
    }

    /**
     * 26.1 把 {@code getCloneItemStack} 的签名由
     * {@code (BlockState, HitResult, LevelReader, BlockPos, Player)}
     * 改为 {@code (LevelReader, BlockPos, BlockState, boolean)}，
     * 且不再需要 {@code HitResult} 与 {@code Player}。
     */
    @Override
    protected ItemStack getCloneItemStack(
        LevelReader level,
        BlockPos pos,
        BlockState state,
        boolean includeData
    ) {
        return new ItemStack(Items.CAULDRON);
    }
}
