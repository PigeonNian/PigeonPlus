package dev.anvilcraft.pigeonplus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.pigeonplus.client.sound.NozzleJetSoundController;
import dev.anvilcraft.pigeonplus.util.NozzlePlasmaJetUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class NozzlePlasmaJetRenderer {
    private static final ResourceLocation BEAM_TEXTURE =
        ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final float CENTER_X = 0.5F;
    private static final float CENTER_Z = 0.5F;
    private static final float BASE_Y_OFFSET = -1.0F;
    private static final float TOTAL_HEIGHT = NozzlePlasmaJetUtil.JET_VISUAL_HEIGHT;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final float PLUME_WAVE_TIME_SCALE = 0.180F;
    private static final float PLUME_FLICKER_TIME_SCALE = 0.720F;

    private NozzlePlasmaJetRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, float time, BlockPos pos, Propellant propellant) {
        float startupProgress = NozzleJetSoundController.getFlameStartupProgress(pos);
        if (startupProgress <= 0.0F) {
            return;
        }

        VertexConsumer buffer = buffers.getBuffer(RenderType.beaconBeam(BEAM_TEXTURE, true));
        PoseStack.Pose pose = poseStack.last();
        RenderProfile profile = profile(propellant);
        float heightScale = startupProgress * startupProgress * (3.0F - 2.0F * startupProgress);
        float radiusScale = 0.30F + startupProgress * 0.70F;
        float alphaScale = 0.20F + startupProgress * 0.80F;

        renderLayer(
            pose,
            buffer,
            time,
            heightScale,
            radiusScale,
            alphaScale,
            profile.outerSegments,
            profile.outerPlanes,
            profile.outerStartRadius,
            profile.outerEndRadius,
            profile.outerWaveAmplitude,
            profile.outerWaveFrequency,
            profile.outerAlpha,
            profile.outerFlickerAmplitude,
            profile.outerStartR, profile.outerStartG, profile.outerStartB,
            profile.outerEndR, profile.outerEndG, profile.outerEndB
        );
        renderLayer(
            pose,
            buffer,
            time + profile.corePhaseOffset,
            heightScale,
            radiusScale,
            alphaScale,
            profile.coreSegments,
            profile.corePlanes,
            profile.coreStartRadius,
            profile.coreEndRadius,
            profile.coreWaveAmplitude,
            profile.coreWaveFrequency,
            profile.coreAlpha,
            profile.coreFlickerAmplitude,
            profile.coreStartR, profile.coreStartG, profile.coreStartB,
            profile.coreEndR, profile.coreEndG, profile.coreEndB
        );
        if (startupProgress >= 0.35F) {
            renderMachDiamonds(pose, buffer, time, heightScale, radiusScale, alphaScale, profile);
        }
    }

    private static void renderLayer(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        float time,
        float heightScale,
        float radiusScale,
        float alphaScale,
        int segments,
        int planes,
        float startRadius,
        float endRadius,
        float waveAmplitude,
        float waveFrequency,
        float baseAlpha,
        float flickerAmplitude,
        float startR,
        float startG,
        float startB,
        float endR,
        float endG,
        float endB
    ) {
        for (int plane = 0; plane < planes; plane++) {
            float angle = plane * (TWO_PI / planes);
            float dx = (float) Math.cos(angle);
            float dz = (float) Math.sin(angle);
            for (int segment = 0; segment < segments; segment++) {
                float t0 = segment / (float) segments;
                float t1 = (segment + 1) / (float) segments;
                float y0 = TOTAL_HEIGHT * heightScale * t0;
                float y1 = TOTAL_HEIGHT * heightScale * t1;
                float radius0 = plumeRadius(t0, time, startRadius, endRadius, waveAmplitude, waveFrequency) * radiusScale;
                float radius1 = plumeRadius(t1, time, startRadius, endRadius, waveAmplitude, waveFrequency) * radiusScale;
                float alpha0 = plumeAlpha(t0, time, baseAlpha, flickerAmplitude) * alphaScale;
                float alpha1 = plumeAlpha(t1, time, baseAlpha, flickerAmplitude) * alphaScale;
                float r0 = lerp(startR, endR, t0);
                float g0 = lerp(startG, endG, t0);
                float b0 = lerp(startB, endB, t0);
                float r1 = lerp(startR, endR, t1);
                float g1 = lerp(startG, endG, t1);
                float b1 = lerp(startB, endB, t1);

                addVerticalQuad(
                    pose,
                    buffer,
                    dx,
                    dz,
                    y0,
                    y1,
                    radius0,
                    radius1,
                    r0,
                    g0,
                    b0,
                    alpha0,
                    r1,
                    g1,
                    b1,
                    alpha1
                );
            }
        }
    }

    private static void renderMachDiamonds(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        float time,
        float heightScale,
        float radiusScale,
        float alphaScale,
        RenderProfile profile
    ) {
        for (int i = 0; i < profile.diamondCount; i++) {
            float center = (i + profile.diamondCenterOffset) / (profile.diamondCount + profile.diamondCountOffset);
            float bandHeight = profile.diamondBandHeightBase + i * profile.diamondBandHeightStep;
            float scaledCenterY = center * TOTAL_HEIGHT * heightScale;
            float scaledBandHeight = bandHeight * heightScale;
            float y0 = Math.max(0.0F, scaledCenterY - scaledBandHeight * 0.5F);
            float y1 = Math.min(TOTAL_HEIGHT * heightScale, scaledCenterY + scaledBandHeight * 0.5F);
            float pulse = profile.diamondPulseBase
                + profile.diamondPulseAmplitude * (float) Math.sin(time * profile.diamondPulseFrequency + i * profile.diamondPulsePhaseStep);
            float coreRadius = plumeRadius(
                center,
                time + profile.corePhaseOffset,
                profile.coreStartRadius,
                profile.coreEndRadius,
                profile.coreWaveAmplitude,
                profile.coreWaveFrequency
            ) * radiusScale;
            float outer = Math.max(0.08F, coreRadius * profile.diamondOuterScale + profile.diamondOuterBias);
            float inner = outer * profile.diamondInnerRatio;
            float alpha = profile.diamondAlpha * alphaScale * (1.0F - center * profile.diamondAlphaTaper) * pulse;

            for (int plane = 0; plane < 4; plane++) {
                float angle = (float) (Math.PI * 0.25 * plane);
                float dx = (float) Math.cos(angle);
                float dz = (float) Math.sin(angle);
                addVerticalQuad(
                    pose,
                    buffer,
                    dx,
                    dz,
                    y0,
                    scaledCenterY,
                    outer,
                    inner,
                    profile.diamondStartR, profile.diamondStartG, profile.diamondStartB, alpha * profile.diamondStartAlphaScale,
                    profile.diamondMidR, profile.diamondMidG, profile.diamondMidB, alpha
                );
                addVerticalQuad(
                    pose,
                    buffer,
                    dx,
                    dz,
                    scaledCenterY,
                    y1,
                    inner,
                    outer,
                    profile.diamondMidR, profile.diamondMidG, profile.diamondMidB, alpha,
                    profile.diamondEndR, profile.diamondEndG, profile.diamondEndB, alpha * profile.diamondEndAlphaScale
                );
            }
        }
    }

    private static float plumeRadius(
        float t,
        float time,
        float startRadius,
        float endRadius,
        float waveAmplitude,
        float waveFrequency
    ) {
        float base = lerp(startRadius, endRadius, (float) Math.pow(t, 0.85F));
        float wave = triangleWave(t * waveFrequency - time * PLUME_WAVE_TIME_SCALE);
        float taper = 1.0F - t * 0.55F;
        return Math.max(0.08F, base + waveAmplitude * wave * taper);
    }

    private static float plumeAlpha(float t, float time, float baseAlpha, float flickerAmplitude) {
        float falloff = 1.0F - t * 0.72F;
        float flicker = 0.88F + flickerAmplitude * (float) Math.sin(time * PLUME_FLICKER_TIME_SCALE + t * 11.0F);
        return Math.max(0.02F, baseAlpha * falloff * flicker);
    }

    private static float triangleWave(float x) {
        float wrapped = x - (float) Math.floor(x);
        return 1.0F - Math.abs(wrapped * 2.0F - 1.0F);
    }

    private static float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    private static void addVerticalQuad(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        float dx,
        float dz,
        float y0,
        float y1,
        float radius0,
        float radius1,
        float r0,
        float g0,
        float b0,
        float a0,
        float r1,
        float g1,
        float b1,
        float a1
    ) {
        float yy0 = y0 + BASE_Y_OFFSET;
        float yy1 = y1 + BASE_Y_OFFSET;
        float v0 = yy0 * 0.18F;
        float v1 = yy1 * 0.18F;
        buffer.addVertex(pose, CENTER_X - dx * radius0, yy0, CENTER_Z - dz * radius0)
            .setColor(r0, g0, b0, a0)
            .setUv(0.0F, v0)
            .setOverlay(0)
            .setLight(FULL_BRIGHT)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
        buffer.addVertex(pose, CENTER_X - dx * radius1, yy1, CENTER_Z - dz * radius1)
            .setColor(r1, g1, b1, a1)
            .setUv(0.0F, v1)
            .setOverlay(0)
            .setLight(FULL_BRIGHT)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
        buffer.addVertex(pose, CENTER_X + dx * radius1, yy1, CENTER_Z + dz * radius1)
            .setColor(r1, g1, b1, a1)
            .setUv(1.0F, v1)
            .setOverlay(0)
            .setLight(FULL_BRIGHT)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
        buffer.addVertex(pose, CENTER_X + dx * radius0, yy0, CENTER_Z + dz * radius0)
            .setColor(r0, g0, b0, a0)
            .setUv(1.0F, v0)
            .setOverlay(0)
            .setLight(FULL_BRIGHT)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static RenderProfile profile(Propellant propellant) {
        return switch (propellant) {
            case METHANE -> new RenderProfile(
                22, 7, 1.36F, 0.28F, 0.30F, 7.4F, 0.20F, 0.08F,
                0.62F, 0.86F, 1.00F, 0.50F, 0.28F, 1.00F,
                20, 9, 7.0F, 0.58F, 0.12F, 0.18F, 8.1F, 0.56F, 0.14F,
                0.96F, 1.00F, 1.00F, 0.76F, 0.93F, 1.00F,
                7, 0.52F, 0.30F, 0.44F, 0.05F, 0.24F, 0.80F, 0.15F, 0.42F, 0.08F, 0.84F, 0.14F,
                0.12F, 0.98F, 0.70F, 0.90F, 1.00F, 0.92F, 0.98F, 1.00F, 0.52F, 0.28F, 0.95F,
                0.82F, 0.78F
            );
            case KEROSENE -> new RenderProfile(
                24, 6, 1.06F, 0.42F, 0.40F, 6.8F, 0.46F, 0.16F,
                0.92F, 0.44F, 0.14F, 1.00F, 0.96F, 0.82F,
                18, 8, 9.0F, 0.76F, 0.24F, 0.18F, 7.2F, 0.62F, 0.16F,
                1.00F, 0.82F, 0.30F, 1.00F, 0.99F, 0.86F,
                3, 0.55F, 0.20F, 0.70F, 0.10F, 0.48F, 0.88F, 0.14F, 0.40F, 0.10F, 0.96F, 0.10F,
                0.11F, 0.94F, 0.92F, 0.58F, 0.22F, 1.00F, 0.88F, 0.50F, 1.00F, 0.94F, 0.78F,
                0.92F, 0.84F
            );
        };
    }

    public enum Propellant {
        METHANE,
        KEROSENE
    }

    private record RenderProfile(
        int outerSegments,
        int outerPlanes,
        float outerStartRadius,
        float outerEndRadius,
        float outerWaveAmplitude,
        float outerWaveFrequency,
        float outerAlpha,
        float outerFlickerAmplitude,
        float outerStartR,
        float outerStartG,
        float outerStartB,
        float outerEndR,
        float outerEndG,
        float outerEndB,
        int coreSegments,
        int corePlanes,
        float corePhaseOffset,
        float coreStartRadius,
        float coreEndRadius,
        float coreWaveAmplitude,
        float coreWaveFrequency,
        float coreAlpha,
        float coreFlickerAmplitude,
        float coreStartR,
        float coreStartG,
        float coreStartB,
        float coreEndR,
        float coreEndG,
        float coreEndB,
        int diamondCount,
        float diamondCenterOffset,
        float diamondCountOffset,
        float diamondBandHeightBase,
        float diamondBandHeightStep,
        float diamondAlpha,
        float diamondOuterScale,
        float diamondOuterBias,
        float diamondInnerRatio,
        float diamondAlphaTaper,
        float diamondPulseBase,
        float diamondPulseAmplitude,
        float diamondPulseFrequency,
        float diamondPulsePhaseStep,
        float diamondStartR,
        float diamondStartG,
        float diamondStartB,
        float diamondMidR,
        float diamondMidG,
        float diamondMidB,
        float diamondEndR,
        float diamondEndG,
        float diamondEndB,
        float diamondStartAlphaScale,
        float diamondEndAlphaScale
    ) {
    }
}
