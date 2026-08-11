package dev.anvilcraft.pigeonplus.client.support;

import dev.anvilcraft.pigeonplus.client.AnvilCraftPigeonPlusClient;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class NozzleScreenShakeManager {
    private static final NozzleScreenShakeManager INSTANCE = new NozzleScreenShakeManager();
    private static final long STARTUP_DURATION_NANOS = 800_000_000L;
    private static final long CONTINUOUS_TIMEOUT_NANOS = 150_000_000L;
    private static final float STARTUP_YAW_PITCH_AMPLITUDE = 3.2F;
    private static final float STARTUP_ROLL_AMPLITUDE = 4.8F;
    private static final float STARTUP_FREQUENCY = 11.5F;
    private static final float CONTINUOUS_YAW_PITCH_AMPLITUDE = 0.42F;
    private static final float CONTINUOUS_ROLL_AMPLITUDE = 0.70F;
    private static final float CONTINUOUS_FREQUENCY = 18.0F;

    private final RandomSource random = RandomSource.create();
    private long startupStartNanos;
    private float startupIntensity;
    private float startupPhaseSeed;
    private long continuousUpdateNanos;
    private float continuousIntensity;
    private float continuousPhaseSeed;

    private NozzleScreenShakeManager() {
    }

    public static NozzleScreenShakeManager getInstance() {
        return INSTANCE;
    }

    public void trigger(Vec3 center, float radius) {
        if (this.amplitudeScale() <= 0.0F) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || radius <= 0.0F) {
            return;
        }
        double distance = minecraft.player.position().distanceTo(center);
        if (distance > radius) {
            return;
        }
        float falloff = Mth.clamp((float) (1.0D - distance / radius), 0.0F, 1.0F);
        if (falloff <= 0.01F) {
            return;
        }
        long now = System.nanoTime();
        if (falloff >= this.startupIntensity || !this.isStartupActive(now)) {
            this.startupStartNanos = now;
            this.startupIntensity = falloff;
            this.startupPhaseSeed = this.random.nextFloat() * 1000.0F;
        }
    }

    public void sustain(Vec3 center, float radius) {
        if (this.amplitudeScale() <= 0.0F) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || radius <= 0.0F) {
            return;
        }
        double distance = minecraft.player.position().distanceTo(center);
        if (distance > radius) {
            return;
        }
        float falloff = Mth.clamp((float) (1.0D - distance / radius), 0.0F, 1.0F);
        if (falloff <= 0.01F) {
            return;
        }
        if (!this.isContinuousActive(System.nanoTime())) {
            this.continuousPhaseSeed = this.random.nextFloat() * 1000.0F;
        }
        this.continuousUpdateNanos = System.nanoTime();
        this.continuousIntensity = falloff;
    }

    public boolean isActive() {
        long now = System.nanoTime();
        return this.isStartupActive(now) || this.isContinuousActive(now);
    }

    public float[] computeAngleOffsets() {
        long now = System.nanoTime();
        if (!this.isStartupActive(now) && !this.isContinuousActive(now)) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Entity camera = minecraft.getCameraEntity();
        if (camera == null) {
            return null;
        }
        float[] startupOffsets = this.computeStartupOffsets(now);
        float[] continuousOffsets = this.computeContinuousOffsets(now);
        if (startupOffsets == null && continuousOffsets == null) {
            return null;
        }
        float yaw = (startupOffsets == null ? 0.0F : startupOffsets[0]) + (continuousOffsets == null ? 0.0F : continuousOffsets[0]);
        float pitch = (startupOffsets == null ? 0.0F : startupOffsets[1]) + (continuousOffsets == null ? 0.0F : continuousOffsets[1]);
        float roll = (startupOffsets == null ? 0.0F : startupOffsets[2]) + (continuousOffsets == null ? 0.0F : continuousOffsets[2]);
        return new float[] {yaw, pitch, roll};
    }

    private float[] computeStartupOffsets(long now) {
        if (!this.isStartupActive(now)) {
            return null;
        }
        float progress = (now - this.startupStartNanos) / (float) STARTUP_DURATION_NANOS;
        float falloff = (1.0F - Mth.clamp(progress, 0.0F, 1.0F));
        falloff = falloff * falloff * this.startupIntensity;
        if (falloff <= 0.01F) {
            return null;
        }
        float seconds = (now - this.startupStartNanos) / 1_000_000_000.0F;
        float phase = this.startupPhaseSeed + seconds * STARTUP_FREQUENCY * Mth.TWO_PI;
        float amplitude = this.amplitudeScale();
        float yaw = (float) (Math.sin(phase * 1.1F) + 0.55F * Math.sin(phase * 2.6F + 0.7F))
            * STARTUP_YAW_PITCH_AMPLITUDE * falloff * amplitude;
        float pitch = (float) (Math.sin(phase * 1.3F + 1.4F) + 0.5F * Math.sin(phase * 3.0F))
            * STARTUP_YAW_PITCH_AMPLITUDE * falloff * amplitude;
        float roll = (float) (Math.sin(phase * 0.9F + 0.3F) + 0.45F * Math.sin(phase * 2.2F + 2.1F))
            * STARTUP_ROLL_AMPLITUDE * falloff * amplitude;
        return new float[] {yaw, pitch, roll};
    }

    private float[] computeContinuousOffsets(long now) {
        if (!this.isContinuousActive(now)) {
            return null;
        }
        float timeout = (now - this.continuousUpdateNanos) / (float) CONTINUOUS_TIMEOUT_NANOS;
        float falloff = (1.0F - Mth.clamp(timeout, 0.0F, 1.0F)) * this.continuousIntensity;
        if (falloff <= 0.01F) {
            return null;
        }
        float seconds = now / 1_000_000_000.0F;
        float phase = this.continuousPhaseSeed + seconds * CONTINUOUS_FREQUENCY * Mth.TWO_PI;
        float amplitude = this.amplitudeScale();
        float yaw = (float) (Math.sin(phase * 1.8F) + 0.35F * Math.sin(phase * 4.1F))
            * CONTINUOUS_YAW_PITCH_AMPLITUDE * falloff * amplitude;
        float pitch = (float) (Math.sin(phase * 1.6F + 1.2F) + 0.30F * Math.sin(phase * 3.7F + 0.4F))
            * CONTINUOUS_YAW_PITCH_AMPLITUDE * falloff * amplitude;
        float roll = (float) (Math.sin(phase * 1.2F + 2.3F) + 0.35F * Math.sin(phase * 3.4F + 1.1F))
            * CONTINUOUS_ROLL_AMPLITUDE * falloff * amplitude;
        return new float[] {yaw, pitch, roll};
    }

    private float amplitudeScale() {
        return AnvilCraftPigeonPlusClient.CLIENT_CONFIG.nozzleShakeAmplitude;
    }

    private boolean isStartupActive(long now) {
        return this.startupIntensity > 0.01F && now - this.startupStartNanos < STARTUP_DURATION_NANOS;
    }

    private boolean isContinuousActive(long now) {
        return this.continuousIntensity > 0.01F && now - this.continuousUpdateNanos < CONTINUOUS_TIMEOUT_NANOS;
    }
}
