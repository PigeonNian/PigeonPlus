package dev.anvilcraft.pigeonplus.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.util.UppercutManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 客户端请求释放上勾拳。
 *
 * <p>触发判定必须在客户端做（读取 Shift 按键），但伤害与击飞由服务端权威结算，
 * 因此客户端只负责“请求”，服务端再校验冷却、计算命中并回发结果。
 */
public record UppercutRequestPacket() implements IServerboundPacket {
    public static final Type<UppercutRequestPacket> TYPE =
        IPacket.type(AnvilCraftPigeonPlus.of("uppercut_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UppercutRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new UppercutRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            UppercutManager.performUppercut(serverPlayer);
        }
    }
}
