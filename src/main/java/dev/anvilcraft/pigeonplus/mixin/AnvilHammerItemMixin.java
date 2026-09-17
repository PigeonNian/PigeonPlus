package dev.anvilcraft.pigeonplus.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.pigeonplus.util.DoomfistEnchantmentUtil;
import dev.dubhe.anvilcraft.api.hammer.HammerManager;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 铁拳附魔：让铁砧锤失去“扳手”类功能。
 *
 * <p>被附魔的铁砧锤不再能旋转方块、拆除方块、开启便携铁砧、火箭跳，也不再作为护目镜。
 * 只保留作为武器的近战能力（{@code hurtEnemy} 的铁砧猛击未作改动）。
 *
 * <p>这里刻意不去整体取消 {@code useBlock}，因为该方法同时还承担着“右键打开容器/方块交互”的
 * 普通功能；逐个掐掉扳手分支可以避免误伤常规交互。
 */
@Mixin(AnvilHammerItem.class)
public class AnvilHammerItemMixin {

    /**
     * 左键：不再落下铁砧。返回 false 会使 AnvilCraft 的
     * {@code BlockEventListener#anvilHammerAttack} 取消该次左键方块事件。
     *
     * <p>这里只看主手——{@code dropAnvil} 本身就只对主手物品生效。
     */
    @Inject(method = "dropAnvil", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$stripDropAnvil(
        Player player, Level level, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir
    ) {
        if (player != null && DoomfistEnchantmentUtil.hasDoomfist(player.getMainHandItem())) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 潜行右键：不再拆除方块。
     */
    @Inject(method = "breakBlock", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$stripBreakBlock(
        ServerPlayer player, BlockPos pos, ServerLevel level, ItemStack tool, CallbackInfo ci
    ) {
        if (DoomfistEnchantmentUtil.hasDoomfist(tool)) {
            ci.cancel();
        }
    }

    /**
     * 不再允许旋转方块。
     *
     * <p>客户端 {@code ClientBlockEventListener#clientHandle} 依赖该方法决定是否弹出旋转界面，
     * 返回 false 后连界面都不会出现，服务端也就收不到旋转/拆除用的 {@code HammerUsePacket}。
     */
    @Inject(method = "ableToUseAnvilHammer", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$stripRotate(
        Level level, BlockPos blockPos, Player player, CallbackInfoReturnable<Boolean> cir
    ) {
        if (DoomfistEnchantmentUtil.isWieldingDoomfist(player)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 长按右键：不再打开便携铁砧。
     *
     * <p>同时把 {@code use} 拦成 PASS，使长按根本不会开始，避免客户端继续绘制那个注定无效的
     * 便携铁砧进度条（见 {@code AnvilHammerUseHUD}）。
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$stripPortableAnvilStart(
        Level level, Player player, InteractionHand usedHand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir
    ) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (DoomfistEnchantmentUtil.hasDoomfist(stack)) {
            cir.setReturnValue(InteractionResultHolder.pass(stack));
        }
    }

    /**
     * 长按右键：不再打开便携铁砧（服务端兜底）。
     */
    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void pigeonplus$stripPortableAnvil(
        ItemStack stack, Level level, LivingEntity livingEntity, CallbackInfoReturnable<ItemStack> cir
    ) {
        if (DoomfistEnchantmentUtil.hasDoomfist(stack)) {
            cir.setReturnValue(stack);
        }
    }

    /**
     * 火箭跳：不再生效。
     */
    @Inject(method = "canRocketJump", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$stripRocketJump(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (DoomfistEnchantmentUtil.isWieldingDoomfist(player)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 戴在头上时不再视为护目镜。
     */
    @Inject(method = "isWearing", at = @At("HEAD"), cancellable = true)
    private static void pigeonplus$stripGoggles(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (DoomfistEnchantmentUtil.hasDoomfist(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD))) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 不再允许旋转方块。
     *
     * <p>{@code useBlock} 末尾会调用 {@code HammerManager#getChange(...).change(...)} 来旋转方块。
     * 这里把取到的行为替换成空实现，从而只掐掉旋转，保留同一方法内“右键打开容器/方块交互”的能力。
     * 用 {@code EMPTY} 而非直接取消 {@code useBlock}，是为了不误伤常规交互。
     *
     * <p>通过 MixinExtras 的 {@code @Local(argsOnly = true)} 取回本次调用的铁砧锤堆栈，
     * 避免用静态字段暂存状态。
     */
    @Redirect(
        method = "useBlock",
        at = @At(
            value = "INVOKE",
            target = "Ldev/dubhe/anvilcraft/api/hammer/HammerManager;getChange(Lnet/minecraft/world/level/block/Block;)Ldev/dubhe/anvilcraft/api/hammer/IHammerChangeable;"
        )
    )
    private static IHammerChangeable pigeonplus$stripRotationBehavior(
        Block block, @Local(argsOnly = true) ItemStack anvilHammer
    ) {
        if (DoomfistEnchantmentUtil.hasDoomfist(anvilHammer)) return HammerRotateBehavior.EMPTY;
        return HammerManager.getChange(block);
    }
}
