package dev.anvilcraft.pigeonplus.mixin;

import dev.anvilcraft.pigeonplus.util.NozzleCauldronShapes;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 给大炼药锅的喷口盖板补<strong>实体碰撞</strong>。
 *
 * <h3>为什么必须注入父类</h3>
 * {@code getCollisionShape} 并<strong>未</strong>在 {@code LargeCauldronBlock} 中声明，
 * 它定义在父类 {@code AbstractMultiPartBlock} 上：
 * <pre>
 *   protected VoxelShape getCollisionShape(state, level, pos, context) {
 *       return hasCollision ? getPartShape(state) : Shapes.empty();
 *   }
 * </pre>
 * 若把 {@code @Inject(method = "getCollisionShape")} 写在 {@code LargeCauldronBlock} 的
 * mixin 里，Mixin 会因为该类自身没有这个方法而报
 * {@code Critical injection failure: could not find any targets}，
 * 导致<strong>游戏启动即崩溃</strong>（这个坑已经踩过一次）。
 *
 * <p>所以注入到父类，再用 {@code instanceof LargeCauldronBlock} 把影响范围限定回炼药锅——
 * 父类还被巨型激光、特斯拉塔、大型储罐等共用，不加限定会波及它们。
 *
 * <p>另注意：这个方法直接取 {@code getPartShape}，<strong>不经过 {@code getShape}</strong>，
 * 所以只改 {@code getShape} 时模型能渲染却撞不上去——这正是「额外模型没有碰撞体积」的成因。
 */
@Mixin(AbstractMultiPartBlock.class)
public class AbstractMultiPartBlockMixin {
    @Inject(method = "getCollisionShape", at = @At("RETURN"), cancellable = true)
    private void pigeonplus$addNozzleAttachmentCollision(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context,
        CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (!((Object) this instanceof LargeCauldronBlock cauldron)) {
            return;
        }
        if (!(level instanceof Level realLevel)) {
            return;
        }
        // HALF 是 LargeCauldronBlock 自己的属性
        if (!state.hasProperty(LargeCauldronBlock.HALF)) {
            return;
        }
        Cube3x3PartHalf part = state.getValue(LargeCauldronBlock.HALF);
        VoxelShape attachment = NozzleCauldronShapes.attachmentShape(
            realLevel,
            pos,
            cauldron.getMainPartPos(pos, state),
            part.getOffsetY(),
            part.name()
        );
        if (attachment.isEmpty()) {
            return;
        }
        // 直接与原有形状合并，**不能**因为原有形状为空就跳过。
        //
        // 曾经这里写过 `if (base.isEmpty()) return;`，本意是「方块没有碰撞时不要加」，
        // 但那会把「该部件本身就是空的」一并挡掉。而 TOP_CENTER 正属于后者：
        // 由 LargeCauldronBlock#createShapes 的字节码可见——
        //     offsetY == 0  ->  base = box(0,0,0, 16,8,16)   （底层有底板）
        //     offsetY != 0  ->  base = Shapes.empty()        （中/顶层无底板）
        // 之后只在 offsetX/offsetZ 为 ±1 时才补四面墙；TOP_CENTER 两者都是 0，
        // 因此它的原始碰撞就是空——那是锅的开口。
        // 现在装了喷口要给整个顶面加盖板，恰恰必须在这种「原本为空」的部件上补形状，
        // 否则锅盖中间会漏下去。Shapes.or 对空形状本来就能正常合并，无需特判。
        cir.setReturnValue(Shapes.or(cir.getReturnValue(), attachment));
    }
}
