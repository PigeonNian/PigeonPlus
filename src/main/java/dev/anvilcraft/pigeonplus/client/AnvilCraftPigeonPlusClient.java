package dev.anvilcraft.pigeonplus.client;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.block.entity.ModBlockEntities;
import dev.anvilcraft.pigeonplus.client.renderer.block.BlenderBlockEntityRenderer;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.anvilcraft.pigeonplus.init.AddonItems;
import dev.dubhe.anvilcraft.util.ModClientFluidTypeExtensionImpl;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;

@Mod(value = AnvilCraftPigeonPlus.MOD_ID, dist = Dist.CLIENT)
public class AnvilCraftPigeonPlusClient {
    public AnvilCraftPigeonPlusClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(this::onRegisterAdditionalModels);
        modBus.addListener(this::onRegisterBER);
        modBus.addListener(this::onRegisterClientExtensions);
        modBus.addListener(this::onRegisterItemColors);
    }

    private void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        ResourceLocation bottom = ResourceLocation.fromNamespaceAndPath(
            AnvilCraftPigeonPlus.MOD_ID, "block/blender_bottom");
        ResourceLocation top = ResourceLocation.fromNamespaceAndPath(
            AnvilCraftPigeonPlus.MOD_ID, "block/blender_top");
        event.register(new ModelResourceLocation(bottom, "standalone"));
        event.register(new ModelResourceLocation(top, "standalone"));
    }

    private void onRegisterBER(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.BLENDER.get(), BlenderBlockEntityRenderer::new);
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
    }

    private void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        DynamicFluidContainerModel.Colors colors = new DynamicFluidContainerModel.Colors();
        event.register(
            colors,
            AddonItems.GASEOUS_BIOGAS_BUCKET.get(),
            AddonItems.COMPRESSED_AIR_BUCKET.get(),
            AddonItems.MIXED_BIOMASS_BUCKET.get()
        );
    }
}
