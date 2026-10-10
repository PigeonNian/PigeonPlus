package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import dev.anvilcraft.pigeonplus.util.PotionContainerGeometry;
import dev.dubhe.anvilcraft.api.event.FishTankEvent;
import dev.dubhe.anvilcraft.api.event.LargeCauldronEvent;
import dev.dubhe.anvilcraft.api.fluid.FluidStackResourceHandler;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class PotionSoakEffectListener {
    private static final int SOAK_COST = 250;
    private static final int CHECK_INTERVAL_TICKS = 20;

    private static final String CHECK_STAMP = "anvilcraft_pigeon_plus.potion_soak_check";

    @SubscribeEvent
    public static void onFishTankInside(FishTankEvent.EntityInside event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        FluidStackResourceHandler tank = event.getTank().getFluidHandler();
        apply(level, event.getEntity(), PotionFluidUtil.allPotions(tank),
            PotionContainerGeometry.fishTankSurfaceY(event.getPos(), tank),
            (stored, transaction) -> tank.extract(FluidResource.of(stored), SOAK_COST, transaction));
    }

    @SubscribeEvent
    public static void onCauldronInside(LargeCauldronEvent.EntityInside event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        LargeCauldronBlockEntity main = mainCauldron(level, event.getPos(), event.getState());
        if (main == null) {
            return;
        }
        LargeCauldronFluidHandler fluids = main.getFluids();
        apply(level, event.getEntity(), PotionFluidUtil.allPotions(fluids),
            PotionContainerGeometry.cauldronSurfaceY(main.getBlockPos(), fluids),
            (stored, transaction) -> fluids.extract(FluidResource.of(stored), SOAK_COST, transaction));
    }

    private static void apply(
        ServerLevel level,
        Entity entity,
        List<FluidStack> potions,
        double surfaceY,
        FluidDrainer drainer
    ) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        if (entity.getY() >= surfaceY) {
            return;
        }
        if (!claimCheck(living, level.getGameTime())) {
            return;
        }
        for (FluidStack potion : potions) {
            PotionContents contents = PotionFluidUtil.getContents(potion);
            if (contents == null || shouldSkip(living, contents)) {
                continue;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                if (drainer.extract(potion, transaction) < SOAK_COST) {
                    continue;
                }
                transaction.commit();
            }
            applyEffects(level, living, contents);
        }
    }

    private static boolean shouldSkip(LivingEntity living, PotionContents contents) {
        for (MobEffectInstance instance : contents.getAllEffects()) {
            if (instance.getEffect().value().isInstantenous()) {

                return false;
            }
            MobEffectInstance current = living.getEffect(instance.getEffect());
            if (current == null || current.getAmplifier() < instance.getAmplifier()) {
                return false;
            }
        }
        return true;
    }

    private static void applyEffects(ServerLevel serverLevel, LivingEntity living, PotionContents contents) {
        for (MobEffectInstance instance : contents.getAllEffects()) {
            MobEffect effect = instance.getEffect().value();
            if (effect.isInstantenous()) {
                effect.applyInstantenousEffect(serverLevel, null, null, living, instance.getAmplifier(), 1.0);
            } else {

                living.addEffect(new MobEffectInstance(instance));
            }
        }
    }

    private static boolean claimCheck(LivingEntity living, long tick) {
        CompoundTag data = living.getPersistentData();
        long last = data.getLongOr(CHECK_STAMP, 0L);
        if (last != 0L && tick - last < CHECK_INTERVAL_TICKS) {
            return false;
        }
        data.putLong(CHECK_STAMP, tick);
        return true;
    }

    @Nullable
    private static LargeCauldronBlockEntity mainCauldron(Level level, BlockPos pos, BlockState state) {
        LargeCauldronBlockEntity main = LargeCauldronBlockEntity.getMain(level, pos, state);
        return main != null && main.isMainPart() ? main : null;
    }

    @FunctionalInterface
    private interface FluidDrainer {
        int extract(FluidStack stored, TransactionContext transaction);
    }
}
