package dev.anvilcraft.pigeonplus.client.sound;

import dev.anvilcraft.pigeonplus.init.AddonSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * 火箭重拳的蓄力音效。
 *
 * <p>为什么做成 {@link AbstractTickableSoundInstance} 而不是一次性 {@code playSound}：
 * 蓄力音效需要在「冲刺开始」时被<strong>主动掐断</strong>。{@code Level#playSound} 播出去的声音
 * 没有任何句柄可以停，只能靠发停止包按 location 匹配——而客户端自己持有的实例对象
 * 可以直接 {@code stop()}，最干净也最可靠。
 *
 * <p>{@code isRelative} 为 true（构造里传 {@code true}）：声音跟随玩家，玩家高速冲刺时
 * 不会因为声源留在原地而衰减掉。
 */
public final class RocketPunchChargeSoundInstance extends AbstractTickableSoundInstance {
    /** 播放该音效的玩家，用于确认实例仍然属于当前客户端玩家。 */
    private final int playerId;

    public RocketPunchChargeSoundInstance(int playerId) {
        super(AddonSounds.ROCKET_PUNCH_CHARGE.get(), SoundSource.PLAYERS, RandomSource.create());
        this.playerId = playerId;
        this.looping = false;
        this.delay = 0;
        this.volume = 1.0f;
        this.pitch = 1.0f;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    /**
     * 该实例是否仍属于当前客户端玩家。
     *
     * <p>换维度或重连后玩家对象会变，旧实例不该继续占用槽位。
     */
    public boolean belongsToCurrentPlayer() {
        return Minecraft.getInstance().player != null
            && Minecraft.getInstance().player.getId() == this.playerId;
    }

    /**
     * 主动掐断。{@code AbstractTickableSoundInstance#stop} 是 protected，这里开一个公开入口。
     */
    public void stopNow() {
        this.stop();
    }

    @Override
    public void tick() {
        // 非循环音效播完即由引擎移除；这里只处理「玩家已不存在」的情况
        if (!this.belongsToCurrentPlayer()) {
            this.stop();
        }
    }
}
