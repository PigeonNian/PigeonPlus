package dev.anvilcraft.pigeonplus.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.client.model.standalone.UnbakedStandaloneModel;

import java.util.List;

/**
 * 在方块实体渲染器里绘制「独立方块模型」的共用工具。
 *
 * <h3>为什么需要它</h3>
 * 26.1 的方块模型渲染被整体重构了：
 * <ul>
 *   <li>{@code BakedModel} / {@code ModelResourceLocation} / {@code BlockRenderDispatcher}
 *       全部移除，代码里再也拿不到「一个可以直接丢给渲染器的模型对象」。</li>
 *   <li>改为两级：先用 {@link StandaloneModelKey} 注册一个
 *       {@link UnbakedStandaloneModel}，烘焙后得到 {@link BlockStateModel}；
 *       渲染时把它展开成 {@link BlockStateModelPart} 列表，交给
 *       {@link SubmitNodeCollector#submitBlockModel} 提交。</li>
 *   <li>{@code render()} 本身也不存在了——方块实体渲染器改为
 *       {@code createRenderState()} + {@code extractRenderState()} + {@code submit()}。</li>
 * </ul>
 *
 * <p>这些步骤与具体方块无关，因此集中在这里，避免 3 个 BER 各写一份。
 *
 * <h3>用法</h3>
 * <pre>
 *   // 1) 定义 key（静态常量）
 *   private static final StandaloneModelKey&lt;BlockStateModel&gt; MY_MODEL =
 *       StandaloneBlockModels.key("block/my_model");
 *
 *   // 2) 在 ModelEvent.RegisterStandalone 里注册（客户端入口）
 *   StandaloneBlockModels.register(event, MY_MODEL);
 *
 *   // 3) 在 BER 的 submit() 里绘制
 *   StandaloneBlockModels.submit(poseStack, collector, MY_MODEL, lightCoords, overlayCoords);
 * </pre>
 */
public final class StandaloneBlockModels {
    /** 无染色层。这些模型不使用方块着色器染色。 */
    private static final int[] NO_TINTS = BlockModelRenderState.EMPTY_TINTS;

    /**
     * 复用的展开缓冲。
     *
     * <p>{@code submitBlockModel} 内部会复制一份，所以这里复用是安全的；
     * 渲染在客户端主线程单线程进行，不需要额外同步。
     */
    private static final List<BlockStateModelPart> PARTS = new ObjectArrayList<>();

    private StandaloneBlockModels() {
    }

    /**
     * 为某个模型 id 建一个独立模型的 key。
     *
     * <p>{@code ModelDebugName} 是只返回调试名的函数式接口，这里用模型路径当名字，
     * 便于崩溃报告里定位是哪个模型出的问题。
     */
    public static StandaloneModelKey<BlockStateModel> key(String path) {
        ModelDebugName debugName = () -> path;
        return new StandaloneModelKey<>(debugName);
    }

    /**
     * 把独立模型注册进烘焙流程。
     *
     * <p>必须在 {@code ModelEvent.RegisterStandalone} 里调用；若漏了，
     * {@link #submit} 取模型时会拿到 null。
     */
    public static void register(net.neoforged.neoforge.client.event.ModelEvent.RegisterStandalone event,
                                StandaloneModelKey<BlockStateModel> key,
                                Identifier modelId) {
        event.register(key, SimpleUnbakedStandaloneModel.blockStateModel(modelId));
    }

    /**
     * 绘制一个独立方块模型。
     *
     * <p>用的渲染层是 {@code cutoutBlockSheet()}：本模组的这些附加模型都带镂空
     * （活塞、桶、喷嘴零件），需要 cutout 才不会被渲染成实心黑块。
     */
    public static void submit(
        PoseStack poseStack,
        SubmitNodeCollector collector,
        StandaloneModelKey<BlockStateModel> key,
        int lightCoords,
        int overlayCoords
    ) {
        BlockStateModel model = Minecraft.getInstance().getModelManager().getStandaloneModel(key);
        if (model == null) {
            return;
        }
        PARTS.clear();
        model.collectParts(RandomSource.create(0L), PARTS);
        if (PARTS.isEmpty()) {
            return;
        }
        collector.submitBlockModel(
            poseStack,
            Sheets.cutoutBlockSheet(),
            new ObjectArrayList<>(PARTS),
            NO_TINTS,
            lightCoords,
            overlayCoords,
            0
        );
    }
}
