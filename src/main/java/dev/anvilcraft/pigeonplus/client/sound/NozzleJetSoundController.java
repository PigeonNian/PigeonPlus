package dev.anvilcraft.pigeonplus.client.sound;

import dev.anvilcraft.pigeonplus.client.particle.NozzleStartupParticleUtil;
import dev.anvilcraft.pigeonplus.util.NozzlePlasmaJetUtil;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class NozzleJetSoundController {
    private static final Map<BlockPos, SoundState> SOUND_STATES = new HashMap<>();
    public static final int FLAME_DELAY_TICKS = 6;
    public static final int FLAME_GROWTH_TICKS = 12;

    private record SoundState(List<NozzleJetSoundInstance> playingSounds, int age) {
        private SoundState ticked() {
            return new SoundState(this.playingSounds, this.age + 1);
        }
    }

    private NozzleJetSoundController() {
    }

    public static void tick(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused() || minecraft.level == null) {
            return;
        }
        Direction facing = NozzlePlasmaJetUtil.getStructuralFacing(minecraft.level, pos);
        if (facing == null) {
            return;
        }

        SoundState state = SOUND_STATES.get(pos);
        if (state == null) {
            NozzleJetSoundInstance created = new NozzleJetSoundInstance(pos, true);
            minecraft.getSoundManager().play(created);
            NozzleStartupParticleUtil.spawnStartupRing((ClientLevel) minecraft.level, pos, facing, 0);
            List<NozzleJetSoundInstance> sounds = new ArrayList<>();
            sounds.add(created);
            SOUND_STATES.put(pos, new SoundState(sounds, 1));
            return;
        }

        state.playingSounds().removeIf(NozzleJetSoundInstance::isStopped);

        if (state.age() < NozzleStartupParticleUtil.STARTUP_RING_TICKS) {
            NozzleStartupParticleUtil.spawnStartupRing((ClientLevel) minecraft.level, pos, facing, state.age());
        }

        int firstFireTick = NozzleJetSoundInstance.ENGINE_ON_TICKS - NozzleJetSoundInstance.ENGINE_ON_TO_FIRE_LEAD_TICKS;
        int fireInterval = NozzleJetSoundInstance.ENGINE_FIRE_TICKS - NozzleJetSoundInstance.ENGINE_FIRE_TO_FIRE_LEAD_TICKS;
        if (state.age() == firstFireTick
            || (state.age() > firstFireTick && (state.age() - firstFireTick) % fireInterval == 0)) {
            NozzleJetSoundInstance fireSound = new NozzleJetSoundInstance(pos, false);
            minecraft.getSoundManager().play(fireSound);
            state.playingSounds().add(fireSound);
        }

        SOUND_STATES.put(pos, state.ticked());
    }

    public static void cleanup() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            for (SoundState state : SOUND_STATES.values()) {
                for (NozzleJetSoundInstance sound : state.playingSounds()) {
                    if (!sound.isStopped()) {
                        sound.forceStop();
                    }
                }
            }
            SOUND_STATES.clear();
            return;
        }

        Iterator<Map.Entry<BlockPos, SoundState>> iterator = SOUND_STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, SoundState> entry = iterator.next();
            BlockPos pos = entry.getKey();
            SoundState state = entry.getValue();
            state.playingSounds().removeIf(NozzleJetSoundInstance::isStopped);

            boolean active = level.getBlockEntity(pos) instanceof PlasmaJetsBlockEntity
                && NozzlePlasmaJetUtil.getStructuralCauldron(level, pos) != null;
            if (active) {
                continue;
            }

            for (NozzleJetSoundInstance sound : state.playingSounds()) {
                if (!sound.isStopped()) {
                    sound.forceStop();
                }
            }
            iterator.remove();
        }
    }

    public static float getFlameStartupProgress(BlockPos pos) {
        SoundState state = SOUND_STATES.get(pos);
        if (state == null) {
            return 1.0F;
        }
        if (state.age() <= FLAME_DELAY_TICKS) {
            return 0.0F;
        }
        float progress = (state.age() - FLAME_DELAY_TICKS) / (float) FLAME_GROWTH_TICKS;
        return Math.min(1.0F, Math.max(0.0F, progress));
    }
}
