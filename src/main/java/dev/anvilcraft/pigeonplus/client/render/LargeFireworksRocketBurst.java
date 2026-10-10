package dev.anvilcraft.pigeonplus.client.render;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.Level;

import java.util.List;

public final class LargeFireworksRocketBurst {

    public static final int BALLS_PER_STAR = 4;

    private static final float MIN_SCALE = 1.0F;

    private static final float MAX_SCALE = 2.0F;

    private static final float BALL_SCALE = 0.5F;

    private static final float BALL_JITTER = 0.3F;

    private static final double[][] STAR_COORDS = {
        {0.0, 1.0},
        {0.3455, 0.309},
        {0.9511, 0.309},
        {0.3795918367346939, -0.12653061224489795},
        {0.6122448279591837, -0.8040816326530612},
        {0.0, -0.35918367346938773}
    };

    private static final double[][] CREEPER_COORDS = {
        {0.0, 0.2}, {0.2, 0.2}, {0.2, 0.6}, {0.6, 0.6}, {0.6, 0.2}, {0.2, 0.2},
        {0.2, 0.0}, {0.4, 0.0}, {0.4, -0.6}, {0.2, -0.6}, {0.2, -0.4}, {0.0, -0.4}
    };

    private LargeFireworksRocketBurst() {
    }

    public static float randomScale(RandomSource random) {
        return Mth.lerp(random.nextFloat(), MIN_SCALE, MAX_SCALE);
    }

    public static float ballScale(RandomSource random, float starScale) {
        return starScale * BALL_SCALE
               * (1.0F - BALL_JITTER / 2.0F + random.nextFloat() * BALL_JITTER);
    }

    public static void spawn(
        Level level,
        RandomSource random,
        double x,
        double y,
        double z,
        FireworkExplosion explosion,
        float ballScale
    ) {
        if (!(level instanceof ClientLevel clientLevel)) {
            return;
        }
        ParticleEngine engine = Minecraft.getInstance().particleEngine;
        IntList colors = explosion.colors();
        if (colors.isEmpty()) {
            colors = IntList.of(DyeColor.BLACK.getFireworkColor());
        }
        IntList fadeColors = explosion.fadeColors();
        boolean trail = explosion.hasTrail();
        boolean twinkle = explosion.hasTwinkle();

        double speed = ballSpeed(explosion.shape(), ballScale);

        double angle = random.nextDouble() * Math.PI * 2.0;
        FireworkParticles.Starter factory = new FireworkParticles.Starter(
            clientLevel, x, y, z,
            Math.cos(angle) * speed, speed * 0.5, Math.sin(angle) * speed,
            engine, List.of(explosion)
        );

        switch (explosion.shape()) {
            case SMALL_BALL -> factory.createParticleBall(
                0.25 * ballScale, ballRadius(2, ballScale), colors, fadeColors, trail, twinkle);
            case LARGE_BALL -> factory.createParticleBall(
                0.5 * ballScale, ballRadius(4, ballScale), colors, fadeColors, trail, twinkle);
            case STAR -> factory.createParticleShape(
                0.5 * ballScale, STAR_COORDS, colors, fadeColors, trail, twinkle, false);
            case CREEPER -> factory.createParticleShape(
                0.5 * ballScale, CREEPER_COORDS, colors, fadeColors, trail, twinkle, true);
            case BURST -> factory.createParticleBurst(colors, fadeColors, trail, twinkle);
        }

        engine.createParticle(
            ColorParticleOption.create(ParticleTypes.FLASH, colors.getInt(0)),
            x, y, z, 0.0, 0.0, 0.0
        );
    }

    private static double ballSpeed(FireworkExplosion.Shape shape, float scale) {
        return switch (shape) {
            case SMALL_BALL -> 0.25 * scale;
            case LARGE_BALL, STAR, CREEPER, BURST -> 0.5 * scale;
        };
    }

    private static int ballRadius(int baseRadius, float scale) {
        return Math.max(1, Math.round(baseRadius * scale));
    }
}
