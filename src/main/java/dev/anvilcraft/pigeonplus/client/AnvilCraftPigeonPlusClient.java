package dev.anvilcraft.pigeonplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.pigeonplus.AddonClientConfig;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.pigeonplus.block.entity.ModBlockEntities;
import dev.anvilcraft.pigeonplus.client.hud.RocketPunchChargeHud;
import dev.anvilcraft.pigeonplus.client.hud.SkillCooldownHud;
import dev.anvilcraft.pigeonplus.client.hud.SlamDamageHud;
import dev.anvilcraft.pigeonplus.client.render.RocketPunchHandAnimation;
import dev.anvilcraft.pigeonplus.client.render.SlamHandAnimation;
import dev.anvilcraft.pigeonplus.client.render.SlamIndicatorRenderer;
import dev.anvilcraft.pigeonplus.client.render.UppercutHandAnimation;
import dev.anvilcraft.pigeonplus.client.particle.RollingPlasmaParticle;
import dev.anvilcraft.pigeonplus.client.renderer.block.StandaloneBlockModels;
import dev.anvilcraft.pigeonplus.client.renderer.block.LargeCauldronAttachmentModels;
import dev.anvilcraft.pigeonplus.client.renderer.block.FeedSpreaderBlockEntityRenderer;
import dev.anvilcraft.pigeonplus.client.renderer.block.AnvilPumpBlockEntityRenderer;
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
import dev.anvilcraft.pigeonplus.client.renderer.block.BlenderBlockEntityRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
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
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.client.color.block.BlockTintSource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import dev.dubhe.anvilcraft.api.tooltip.HudTooltipManager;

import java.util.List;

@Mod(value = AnvilCraftPigeonPlus.MOD_ID, dist = Dist.CLIENT)
public class AnvilCraftPigeonPlusClient {
    public static final AddonClientConfig CLIENT_CONFIG = ConfigManager.register(AnvilCraftPigeonPlus.MOD_ID, AddonClientConfig::new);

    public AnvilCraftPigeonPlusClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(this::onRegisterStandaloneModels);
        modBus.addListener(this::onRegisterBER);
        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::onRegisterClientExtensions);
        modBus.addListener(this::onRegisterBlockColors);
        modBus.addListener(this::onRegisterItemColors);
        modBus.addListener(this::onRegisterParticleProviders);
        modBus.addListener(this::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(this::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onClientTickPre);
        NeoForge.EVENT_BUS.addListener(this::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(this::onRenderHand);
        NeoForge.EVENT_BUS.addListener(this::onLoggingOut);
    }

    /**
     * 客户端 tick 前置：处理需要赶在 {@code handleKeybinds} 之前完成的按键拦截。
     *
     * <p>{@code Minecraft#tick()} 的顺序是
     * {@code ClientHooks.fireClientTickPre()} → {@code handleKeybinds()}，
     * 所以在这里消费 E 键能有效阻止原版打开物品栏。
     */
    private void onClientTickPre(ClientTickEvent.Pre event) {
        SlamKeyHandler.handleInventoryKey();
    }

    /**
     * 在世界中绘制指向性裂地重拳的地面扇形指示器。
     *
     * <p>26.1 起 {@code RenderLevelStageEvent} 由「一个事件 + {@code Stage} 枚举」
     * 改为「每个阶段一个事件子类」，因此不再需要 {@code getStage()} 判断，
     * 直接把监听器参数声明成 {@link RenderLevelStageEvent.AfterLevel} 即可，
     * 且此时世界已画完、深度缓冲可用，画在贴地位置才不会被地形遮挡。
     *
     * <p>相机位置也换了来源：原 {@code getCamera()} 已移除，
     * 改为从 {@code getLevelRenderState().cameraRenderState.pos} 取。
     */
    private void onRenderLevelStage(RenderLevelStageEvent.AfterLevel event) {
        SlamIndicatorRenderer.render(
            event.getPoseStack(),
            event.getModelViewMatrix(),
            event.getLevelRenderState().cameraRenderState.pos
        );
    }

    /**
     * 第一人称手部动画。
     *
     * <p>该事件在绘制手持物<strong>之前</strong>触发且带 PoseStack，
     * 所以在这里改位姿即可作用于随后绘制的手。
     *
     * <p>{@code equipProgress} 只透传给上勾拳：它用枢轴补偿（绕握把转），
     * 需要该值还原原版的手部基准平移；裂地重拳与火箭重拳的角度是按旧枢轴调定的，
     * 换枢轴会让那些数值的观感全变，故不传。
     *
     * <p>三个技能各管一段，靠技能互斥（{@code SkillGate}）保证不会同时生效。
     */
    private void onRenderHand(RenderHandEvent event) {
        float partialTick = event.getPartialTick();
        PoseStack poseStack = event.getPoseStack();
        InteractionHand hand = event.getHand();

        SlamHandAnimation.onRenderHand(poseStack, hand, partialTick);
        RocketPunchHandAnimation.onRenderHand(poseStack, hand, partialTick);
        UppercutHandAnimation.onRenderHand(poseStack, hand, partialTick, event.getEquipProgress());
    }

    /**
     * 断开连接时清空技能冷却镜像。
     *
     * <p>冷却表是静态的，不清会在切换到另一个服务器后残留上一个服务器的冷却时间。
     */
    private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SkillCooldowns.clearClient();
        // 命中/挥击动作是静态计时，不清会在切换世界后残留、闪一下
        RocketPunchAnimState.reset();
        UppercutClientState.reset();
    }

    private void onClientTick(ClientTickEvent.Post event) {
        NozzleSoundController.clientTick();
        RocketPunchClientState.clientTick();
        // 递减命中动作计时（命中是一瞬间的事件，需要自己记时长才能播完）
        RocketPunchAnimState.clientTick();
        UppercutClientState.clientTick();
        // 递减统一的技能冷却镜像（HUD 读它，与服务端放行判断同源）
        SkillCooldowns.clientTick();
        // 校准蓄力音效实例（自然播完 / 换维度后清理引用）
        RocketPunchChargeSoundController.clientTick();
        // 裂地重拳：检测落地并请求结算
        SeismicSlamClientState.clientTick();
        // 递减下砸动作计时（结算后仍要继续播完，故独立于 clientTick）
        SeismicSlamClientState.tickSlamSwing();
    }

    private void onItemTooltip(ItemTooltipEvent event) {
        AddonItemTooltipManager.addTooltip(event.getItemStack(), event.getToolTip());
    }

    /**
     * 注册 BER 用到的「独立方块模型」。
     *
     * <p>26.1 把 {@code ModelEvent.RegisterAdditional} 换成了
     * {@link ModelEvent.RegisterStandalone}，且 API 完全不同：
     * 旧写法是 {@code event.register(new ModelResourceLocation(id, "standalone"))}，
     * 现在是「{@code StandaloneModelKey} + {@code UnbakedStandaloneModel}」配对注册，
     * 烘焙后用 {@code ModelManager.getStandaloneModel(key)} 取回。
     *
     * <p>因此每个模型都需要一个稳定的 key 常量，它们定义在各自的 BER 里，
     * 这里统一注册，避免注册点与使用点分散在两处而对不上。
     */
    private void onRegisterStandaloneModels(ModelEvent.RegisterStandalone event) {
        StandaloneBlockModels.register(event, AnvilPumpBlockEntityRenderer.PISTON,
            pigeonplus$id("block/anvil_pump_pistion"));
        StandaloneBlockModels.register(event, FeedSpreaderBlockEntityRenderer.BUCKET_MODEL,
            pigeonplus$id("block/feed_spreader_bucket"));
        StandaloneBlockModels.register(event, FeedSpreaderBlockEntityRenderer.PISTON_MODEL,
            pigeonplus$id("block/feed_spreader_piston"));
        StandaloneBlockModels.register(event, BlenderBlockEntityRenderer.TOP_MODEL,
            pigeonplus$id("block/blender_top"));
        StandaloneBlockModels.register(event, LargeCauldronAttachmentModels.TOP,
            pigeonplus$id("block/large_cauldron_top"));
        StandaloneBlockModels.register(event, LargeCauldronAttachmentModels.BOTTOM,
            pigeonplus$id("block/large_cauldron_bottom"));
    }

    /** 本模组的资源 id。 */
    private static Identifier pigeonplus$id(String path) {
        return Identifier.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, path);
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
            // 26.1 起流体的贴图与渲染层改为**数据驱动**，代码里不再需要设置：
            // ItemBlockRenderTypes 与 RenderType.translucent() 均已移除，
            // IClientFluidTypeExtensions 也只保留 overlay 与 fog 相关方法。
            // 流体外观现由资源文件决定（assets/<modid>/ 下的流体模型/材质定义）。
        });
    }

    private void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                0x6B8E3D,
                24.0f
            ),
            AddonFluids.GASEOUS_BIOGAS_TYPE
        );
        event.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                0xD9F2FF,
                48.0f
            ),
            AddonFluids.COMPRESSED_AIR_TYPE
        );
        event.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                0x6E5F2C,
                20.0f
            ),
            AddonFluids.MIXED_BIOMASS_TYPE
        );
        event.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                0x8FD2B3,
                28.0f
            ),
            AddonFluids.LIQUEFIED_BIOGAS_TYPE
        );
        event.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                0x87CEEB,
                8.0f
            ),
            AddonFluids.LIQUID_OXYGEN_TYPE
        );
        event.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                0xB8E2F4,
                8.0f
            ),
            AddonFluids.LIQUID_HYDROGEN_TYPE
        );
    }

    /**
     * 方块染色。
     *
     * <p>26.1 把 {@code RegisterColorHandlersEvent.Block} 换成了
     * {@link RegisterColorHandlersEvent.BlockTintSources}：不再是「注册一个
     * {@code BlockColor} 函数」，而是要提供 {@link BlockTintSource} 的**列表**
     * （每个 tintIndex 一个来源），与原版红石线等方块的写法一致。
     *
     * <p>本模组只有混合生物质炼药锅需要染色：tintIndex 0 是液面，其余部件用白色
     * （即不改变原贴图颜色）。
     */
    private void onRegisterBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(
            List.<BlockTintSource>of(state -> 0x6E5F2C),
            AddonBlocks.MIXED_BIOMASS_CAULDRON.get()
        );
    }

    /**
     * 物品染色。
     *
     * <p>原先这里用 {@code DynamicFluidContainerModel.Colors} 给流体桶染色，
     * 但该类在 26.1 已被重写为完整的 {@code ItemModel}
     * （{@code net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel}），
     * 不再暴露 {@code Colors}；流体的贴图与颜色改为**数据驱动**，
     * 由桶的模型定义里声明的流体自行决定。
     *
     * <p>因此这里不再需要注册任何物品染色来源，方法保留为空壳仅作说明。
     * 若将来确有自定义物品染色需求，应改为实现
     * {@code ItemTintSource} 并通过
     * {@link RegisterColorHandlersEvent.ItemTintSources} 注册
     * （需要同时提供 {@code MapCodec}，因为 26.1 的染色来源是可在模型里引用的类型）。
     */
    private void onRegisterItemColors(RegisterColorHandlersEvent.ItemTintSources event) {
        // 无需注册：流体桶的染色由数据驱动
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
            Identifier.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "rocket_punch_charge"),
            RocketPunchChargeHud::render
        );
        event.registerAboveAll(
            Identifier.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "skill_cooldown"),
            SkillCooldownHud::render
        );
        event.registerAboveAll(
            Identifier.fromNamespaceAndPath(AnvilCraftPigeonPlus.MOD_ID, "slam_damage"),
            SlamDamageHud::render
        );
        try {
            event.wrapLayer(
                Identifier.fromNamespaceAndPath("anvilcraft", "anvil_hammer_use"),
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
