package dev.anvilcraft.pigeonplus.mixin.client;

import dev.anvilcraft.pigeonplus.fluid.GasFluid;
import dev.dubhe.anvilcraft.client.renderer.blockentity.BaseFluidHandlerHolderRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.FluidHandlerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让气体把容器「充满」，而不是只占底部一小截。
 *
 * <h3>为什么能合并三个 mixin</h3>
 * 1.21.1 时，流体箱、大流体罐、鱼缸各有自己的渲染器与
 * {@code drawFluidInTank} 方法，所以需要三个 mixin 分别拦截，
 * 还要额外声明各自的箱壁厚度与坐标。
 *
 * <p>26.1 起 AnvilCraft 把这些渲染器统一到
 * {@link BaseFluidHandlerHolderRenderer}：液面比例由基类在
 * {@code extractRenderState} 里算好（写入 {@code state.setFill}），
 * 各子类只是覆写箱壁尺寸。于是「气体充满」这件事只需在<b>基类</b>
 * 改一个数字即可，箱壁与坐标全部沿用 AnvilCraft 自己的计算，
 * 我们不再需要知道任何尺寸常量。
 *
 * <h3>为什么只改 fill 就够了</h3>
 * <ul>
 *   <li><b>液面</b>：{@code submit} 用 {@code getFill()} 插值出液面高度，
 *       fill=1 即满箱。</li>
 *   <li><b>漂浮物沉底</b>：鱼缸的 {@code submit} 里有
 *       {@code translate(0, (maxY-minY)*(fill-1), 0)}——fill=1 时位移恰好为 0，
 *       物品自然落在底部。原 FishTank mixin 专门拦截这个参数，
 *       现在由这条公式天然满足。</li>
 * </ul>
 *
 * <p>注入基类方法的 RETURN：此时 AnvilCraft 已设好 resource 与 fill，
 * 我们只在其上做覆盖。子类（鱼缸/大流体罐）都调用 {@code super.extractRenderState}，
 * 因此一处生效、全部覆盖。
 */
@Mixin(BaseFluidHandlerHolderRenderer.class)
public class GasTankFillMixin {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void pigeonplus$gasFillsTank(
        BlockEntity blockEntity,
        FluidHandlerRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.CrumblingOverlay breakProgress,
        CallbackInfo ci
    ) {
        FluidResource resource = state.getResource();
        if (resource == null || resource.isEmpty()) {
            return;
        }
        // 气体扩散到整个容器，视觉上即为满箱
        if (resource.getFluid() instanceof GasFluid) {
            state.setFill(1.0F);
        }
    }
}
