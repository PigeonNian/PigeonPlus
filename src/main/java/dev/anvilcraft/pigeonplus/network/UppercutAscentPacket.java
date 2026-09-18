package dev.anvilcraft.pigeonplus.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.client.UppercutClientState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 服务端告知客户端“上勾拳已生效，开始上升”。
 *
 * <p>与火箭重拳同理：玩家位置由客户端权威决定，上升位移必须在客户端执行，
 * 服务端只下发参数（上升速度与持续 tick）。
 *
 * @param upSpeed 每 tick 上升量（格/tick）
 * @param ticks   持续 tick 数
 */
public record UppercutAscentPacket(double upSpeed, int ticks) implements IClientboundPacket {
    public static final Type<UppercutAscentPacket> TYPE =
        IPacket.type(AnvilCraftPigeonPlus.of("uppercut_ascent"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UppercutAscentPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.DOUBLE, UppercutAscentPacket::upSpeed,
            ByteBufCodecs.VAR_INT, UppercutAscentPacket::ticks,
            UppercutAscentPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleOnClient(Player player) {
        UppercutClientState.startAscent(this.upSpeed, this.ticks);
    }
}
