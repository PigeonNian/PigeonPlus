package dev.anvilcraft.pigeonplus.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.util.SkillCooldowns;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 服务端把某个技能的冷却开始同步给客户端，供右下角计时器显示。
 *
 * <p>与位移包不同，这里只同步「剩余 tick」这一个数：客户端不保存截止时刻，
 * 因为两端 {@code gameTime} 并不同步，存绝对值会算出错误的剩余时间。
 * 客户端收到后自己每 tick 递减即可。
 *
 * @param skillOrdinal 技能下标（{@link SkillCooldowns.Skill}）
 * @param ticks        冷却总时长，单位 tick
 */
public record SkillCooldownPacket(int skillOrdinal, int ticks) implements IClientboundPacket {
    public static final Type<SkillCooldownPacket> TYPE =
        IPacket.type(AnvilCraftPigeonPlus.of("skill_cooldown"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkillCooldownPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SkillCooldownPacket::skillOrdinal,
            ByteBufCodecs.VAR_INT, SkillCooldownPacket::ticks,
            SkillCooldownPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleOnClient(Player player) {
        // 越界下标直接忽略：网络包内容不可信，不能让它把客户端打崩
        SkillCooldowns.Skill skill = SkillCooldowns.Skill.byOrdinal(this.skillOrdinal);
        if (skill == null) return;
        SkillCooldowns.applySync(skill, this.ticks);
    }
}
