package dev.anvilcraft.pigeonplus.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.client.sound.RocketPunchChargeSoundController;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 控制火箭重拳的蓄力音效：开始播放 / 立即停止。
 *
 * <p>为什么需要「停止」这条消息：蓄力音效要在冲刺开始的瞬间被掐断，
 * 否则冲刺音效会和还在响的蓄力音叠在一起。声音实例由客户端持有，
 * 服务端只需告诉它什么时候开始、什么时候停。
 *
 * @param playing true = 开始蓄力音效，false = 立即停止
 */
public record RocketPunchChargeSoundPacket(boolean playing) implements IClientboundPacket {
    public static final Type<RocketPunchChargeSoundPacket> TYPE =
        IPacket.type(AnvilCraftPigeonPlus.of("rocket_punch_charge_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RocketPunchChargeSoundPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BOOL, RocketPunchChargeSoundPacket::playing,
            RocketPunchChargeSoundPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleOnClient(Player player) {
        if (this.playing) {
            RocketPunchChargeSoundController.start();
        } else {
            RocketPunchChargeSoundController.stop();
        }
    }
}
