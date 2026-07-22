package dev.anvilcraft.pigeonplus.client.sound;

import dev.anvilcraft.pigeonplus.init.AddonSounds;
import dev.anvilcraft.pigeonplus.util.NozzlePlasmaJetUtil;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public class NozzleJetSoundInstance extends AbstractTickableSoundInstance {
    public static final int ENGINE_ON_TICKS = 28;
    public static final int ENGINE_ON_TO_FIRE_LEAD_TICKS = 8;
    public static final int ENGINE_FIRE_TO_FIRE_LEAD_TICKS = 10;
    public static final int ENGINE_FIRE_TICKS = 96;
    private static final float VOLUME = 5.0F;

    private final BlockPos pos;
    private final boolean startup;
    private int age;

    public NozzleJetSoundInstance(BlockPos pos, boolean startup) {
        super(startup ? AddonSounds.ENGINE_ON.get() : AddonSounds.ENGINE_FIRE.get(), SoundSource.BLOCKS, RandomSource.create());
        this.pos = pos.immutable();
        this.startup = startup;
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
            || !(level.getBlockEntity(this.pos) instanceof PlasmaJetsBlockEntity)
            || NozzlePlasmaJetUtil.getStructuralCauldron(level, this.pos) == null) {
            this.stop();
            return;
        }

        this.x = this.pos.getX() + 0.5D;
        this.y = this.pos.getY() + 0.5D;
        this.z = this.pos.getZ() + 0.5D;

        this.age++;
        if (this.startup) {
            if (this.age >= ENGINE_ON_TICKS) {
                this.stop();
            }
        } else if (this.age >= ENGINE_FIRE_TICKS) {
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
        return this.startup && this.age >= ENGINE_ON_TICKS - ENGINE_ON_TO_FIRE_LEAD_TICKS;
    }

    public boolean shouldChainFire() {
        return !this.startup && this.age >= ENGINE_FIRE_TICKS - ENGINE_FIRE_TO_FIRE_LEAD_TICKS;
    }

    public void forceStop() {
        this.stop();
    }
}
