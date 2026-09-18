package dev.anvilcraft.pigeonplus.client;

import dev.anvilcraft.pigeonplus.AddonClientConfig;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.pigeonplus.block.entity.ModBlockEntities;
import dev.anvilcraft.pigeonplus.client.hud.RocketPunchChargeHud;
import dev.anvilcraft.pigeonplus.client.hud.SkillCooldownHud;
import dev.anvilcraft.pigeonplus.client.particle.RollingPlasmaParticle;
import dev.anvilcraft.pigeonplus.client.renderer.block.AnvilPumpBlockEntityRenderer;
import dev.anvilcraft.pigeonplus.client.renderer.block.BlenderBlockEntityRenderer;
import dev.anvilcraft.pigeonplus.client.renderer.block.FeedSpreaderBlockEntityRenderer;
import dev.anvilcraft.pigeonplus.client.renderer.block.NozzleExhaustBlockEntityRenderer;
import dev.anvilcraft.pigeonplus.client.renderer.block.StasisBeaconBlockEntityRenderer;
import dev.anvilcraft.pigeonplus.client.sound.NozzleSoundController;
import dev.anvilcraft.pigeonplus.client.sound.RocketPunchChargeSoundController;
import dev.anvilcraft.pigeonplus.client.tooltip.AddonItemTooltipManager;
import dev.anvilcraft.pigeonplus.client.tooltip.StasisBeaconTooltipProvider;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.anvilcraft.pigeonplus.init.AddonItems;
import dev.anvilcraft.pigeonplus.init.AddonParticles;
import dev.anvilcraft.pigeonplus.util.DoomfistEnchantmentUtil;
import dev.anvilcraft.pigeonplus.util.SkillCooldowns;
import dev.dubhe.anvilcraft.util.ModClientFluidTypeExtensionImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import dev.dubhe.anvilcraft.api.tooltip.HudTooltipManager;

@Mod(value = AnvilCraftPigeonPlus.MOD_ID, dist = Dist.CLIENT)
public class AnvilCraftPigeonPlusClient {
    public static final AddonClientConfig CLIENT_CONFIG = ConfigManager.register(AnvilCraftPigeonPlus.MOD_ID, AddonClientConfig::new);

    public AnvilCraftPigeonPlusClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(this::onRegisterAdditionalModels);
        modBus.addListener(this::onRegisterBER);
        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::onRegisterClientExtensions);
        modBus.addListener(this::onRegisterBlockColors);
        modBus.addListener(this::onRegisterItemColors);
        modBus.addListener(this::onRegisterParticleProviders);
        modBus.addListener(this::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(this::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onLoggingOut);
    }

    /**
     * 断开连接时清空技能冷却镜像。
     *
     * <p>冷却表是静态的，不清会在切换到另一个服务器后残留上一个服务器的冷却时间。
     */
    private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SkillCooldowns.clearClient();
    }

    private void onClientTick(ClientTickEvent.Post event) {
        NozzleSoundController.clientTick();
        RocketPunchClientState.clientTick();
        UppercutClientState.clientTick();
        // 递减统一的技能冷却镜像（HUD 读它，与服务端放行判断同源）
        SkillCooldowns.clientTick();
        // 校准蓄力音效实例（自然播完 / 换维度后清理引用）
        RocketPunchChargeSoundController.clientTick();
    }

    private void onItemTooltip(ItemTooltipEvent event) {
        AddonItemTooltipManager.addTooltip(event.getItemStack(), event.getToolTip());
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
        ResourceLocation feedSpreaderBucket = ResourceLocation.fromNamespaceAndPath(
            AnvilCraftPigeonPlus.MOD_ID, "block/feed_spreader_bucket");
        ResourceLocation feedSpreaderPiston = ResourceLocation.fromNamespaceAndPath(
            AnvilCraftPigeonPlus.MOD_ID, "block/feed_spreader_piston");
        event.register(new ModelResourceLocation(bottom, "standalone"));
        event.register(new ModelResourceLocation(top, "standalone"));
        event.register(new ModelResourceLocation(anvilPumpPiston, "standalone"));
        event.register(new ModelResourceLocation(largeCauldronTop, "standalone"));
        event.register(new ModelResourceLocation(largeCauldronBottom, "standalone"));
        event.register(new ModelResourceLocation(feedSpreaderBucket, "standalone"));
        event.register(new ModelResourceLocation(feedSpreaderPiston, "standalone"));
    }

    private void onRegisterBER(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.BLENDER.get(), BlenderBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ANVIL_PUMP.get(), AnvilPumpBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FEED_SPREADER.get(), FeedSpreaderBlockEntityRenderer::new);
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
            ItemBlockRenderTypes.setRenderLayer(AddonFluids.LIQUID_HYDROGEN.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(AddonFluids.LIQUID_HYDROGEN_FLOWING.get(), RenderType.translucent());
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
        event.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                ResourceLocation.withDefaultNamespace("block/water_still"),
                ResourceLocation.withDefaultNamespace("block/water_flow"),
                0xB8E2F4,
                8.0f,
                0x70B8E2F4,
                false
            ),
            AddonFluids.LIQUID_HYDROGEN_TYPE
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
        event.registerSpriteSet(AddonParticles.ROLLING_HYDROGEN_PLASMA.get(), RollingPlasmaParticle.HydrogenProvider::new);
    }

    /**
     * 注册火箭重拳的界面层。
     *
     * <p>同时抑制 AnvilCraft 的 {@code anvil_hammer_use} 层：那个 HUD 会在任何铁砧锤
     * “正在使用中”时画准心进度条，而铁拳附魔把右键改成了蓄力，于是它会以
     * {@code PORTABLE_ANVIL_USE_TICKS}(40) 为分母画一条与蓄力（32 tick）不同步的进度条。
     *
     * <p>这里用 {@code wrapLayer} 而不是 {@code replaceLayer}：NeoForge 的
     * {@code wrapLayer} 会拿到原层，我们才能在“非铁拳”情况下继续调用原实现，
     * 保持其他铁砧锤的便携铁砧进度条不变。
     *
     * <p>注意 {@code wrapLayer} 在目标层不存在时会抛 {@code IllegalArgumentException}，
     * 而模组监听器的执行顺序并不保证 AnvilCraft 先注册，因此这里包一层 try/catch：
     * 万一顺序不利就放弃抑制，只失去“隐藏原进度条”这一项，不至于让客户端崩溃。
     */
    private void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
            ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "rocket_punch_charge"),
            RocketPunchChargeHud::render
        );
        event.registerAboveAll(
            ResourceLocation.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "skill_cooldown"),
            SkillCooldownHud::render
        );
        try {
            event.wrapLayer(
                ResourceLocation.fromNamespaceAndPath("anvilcraft", "anvil_hammer_use"),
                original -> (graphics, deltaTracker) -> {
                    // 手持带铁拳附魔的铁砧锤蓄力时，改由 RocketPunchChargeHud 绘制
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.player != null
                        && minecraft.player.isUsingItem()
                        && DoomfistEnchantmentUtil.hasDoomfist(minecraft.player.getUseItem())) {
                        return;
                    }
                    original.render(graphics, deltaTracker);
                }
            );
        } catch (IllegalArgumentException ignored) {
            // AnvilCraft 的层尚未注册；放弃抑制，优先保证客户端不崩
        }
    }
}
