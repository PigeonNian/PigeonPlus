package dev.anvilcraft.pigeonplus.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class RollingPlasmaParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final ColorProfile colorProfile;

    protected RollingPlasmaParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        SpriteSet sprites,
        ColorProfile colorProfile
    ) {
        super(level, x, y, z);
        this.xd = xSpeed + (Math.random() * 2.0 - 1.0) * 0.008F;
        this.yd = ySpeed + (Math.random() * 2.0 - 1.0) * 0.004F;
        this.zd = zSpeed + (Math.random() * 2.0 - 1.0) * 0.008F;
        this.sprites = sprites;
        this.colorProfile = colorProfile;
        this.friction = 0.92F;
        this.gravity = -0.01F;
        this.speedUpWhenYMotionIsBlocked = false;
        this.quadSize = 0.06F * (this.random.nextFloat() * this.random.nextFloat() * 1.5F + 0.9F);
        this.lifetime = 22 + this.random.nextInt(10);
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = colorProfile.maxAlpha();
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
        this.pigeonplus$setColorFromAge(0, this.lifetime);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        this.xd *= 0.96D;
        this.yd = this.yd * 0.94D + 0.002D;
        this.zd *= 0.96D;
        this.setAlpha(this.colorProfile.maxAlpha() * (1.0F - (float) this.age / this.lifetime));
        this.pigeonplus$setColorFromAge(this.age, this.lifetime);
        this.setSpriteFromAge(this.sprites);
    }

    private void pigeonplus$setColorFromAge(int age, int maxAge) {
        float progress = maxAge <= 0 ? 1.0F : Math.min(1.0F, age / (float) maxAge);
        float r;
        float g;
        float b;
        if (progress < 0.42F) {
            float t = progress / 0.42F;
            r = lerp(this.colorProfile.startR, this.colorProfile.midR, t);
            g = lerp(this.colorProfile.startG, this.colorProfile.midG, t);
            b = lerp(this.colorProfile.startB, this.colorProfile.midB, t);
        } else {
            float t = (progress - 0.42F) / 0.58F;
            r = lerp(this.colorProfile.midR, this.colorProfile.endR, t);
            g = lerp(this.colorProfile.midG, this.colorProfile.endG, t);
            b = lerp(this.colorProfile.midB, this.colorProfile.endB, t);
        }
        this.setColor(r, g, b);
    }

    private static float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(
            SimpleParticleType type,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed
        ) {
            return new RollingPlasmaParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, ColorProfile.AIR);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class MethaneProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public MethaneProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(
            SimpleParticleType type,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed
        ) {
            return new RollingPlasmaParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, ColorProfile.AIR);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class HydrogenProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public HydrogenProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(
            SimpleParticleType type,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed
        ) {
            return new RollingPlasmaParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, ColorProfile.HYDROGEN);
        }
    }

    private record ColorProfile(
        float startR,
        float startG,
        float startB,
        float midR,
        float midG,
        float midB,
        float endR,
        float endG,
        float endB,
        float maxAlpha
    ) {
        private static final ColorProfile AIR = new ColorProfile(
            1.00F, 1.00F, 1.00F,
            0.92F, 0.96F, 1.00F,
            0.78F, 0.82F, 0.86F,
            0.90F
        );
        private static final ColorProfile HYDROGEN = new ColorProfile(
            1.00F, 0.98F, 0.93F,
            1.00F, 0.90F, 0.72F,
            0.99F, 0.79F, 0.50F,
            0.42F
        );
    }
}
