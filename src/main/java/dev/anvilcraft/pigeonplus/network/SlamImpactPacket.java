package dev.anvilcraft.pigeonplus.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.util.SlamManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 客户端告知服务端「已落地，结算裂地重拳」。
 *
 * <p>落地时机只有客户端知道（玩家位置客户端权威），因此由客户端在检测到触地时上报，
 * 服务端再按<strong>它自己记录的</strong>起跳时刻计算滞空时长与伤害——不采信客户端上报的数值，
 * 避免被伪造。
 */
public record SlamImpactPacket() implements IServerboundPacket {
    public static final Type<SlamImpactPacket> TYPE =
        IPacket.type(AnvilCraftPigeonPlus.of("seismic_slam_impact"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlamImpactPacket> STREAM_CODEC =
        StreamCodec.unit(new SlamImpactPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            SlamManager.impact(serverPlayer);
        }
    }
}
