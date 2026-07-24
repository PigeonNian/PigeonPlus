package dev.anvilcraft.pigeonplus.client.sound;

import dev.anvilcraft.pigeonplus.block.entity.NozzleExhaustBlockEntity;
import dev.anvilcraft.pigeonplus.init.AddonSounds;
import dev.anvilcraft.pigeonplus.util.NozzleExhaustUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public class NozzleSoundInstance extends AbstractTickableSoundInstance {
    public static final int ENGINE_ON_TICKS = 28;
    public static final int ENGINE_ON_TO_FIRE_LEAD_TICKS = 8;
    public static final int ENGINE_FIRE_TO_FIRE_LEAD_TICKS = 10;
    public static final int ENGINE_FIRE_TICKS = 96;
    public static final long TICK_MILLIS = 50L;
    public static final long ENGINE_ON_MILLIS = ENGINE_ON_TICKS * TICK_MILLIS;
    public static final long ENGINE_ON_TO_FIRE_LEAD_MILLIS = ENGINE_ON_TO_FIRE_LEAD_TICKS * TICK_MILLIS;
    public static final long ENGINE_FIRE_TO_FIRE_LEAD_MILLIS = ENGINE_FIRE_TO_FIRE_LEAD_TICKS * TICK_MILLIS;
    public static final long ENGINE_FIRE_MILLIS = ENGINE_FIRE_TICKS * TICK_MILLIS;
    private static final float VOLUME = 5.0F;

    private final BlockPos pos;
    private final boolean startup;
    private final long startNanos;
    private final long durationNanos;

    public NozzleSoundInstance(BlockPos pos, boolean startup) {
        super(startup ? AddonSounds.ENGINE_ON.get() : AddonSounds.ENGINE_FIRE.get(), SoundSource.BLOCKS, RandomSource.create());
        this.pos = pos.immutable();
        this.startup = startup;
        this.startNanos = System.nanoTime();
        this.durationNanos = millisToNanos(startup ? ENGINE_ON_MILLIS : ENGINE_FIRE_MILLIS);
        this.looping = false;
        this.delay = 0;
        this.volume = VOLUME;
        this.pitch = 1.0F;
        this.relative = false;
        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null
            || !(level.getBlockEntity(this.pos) instanceof NozzleExhaustBlockEntity)
            || !NozzleExhaustUtil.isNozzleActive(level, this.pos)) {
            this.stop();
            return;
        }
        BlockPos soundPos = NozzleExhaustUtil.getStructuralOutletPos(level, this.pos);
        if (soundPos == null) {
            this.stop();
            return;
        }

        this.x = soundPos.getX() + 0.5D;
        this.y = soundPos.getY() + 0.5D;
        this.z = soundPos.getZ() + 0.5D;

        if (System.nanoTime() - this.startNanos >= this.durationNanos) {
            this.stop();
        }
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public boolean isStartup() {
        return this.startup;
    }

    public boolean shouldStartLoop() {
        return this.startup && System.nanoTime() - this.startNanos >= millisToNanos(ENGINE_ON_MILLIS - ENGINE_ON_TO_FIRE_LEAD_MILLIS);
    }

    public boolean shouldChainFire() {
        return !this.startup && System.nanoTime() - this.startNanos >= millisToNanos(ENGINE_FIRE_MILLIS - ENGINE_FIRE_TO_FIRE_LEAD_MILLIS);
    }

    public void forceStop() {
        this.stop();
    }

    private static long millisToNanos(long millis) {
        return millis * 1_000_000L;
    }
}
