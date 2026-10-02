package dev.anvilcraft.pigeonplus.client.renderer.block;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/**
 * 大炼药锅喷口盖板的独立模型。
 *
 * <p>这两个模型由 {@code LargeCauldronBlockEntityRendererMixin} 使用——装喷口后
 * 锅的顶部/底部会多出一圈盖板。因为使用点在 mixin 里，key 单独放在这里，
 * 便于客户端入口统一注册、也让 mixin 能直接引用。
 */
public final class LargeCauldronAttachmentModels {
    /** 顶盖（水平或向下喷口时出现）。 */
    public static final StandaloneModelKey<BlockStateModel> TOP =
        StandaloneBlockModels.key("block/large_cauldron_top");
    /** 底盖（向下喷口时出现）。 */
    public static final StandaloneModelKey<BlockStateModel> BOTTOM =
        StandaloneBlockModels.key("block/large_cauldron_bottom");

    private LargeCauldronAttachmentModels() {
    }
}
