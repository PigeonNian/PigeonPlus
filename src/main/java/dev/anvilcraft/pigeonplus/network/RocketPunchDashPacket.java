package dev.anvilcraft.pigeonplus.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.client.RocketPunchClientState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 通知客户端“已进入火箭重拳冲刺”。
 *
 * <p>为什么位移要交给客户端：玩家位置与朝向由<strong>客户端权威</strong>决定并每 tick 上报。
 * 服务端单方面位移会被客户端下一个位置包覆盖（表现为回弹），而且 45 米/秒 远超服务端的
 * “moved too quickly”阈值会被拉回。AnvilCraft 自己的火箭跳也是这个套路——
 * 服务端只发包，客户端应用冲量。
 *
 * <p>方向与速度由服务端算好后下发，客户端只负责按固定向量匀速位移，避免作弊客户端改数值。
 *
 * @param dirX   水平单位方向 X
 * @param dirZ   水平单位方向 Z
 * @param speed  每 tick 位移量（米）
 * @param ticks  持续 tick 数
 * @param baseYaw 释放瞬间朝向，视角锁定以此为基准
 */
public record RocketPunchDashPacket(double dirX, double dirZ, double speed, int ticks, float baseYaw)
    implements IClientboundPacket {

    public static final Type<RocketPunchDashPacket> TYPE =
        IPacket.type(AnvilCraftPigeonPlus.of("rocket_punch_dash"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RocketPunchDashPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.DOUBLE, RocketPunchDashPacket::dirX,
            ByteBufCodecs.DOUBLE, RocketPunchDashPacket::dirZ,
            ByteBufCodecs.DOUBLE, RocketPunchDashPacket::speed,
            ByteBufCodecs.VAR_INT, RocketPunchDashPacket::ticks,
            ByteBufCodecs.FLOAT, RocketPunchDashPacket::baseYaw,
            RocketPunchDashPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleOnClient(Player player) {
        RocketPunchClientState.startDash(
            new Vec3(this.dirX, 0.0, this.dirZ).normalize(), this.speed, this.ticks, this.baseYaw
        );
    }
}
