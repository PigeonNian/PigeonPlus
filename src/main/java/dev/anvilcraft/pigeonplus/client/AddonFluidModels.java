package dev.anvilcraft.pigeonplus.client;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.fluid.PotionFluidUtil;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.level.material.Fluid;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID, value = Dist.CLIENT)
public final class AddonFluidModels {
    /** 原版水的静止贴图（32 帧动画）。 */
    private static final Material WATER_STILL =
        new Material(Identifier.withDefaultNamespace("block/water_still"));
    /** 原版水的流动贴图。 */
    private static final Material WATER_FLOW =
        new Material(Identifier.withDefaultNamespace("block/water_flow"));

    private static final int PIGEONPLUS_POTION_FALLBACK = 0xFF385DC6;

    private AddonFluidModels() {
    }

    @SubscribeEvent
    public static void registerFluidModels(RegisterFluidModelsEvent event) {
        pigeonplus$register(event, 0xFF6B8E3D,
            AddonFluids.GASEOUS_BIOGAS, null);
        pigeonplus$register(event, 0x66D9F2FF,
            AddonFluids.COMPRESSED_AIR, null);
        pigeonplus$register(event, 0xFF6E5F2C,
            AddonFluids.MIXED_BIOMASS, AddonFluids.MIXED_BIOMASS_FLOWING);
        pigeonplus$register(event, 0xD08FD2B3,
            AddonFluids.LIQUEFIED_BIOGAS, AddonFluids.LIQUEFIED_BIOGAS_FLOWING);
        pigeonplus$register(event, 0x7087CEEB,
            AddonFluids.LIQUID_OXYGEN, AddonFluids.LIQUID_OXYGEN_FLOWING);
        pigeonplus$register(event, 0x70B8E2F4,
            AddonFluids.LIQUID_HYDROGEN, AddonFluids.LIQUID_HYDROGEN_FLOWING);
        event.register(new FluidModel.Unbaked(
            WATER_STILL,
            WATER_FLOW,
            null,
            new FluidTintSource() {
                @Override
                public int color(FluidState state) {
                    return PIGEONPLUS_POTION_FALLBACK;
                }

                @Override
                public int colorAsStack(FluidStack stack) {
                    PotionContents contents = PotionFluidUtil.getContents(stack);
                    return contents == null
                        ? PIGEONPLUS_POTION_FALLBACK
                        : 0xFF000000 | contents.getColor();
                }
            }
        ), AddonFluids.POTION);
    }

    /**
     * 注册一种流体（可含其流动变体）。
     *
     * <p>气体类流体没有 {@code Flowing} 变体（{@code GasFluid} 不走原版流动逻辑），
     * 因此 {@code flowing} 传 {@code null} 表示只注册静止态。
     */
    private static void pigeonplus$register(
        RegisterFluidModelsEvent event,
        int tintColor,
        DeferredHolder<Fluid, ?> still,
        DeferredHolder<Fluid, ?> flowing
    ) {
        FluidModel.Unbaked model = new FluidModel.Unbaked(
            WATER_STILL,
            WATER_FLOW,
            null,
            FluidTintSources.constant(tintColor)
        );
        if (flowing == null) {
            event.register(model, still);
        } else {
            event.register(model, still, flowing);
        }
    }
}
