package dev.anvilcraft.pigeonplus.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.client.SeismicSlamClientState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 服务端告知客户端「指向性裂地重拳已获准，飞向该落点」。
 *
 * <p>与 {@link SlamLeapPacket} 的区别：那个是给一个初速走抛物线，
 * 这个是给一个固定点、由客户端每 tick 朝它逼近。落点由服务端射线求出，
 * 客户端不参与选点。
 *
 * @param x 目标落点 X
 * @param y 目标落点 Y
 * @param z 目标落点 Z
 */
public record SlamFlightPacket(double x, double y, double z) implements IClientboundPacket {
    public static final Type<SlamFlightPacket> TYPE =
        IPacket.type(AnvilCraftPigeonPlus.of("seismic_slam_flight"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlamFlightPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.DOUBLE, SlamFlightPacket::x,
            ByteBufCodecs.DOUBLE, SlamFlightPacket::y,
            ByteBufCodecs.DOUBLE, SlamFlightPacket::z,
            SlamFlightPacket::new
        );

    public static SlamFlightPacket of(Vec3 landing) {
        return new SlamFlightPacket(landing.x, landing.y, landing.z);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleOnClient(Player player) {
        SeismicSlamClientState.startTargetedFlight(new Vec3(this.x, this.y, this.z));
    }
}
