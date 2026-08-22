package dev.anvilcraft.pigeonplus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.pigeonplus.util.NozzleExhaustUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class NozzleExhaustRenderer {
    private static final ResourceLocation BEAM_TEXTURE =
        ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final float CENTER_X = 0.5F;
    private static final float CENTER_Z = 0.5F;
    private static final float BASE_Y_OFFSET = -1.0F;
    private static final float TOTAL_HEIGHT = NozzleExhaustUtil.JET_VISUAL_HEIGHT;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final float PLUME_WAVE_TIME_SCALE = 0.180F;
    private static final float PLUME_FLICKER_TIME_SCALE = 0.720F;

    private NozzleExhaustRenderer() {
    }

    public static void render(
        PoseStack poseStack,
        MultiBufferSource buffers,
        float time,
        BlockPos pos,
        Direction facing,
        Propellant propellant,
        float visibleLength,
        float flameProgress
    ) {
        if (flameProgress <= 0.0F || visibleLength <= 0.0F) {
            return;
        }

        VertexConsumer buffer = buffers.getBuffer(RenderType.beaconBeam(BEAM_TEXTURE, true));
        PoseStack.Pose pose = poseStack.last();
        RenderProfile profile = profile(propellant);
        float heightScale = flameProgress * flameProgress * (3.0F - 2.0F * flameProgress);
        float radiusScale = 0.30F + flameProgress * 0.70F;
        float alphaScale = 0.20F + flameProgress * 0.80F;

        renderLayer(
            pose,
            buffer,
            time,
            heightScale,
            visibleLength,
            radiusScale,
            alphaScale,
            facing,
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
            visibleLength,
            radiusScale,
            alphaScale,
            facing,
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
        if (flameProgress >= 0.35F) {
            renderMachDiamonds(pose, buffer, time, heightScale, visibleLength, radiusScale, alphaScale, facing, profile);
        }
    }

    private static void renderLayer(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        float time,
        float heightScale,
        float visibleLength,
        float radiusScale,
        float alphaScale,
        Direction facing,
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
        float fullHeight = TOTAL_HEIGHT * heightScale;
        float maxHeight = Math.min(fullHeight, visibleLength);
        if (fullHeight <= 0.0F || maxHeight <= 0.0F) {
            return;
        }
        for (int plane = 0; plane < planes; plane++) {
            float angle = plane * (TWO_PI / planes);
            float dx = (float) Math.cos(angle);
            float dz = (float) Math.sin(angle);
            for (int segment = 0; segment < segments; segment++) {
                float t0 = segment / (float) segments;
                float t1 = (segment + 1) / (float) segments;
                float y0 = fullHeight * t0;
                float y1 = fullHeight * t1;
                if (y0 >= maxHeight) {
                    break;
                }
                if (y1 > maxHeight) {
                    y1 = maxHeight;
                    t1 = y1 / fullHeight;
                }
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
                    alpha1,
                    facing
                );
            }
        }
    }

    private static void renderMachDiamonds(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        float time,
        float heightScale,
        float visibleLength,
        float radiusScale,
        float alphaScale,
        Direction facing,
        RenderProfile profile
    ) {
        float fullHeight = TOTAL_HEIGHT * heightScale;
        float maxHeight = Math.min(fullHeight, visibleLength);
        if (fullHeight <= 0.0F || maxHeight <= 0.0F) {
            return;
        }
        for (int i = 0; i < profile.diamondCount; i++) {
            float center = (i + profile.diamondCenterOffset) / (profile.diamondCount + profile.diamondCountOffset);
            float bandHeight = profile.diamondBandHeightBase + i * profile.diamondBandHeightStep;
            float scaledCenterY = center * fullHeight;
            if (scaledCenterY >= maxHeight) {
                break;
            }
            float scaledBandHeight = bandHeight * heightScale;
            float y0 = Math.max(0.0F, scaledCenterY - scaledBandHeight * 0.5F);
            float y1 = Math.min(maxHeight, scaledCenterY + scaledBandHeight * 0.5F);
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
                    profile.diamondMidR, profile.diamondMidG, profile.diamondMidB, alpha,
                    facing
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
                    profile.diamondEndR, profile.diamondEndG, profile.diamondEndB, alpha * profile.diamondEndAlphaScale,
                    facing
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
        float a1,
        Direction facing
    ) {
        float yy0 = y0 + BASE_Y_OFFSET;
        float yy1 = y1 + BASE_Y_OFFSET;
        float v0 = yy0 * 0.18F;
        float v1 = yy1 * 0.18F;
        addVertex(pose, buffer, facing, CENTER_X - dx * radius0, yy0, CENTER_Z - dz * radius0, r0, g0, b0, a0, 0.0F, v0);
        addVertex(pose, buffer, facing, CENTER_X - dx * radius1, yy1, CENTER_Z - dz * radius1, r1, g1, b1, a1, 0.0F, v1);
        addVertex(pose, buffer, facing, CENTER_X + dx * radius1, yy1, CENTER_Z + dz * radius1, r1, g1, b1, a1, 1.0F, v1);
        addVertex(pose, buffer, facing, CENTER_X + dx * radius0, yy0, CENTER_Z + dz * radius0, r0, g0, b0, a0, 1.0F, v0);
    }

    private static void addVertex(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        Direction facing,
        float sideX,
        float axis,
        float sideZ,
        float r,
        float g,
        float b,
        float a,
        float u,
        float v
    ) {
        float x;
        float y;
        float z;
        float nx;
        float ny;
        float nz;
        switch (facing) {
            case DOWN -> {
                x = sideX;
                y = 1.0F - axis;
                z = sideZ;
                nx = 0.0F;
                ny = -1.0F;
                nz = 0.0F;
            }
            case EAST -> {
                x = axis;
                y = sideX;
                z = sideZ;
                nx = 1.0F;
                ny = 0.0F;
                nz = 0.0F;
            }
            case WEST -> {
                x = 1.0F - axis;
                y = sideX;
                z = sideZ;
                nx = -1.0F;
                ny = 0.0F;
                nz = 0.0F;
            }
            case SOUTH -> {
                x = sideX;
                y = sideZ;
                z = axis;
                nx = 0.0F;
                ny = 0.0F;
                nz = 1.0F;
            }
            case NORTH -> {
                x = sideX;
                y = sideZ;
                z = 1.0F - axis;
                nx = 0.0F;
                ny = 0.0F;
                nz = -1.0F;
            }
            default -> {
                x = sideX;
                y = axis;
                z = sideZ;
                nx = 0.0F;
                ny = 1.0F;
                nz = 0.0F;
            }
        }
        buffer.addVertex(pose, x, y, z)
            .setColor(r, g, b, a)
            .setUv(u, v)
            .setOverlay(0)
            .setLight(FULL_BRIGHT)
            .setNormal(pose, nx, ny, nz);
    }

    private static RenderProfile profile(Propellant propellant) {
        return switch (propellant) {
            case METHANE -> new RenderProfile(
                24, 6, 1.06F, 0.26F, 0.40F, 6.8F, 0.46F, 0.16F,
                0.56F, 0.78F, 1.00F, 0.88F, 0.58F, 0.96F,
                18, 8, 9.0F, 0.76F, 0.14F, 0.18F, 7.2F, 0.62F, 0.16F,
                0.94F, 0.98F, 1.00F, 0.98F, 0.72F, 1.00F,
                3, 0.55F, 0.20F, 0.70F, 0.10F, 0.48F, 0.88F, 0.14F, 0.40F, 0.10F, 0.96F, 0.10F,
                0.11F, 0.94F,
                0.72F, 0.90F, 1.00F, 0.98F, 0.96F, 1.00F, 0.90F, 0.62F, 1.00F,
                0.82F, 0.78F
            );
            case KEROSENE -> new RenderProfile(
                24, 6, 1.06F, 0.26F, 0.40F, 6.8F, 0.46F, 0.16F,
                1.00F, 0.68F, 0.28F, 0.90F, 0.82F, 0.66F,
                18, 8, 9.0F, 0.76F, 0.14F, 0.18F, 7.2F, 0.62F, 0.16F,
                1.00F, 0.90F, 0.62F, 1.00F, 0.98F, 0.88F,
                3, 0.55F, 0.20F, 0.70F, 0.10F, 0.48F, 0.88F, 0.14F, 0.40F, 0.10F, 0.96F, 0.10F,
                0.11F, 0.94F, 1.00F, 0.76F, 0.36F, 1.00F, 0.98F, 0.88F, 0.84F, 0.76F, 0.60F,
                0.92F, 0.84F
            );
            case HYDROGEN -> new RenderProfile(
                24, 6, 1.06F, 0.26F, 0.40F, 6.8F, 0.26F, 0.16F,
                1.00F, 0.95F, 0.84F, 0.99F, 0.78F, 0.52F,
                18, 8, 9.0F, 0.76F, 0.14F, 0.18F, 7.2F, 0.36F, 0.16F,
                1.00F, 0.98F, 0.94F, 1.00F, 0.89F, 0.72F,
                3, 0.55F, 0.20F, 0.70F, 0.10F, 0.24F, 0.88F, 0.14F, 0.40F, 0.10F, 0.96F, 0.10F,
                0.11F, 0.94F,
                1.00F, 0.97F, 0.90F, 1.00F, 0.91F, 0.74F, 0.99F, 0.80F, 0.54F,
                0.82F, 0.78F
            );
        };
    }

    public enum Propellant {
        METHANE,
        KEROSENE,
        HYDROGEN
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
