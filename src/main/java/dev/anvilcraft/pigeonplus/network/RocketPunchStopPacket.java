package dev.anvilcraft.pigeonplus.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.client.RocketPunchClientState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 通知客户端立即中断火箭重拳冲刺。
 *
 * <p>用于“撞到生物后立刻停下”：服务端在命中判定成立的那一 tick 发出本包，
 * 客户端收到后清掉自己的位移状态。必须显式通知——位移由客户端执行，
 * 服务端单方面移除自己的状态并不会让客户端停止移动。
 */
public record RocketPunchStopPacket() implements IClientboundPacket {
    public static final Type<RocketPunchStopPacket> TYPE =
        IPacket.type(AnvilCraftPigeonPlus.of("rocket_punch_stop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RocketPunchStopPacket> STREAM_CODEC =
        StreamCodec.unit(new RocketPunchStopPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleOnClient(Player player) {
        RocketPunchClientState.stopDash();
    }
}
