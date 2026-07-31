package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.anvilcraft.pigeonplus.init.AddonVaporizationSources;
import dev.anvilcraft.pigeonplus.util.GasEscapeUtil;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;

@Mixin(LargeCauldronBlockEntity.class)
public class LargeCauldronBlockEntityMixin {
    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void pigeonplus$escapeGas(
        Level level,
        BlockPos pos,
        BlockState state,
        LargeCauldronBlockEntity entity,
        CallbackInfo ci
    ) {
        if (entity.isMainPart()) {
            GasEscapeUtil.escapeLargeCauldronGas(level, pos, entity.getFluids());
        }
    }

    @Inject(method = "canIgniteTopFluid", at = @At("RETURN"), cancellable = true)
    private void pigeonplus$allowMixedPropellantIgnition(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            AddonVaporizationSources.hasAnyPropellant((LargeCauldronBlockEntity) (Object) this);
        }
    }

    @Redirect(
        method = "tryProcessFluidMixingRecipe",
        at = @At(
            value = "INVOKE",
            target = "Ldev/dubhe/anvilcraft/recipe/FluidMixingRecipe;getMaximumBatches(Ljava/util/List;)I"
        )
    )
    private int pigeonplus$requireHeaterForMixedBiomass(
        FluidMixingRecipe recipe,
        List<FluidStack> storedFluids,
        ServerLevel level
    ) {
        if (pigeonplus$isMixedBiomassRecipe(recipe) && !pigeonplus$hasActiveHeaterBelow(level)) {
            return 0;
        }
        return recipe.getMaximumBatches(storedFluids);
    }

    @Unique
    private boolean pigeonplus$hasActiveHeaterBelow(Level level) {
        BlockPos center = ((LargeCauldronBlockEntity) (Object) this).getBlockPos().below(2);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            if (pigeonplus$isActiveHeatingHelper(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static boolean pigeonplus$isActiveHeatingHelper(BlockState state) {
        if (state.is(ModBlocks.HEATER)) {
            return !state.getValue(HeaterBlock.OVERLOAD);
        }
        return state.is(ModBlocks.BURNING_HEATER) && state.getValue(BurningHeaterBlock.LEVEL) == 2;
    }

    @Unique
    private static boolean pigeonplus$isMixedBiomassRecipe(FluidMixingRecipe recipe) {
        return recipe.getFluidIngredients().size() == 1
            && Arrays.stream(recipe.getFluidIngredients().getFirst().getFluids())
                .anyMatch(fluid -> fluid.getFluid().isSame(AddonFluids.MIXED_BIOMASS.get()))
            && recipe.getFluidResults().stream()
                .anyMatch(fluid -> fluid.getFluid().isSame(AddonFluids.GASEOUS_BIOGAS.get()));
    }
}
