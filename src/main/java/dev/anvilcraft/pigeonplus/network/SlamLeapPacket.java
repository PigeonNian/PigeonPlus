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
 * 服务端告知客户端「普通裂地重拳已获准，开始向前跃起」。
 *
 * <p>方向与速度由服务端算好后下发，客户端只负责按给定初速位移，
 * 避免作弊客户端改数值。
 *
 * <p>指向性版本是另一个包（{@link SlamFlightPacket}）：两者位移方式完全不同
 * （抛物线初速 vs 朝固定点逼近），拆成两个包比塞进一个再分支更清楚，
 * 也避开了 {@code StreamCodec.composite} 最多 6 个字段的限制。
 *
 * @param dirX    水平单位方向 X
 * @param dirZ    水平单位方向 Z
 * @param speed   水平速度（格/tick）
 * @param upSpeed 垂直初速（格/tick）
 * @param ticks   手动接管的 tick 数
 */
public record SlamLeapPacket(double dirX, double dirZ, double speed, double upSpeed, int ticks)
    implements IClientboundPacket {

    public static final Type<SlamLeapPacket> TYPE =
        IPacket.type(AnvilCraftPigeonPlus.of("seismic_slam_leap"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlamLeapPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.DOUBLE, SlamLeapPacket::dirX,
            ByteBufCodecs.DOUBLE, SlamLeapPacket::dirZ,
            ByteBufCodecs.DOUBLE, SlamLeapPacket::speed,
            ByteBufCodecs.DOUBLE, SlamLeapPacket::upSpeed,
            ByteBufCodecs.VAR_INT, SlamLeapPacket::ticks,
            SlamLeapPacket::new
        );

    /**
     * 由水平方向与速度构造。
     */
    public static SlamLeapPacket of(Vec3 horizontal, double speed, double upSpeed, int ticks) {
        return new SlamLeapPacket(horizontal.x, horizontal.z, speed, upSpeed, ticks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleOnClient(Player player) {
        SeismicSlamClientState.startLeap(
            new Vec3(this.dirX, 0.0, this.dirZ).normalize(), this.speed, this.upSpeed, this.ticks
        );
    }
}
