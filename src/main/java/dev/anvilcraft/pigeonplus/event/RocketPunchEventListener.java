package dev.anvilcraft.pigeonplus.event;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.util.DoomfistEnchantmentUtil;
import dev.anvilcraft.pigeonplus.util.RocketPunchManager;
import dev.anvilcraft.pigeonplus.util.SkillCooldowns;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = AnvilCraftPigeonPlus.MOD_ID)
public class RocketPunchEventListener {
    /** 蓄力减速的修饰符 id，需固定以便反复增删。 */
    private static final ResourceLocation CHARGE_SLOW_ID =
        AnvilCraftPigeonPlus.of("rocket_punch_charge_slow");

    /**
     * 推进冲刺与击飞撞墙判定。放在 LevelTick 上以便按维度隔离状态。
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        RocketPunchManager.tick(event.getLevel());
        // 清理已过期的技能冷却记录，避免静态表无限增长
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            SkillCooldowns.prune(serverLevel);
        }
    }

    /**
     * 眩晕结束后恢复 AI，并在蓄力时施加减速。
     *
     * <p>{@code Mob#setNoAi} 是单向的，效果自然到期时不会自动复原，因此每 tick 校准一次。
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        RocketPunchManager.refreshStun(living);
        if (living instanceof Player player) {
            updateChargeSlow(player);
        }
    }

    /**
     * 提前松开右键：按已蓄力时长释放火箭重拳。
     *
     * <p>{@code AnvilHammerItem} 没有覆写 {@code releaseUsing}，因此无法用 mixin 注入该方法；
     * NeoForge 的 {@code Stop} 事件正好对应 {@code LivingEntity#releaseUsingItem}，
     * 而“蓄满自然完成”走的是 {@code finishUsingItem}（已在 mixin 中处理），两者不会重复触发。
     */
    @SubscribeEvent
    public static void onUseItemStop(LivingEntityUseItemEvent.Stop event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        if (!DoomfistEnchantmentUtil.hasDoomfist(stack)) return;
        int chargeTicks = RocketPunchManager.TOTAL_CHARGE_TICKS - event.getDuration();
        RocketPunchManager.performRocketPunch(player, chargeTicks);
    }

    /**
     * 玩家登出时清理其冷却记录。
     *
     * <p>冷却表是静态的，不清理会在反复进出服务器的过程中无限积累。
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SkillCooldowns.clear(event.getEntity().getUUID());
    }

    /**
     * 蓄力时施加 -50% 移动速度。
     *
     * <p>用瞬时修饰符而非 MobEffect，避免与其他减速效果互相覆盖，也便于精确移除。
     */
    private static void updateChargeSlow(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        boolean charging = player.isUsingItem()
            && DoomfistEnchantmentUtil.hasDoomfist(player.getUseItem());
        boolean hasModifier = speed.getModifier(CHARGE_SLOW_ID) != null;
        if (charging && !hasModifier) {
            speed.addTransientModifier(new AttributeModifier(
                CHARGE_SLOW_ID,
                -0.5,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));
        } else if (!charging && hasModifier) {
            speed.removeModifier(CHARGE_SLOW_ID);
        }
    }
}
