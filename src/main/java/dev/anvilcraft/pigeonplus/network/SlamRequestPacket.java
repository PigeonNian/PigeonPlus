package dev.anvilcraft.pigeonplus.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.util.SlamManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 客户端请求释放裂地重拳。
 *
 * <p>触发判定（E 键）在客户端，但伤害、冷却与是否允许释放由服务端权威决定。
 *
 * @param targeted 客户端是否请求「指向性」版本（悬空时）。
 *                 这只是<strong>意图</strong>：服务端会自行复核悬空条件与落点，
 *                 不满足时回退为普通裂地。
 */
public record SlamRequestPacket(boolean targeted) implements IServerboundPacket {
    public static final Type<SlamRequestPacket> TYPE =
        IPacket.type(AnvilCraftPigeonPlus.of("seismic_slam_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlamRequestPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BOOL, SlamRequestPacket::targeted,
            SlamRequestPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        // 先尝试指向性；服务端会自己复核，不满足则回退普通裂地
        if (this.targeted && SlamManager.performTargetedSlam(serverPlayer)) return;
        SlamManager.performSlam(serverPlayer);
    }
}
