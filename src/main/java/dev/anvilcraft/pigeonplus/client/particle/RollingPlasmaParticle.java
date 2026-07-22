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

    protected RollingPlasmaParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed,
        SpriteSet sprites
    ) {
        super(level, x, y, z);
        this.xd = xSpeed + (Math.random() * 2.0 - 1.0) * 0.008F;
        this.yd = ySpeed + (Math.random() * 2.0 - 1.0) * 0.004F;
        this.zd = zSpeed + (Math.random() * 2.0 - 1.0) * 0.008F;
        this.sprites = sprites;
        this.friction = 0.92F;
        this.gravity = -0.01F;
        this.speedUpWhenYMotionIsBlocked = false;
        this.quadSize = 0.06F * (this.random.nextFloat() * this.random.nextFloat() * 1.5F + 0.9F);
        this.lifetime = 22 + this.random.nextInt(10);
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = 0.9F;
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
        this.setAlpha(0.9F * (1.0F - (float) this.age / this.lifetime));
        this.pigeonplus$setColorFromAge(this.age, this.lifetime);
        this.setSpriteFromAge(this.sprites);
    }

    private void pigeonplus$setColorFromAge(int age, int maxAge) {
        float progress = maxAge <= 0 ? 1.0F : Math.min(1.0F, age / (float) maxAge);
        float whiteBlend = Math.max(0.0F, 1.0F - progress * 1.35F);
        float orangeBlend = Math.max(0.0F, (progress - 0.32F) / 0.68F);
        float r = 1.0F;
        float g = 0.56F + whiteBlend * 0.44F - orangeBlend * 0.08F;
        float b = 0.12F + whiteBlend * 0.88F - orangeBlend * 0.10F;
        this.setColor(r, Math.max(0.20F, g), Math.max(0.0F, b));
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
            return new RollingPlasmaParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
