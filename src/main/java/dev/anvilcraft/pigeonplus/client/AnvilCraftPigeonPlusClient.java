package dev.anvilcraft.pigeonplus.client;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.entity.ModBlockEntities;
import dev.anvilcraft.pigeonplus.client.particle.RollingPlasmaParticle;
import dev.anvilcraft.pigeonplus.client.renderer.block.AnvilPumpBlockEntityRenderer;
import dev.anvilcraft.pigeonplus.client.renderer.block.BlenderBlockEntityRenderer;
import dev.anvilcraft.pigeonplus.client.renderer.block.NozzleExhaustBlockEntityRenderer;
import dev.anvilcraft.pigeonplus.client.renderer.block.StasisBeaconBlockEntityRenderer;
import dev.anvilcraft.pigeonplus.client.tooltip.StasisBeaconTooltipProvider;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.anvilcraft.pigeonplus.init.AddonItems;
import dev.anvilcraft.pigeonplus.init.AddonParticles;
import dev.dubhe.anvilcraft.util.ModClientFluidTypeExtensionImpl;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import dev.dubhe.anvilcraft.api.tooltip.HudTooltipManager;

@Mod(value = AnvilCraftPigeonPlus.MOD_ID, dist = Dist.CLIENT)
public class AnvilCraftPigeonPlusClient {
    public AnvilCraftPigeonPlusClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(this::onRegisterAdditionalModels);
        modBus.addListener(this::onRegisterBER);
        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::onRegisterClientExtensions);
        modBus.addListener(this::onRegisterBlockColors);
        modBus.addListener(this::onRegisterItemColors);
        modBus.addListener(this::onRegisterParticleProviders);
    }

    private void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        ResourceLocation bottom = ResourceLocation.fromNamespaceAndPath(
            AnvilCraftPigeonPlus.MOD_ID, "block/blender_bottom");
        ResourceLocation top = ResourceLocation.fromNamespaceAndPath(
            AnvilCraftPigeonPlus.MOD_ID, "block/blender_top");
        ResourceLocation anvilPumpPiston = ResourceLocation.fromNamespaceAndPath(
            AnvilCraftPigeonPlus.MOD_ID, "block/anvil_pump_pistion");
        ResourceLocation largeCauldronTop = ResourceLocation.fromNamespaceAndPath(
            AnvilCraftPigeonPlus.MOD_ID, "block/large_cauldron_top");
        ResourceLocation largeCauldronBottom = ResourceLocation.fromNamespaceAndPath(
            AnvilCraftPigeonPlus.MOD_ID, "block/large_cauldron_bottom");
        event.register(new ModelResourceLocation(bottom, "standalone"));
        event.register(new ModelResourceLocation(top, "standalone"));
        event.register(new ModelResourceLocation(anvilPumpPiston, "standalone"));
        event.register(new ModelResourceLocation(largeCauldronTop, "standalone"));
        event.register(new ModelResourceLocation(largeCauldronBottom, "standalone"));
    }

    private void onRegisterBER(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.BLENDER.get(), BlenderBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ANVIL_PUMP.get(), AnvilPumpBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.STASIS_BEACON.get(), StasisBeaconBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.NOZZLE_EXHAUST.get(), NozzleExhaustBlockEntityRenderer::new);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            HudTooltipManager.INSTANCE.registerBlockEntityTooltip(new StasisBeaconTooltipProvider());
            ItemBlockRenderTypes.setRenderLayer(AddonFluids.LIQUEFIED_BIOGAS.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(AddonFluids.LIQUEFIED_BIOGAS_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(AddonFluids.LIQUID_OXYGEN.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(AddonFluids.LIQUID_OXYGEN_FLOWING.get(), RenderType.translucent());
        });
    }

    private void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                ResourceLocation.withDefaultNamespace("block/water_still"),
                ResourceLocation.withDefaultNamespace("block/water_flow"),
                0x6B8E3D,
                24.0f,
                0xFF6B8E3D,
                false
            ),
            AddonFluids.GASEOUS_BIOGAS_TYPE
        );
        event.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                ResourceLocation.withDefaultNamespace("block/water_still"),
                ResourceLocation.withDefaultNamespace("block/water_flow"),
                0xD9F2FF,
                48.0f,
                0x66D9F2FF,
                false
            ),
            AddonFluids.COMPRESSED_AIR_TYPE
        );
        event.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                ResourceLocation.withDefaultNamespace("block/water_still"),
                ResourceLocation.withDefaultNamespace("block/water_flow"),
                0x6E5F2C,
                20.0f,
                0xFF6E5F2C,
                false
            ),
            AddonFluids.MIXED_BIOMASS_TYPE
        );
        event.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                ResourceLocation.withDefaultNamespace("block/water_still"),
                ResourceLocation.withDefaultNamespace("block/water_flow"),
                0x8FD2B3,
                28.0f,
                0xD08FD2B3,
                false
            ),
            AddonFluids.LIQUEFIED_BIOGAS_TYPE
        );
        event.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                ResourceLocation.withDefaultNamespace("block/water_still"),
                ResourceLocation.withDefaultNamespace("block/water_flow"),
                0x87CEEB,
                8.0f,
                0x7087CEEB,
                false
            ),
            AddonFluids.LIQUID_OXYGEN_TYPE
        );
    }

    private void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        DynamicFluidContainerModel.Colors colors = new DynamicFluidContainerModel.Colors();
        event.register(
            colors,
            AddonItems.GASEOUS_BIOGAS_BUCKET.get(),
            AddonItems.COMPRESSED_AIR_BUCKET.get(),
            AddonItems.MIXED_BIOMASS_BUCKET.get()
        );
        event.register(
            (stack, tintIndex) -> {
                int color = colors.getColor(stack, tintIndex);
                return (color & 0x00FFFFFF) | 0xFF000000;
            },
            AddonItems.LIQUEFIED_BIOGAS_BUCKET.get(),
            AddonItems.LIQUID_OXYGEN_BUCKET.get()
        );
    }

    private void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
            (state, level, pos, tintIndex) -> tintIndex == 0 ? 0x6E5F2C : 0xFFFFFF,
            AddonBlocks.MIXED_BIOMASS_CAULDRON.get()
        );
    }

    private void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(AddonParticles.ROLLING_PLASMA.get(), RollingPlasmaParticle.Provider::new);
        event.registerSpriteSet(AddonParticles.ROLLING_METHANE_PLASMA.get(), RollingPlasmaParticle.MethaneProvider::new);
    }
}
