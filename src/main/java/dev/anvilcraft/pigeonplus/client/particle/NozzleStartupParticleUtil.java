package dev.anvilcraft.pigeonplus.client.particle;

import dev.dubhe.anvilcraft.init.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

public final class NozzleStartupParticleUtil {
    public static final int STARTUP_RING_TICKS = 8;

    private NozzleStartupParticleUtil() {
    }

    public static void spawnStartupRing(ClientLevel level, BlockPos jetPos, int age) {
        if (age < 0 || age >= STARTUP_RING_TICKS) {
            return;
        }

        RandomSource random = level.getRandom();
        double centerX = jetPos.getX() + 0.5;
        double centerY = jetPos.getY() - 0.82;
        double centerZ = jetPos.getZ() + 0.5;
        double progress = age / (double) STARTUP_RING_TICKS;

        for (int ring = 0; ring < 2; ring++) {
            double radius = 0.35 + ring * 0.18 + progress * 1.15;
            double rise = 0.10 + progress * 0.14 + ring * 0.02;
            double y = centerY + progress * 0.90 + ring * 0.05;
            int count = 20 + ring * 8;
            for (int i = 0; i < count; i++) {
                double angle = (Math.PI * 2.0 * i) / count + random.nextDouble() * 0.08;
                double x = centerX + Math.cos(angle) * radius;
                double z = centerZ + Math.sin(angle) * radius;
                double outwardX = Math.cos(angle) * (0.05 + progress * 0.07 + ring * 0.015);
                double outwardZ = Math.sin(angle) * (0.05 + progress * 0.07 + ring * 0.015);
                level.addParticle(
                    ModParticles.PLASMA_JETS.get(),
                    true,
                    x,
                    y,
                    z,
                    outwardX + (random.nextDouble() - 0.5) * 0.01,
                    rise + random.nextDouble() * 0.015,
                    outwardZ + (random.nextDouble() - 0.5) * 0.01
                );
            }
        }
    }
}
