package dev.anvilcraft.pigeonplus.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.util.RocketPunchManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 客户端告知服务端：本次冲刺已被跳跃取消。
 *
 * <p>为什么必须发这个包：位移在客户端，跳跃取消自然也发生在客户端。
 * 若不同步，服务端的 {@code ACTIVE_DASHES} 会继续倒计时并做命中判定——
 * 玩家明明已经停下，却仍会把附近经过的生物打飞。
 */
public record RocketPunchCancelPacket() implements IServerboundPacket {
    public static final Type<RocketPunchCancelPacket> TYPE =
        IPacket.type(AnvilCraftPigeonPlus.of("rocket_punch_cancel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RocketPunchCancelPacket> STREAM_CODEC =
        StreamCodec.unit(new RocketPunchCancelPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            RocketPunchManager.abortDash(serverPlayer);
        }
    }
}
