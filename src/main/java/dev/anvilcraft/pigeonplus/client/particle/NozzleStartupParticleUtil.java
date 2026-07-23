package dev.anvilcraft.pigeonplus.client.particle;

import dev.anvilcraft.pigeonplus.init.AddonParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class NozzleStartupParticleUtil {
    public static final int STARTUP_RING_TICKS = 8;

    private NozzleStartupParticleUtil() {
    }

    public static void spawnStartupRing(ClientLevel level, BlockPos jetPos, Direction facing, int age) {
        if (age < 0 || age >= STARTUP_RING_TICKS) {
            return;
        }

        RandomSource random = level.getRandom();
        double progress = age / (double) STARTUP_RING_TICKS;

        for (int ring = 0; ring < 2; ring++) {
            double radius = 0.35 + ring * 0.18 + progress * 1.15;
            double rise = 0.10 + progress * 0.14 + ring * 0.02;
            int count = 20 + ring * 8;
            for (int i = 0; i < count; i++) {
                double angle = (Math.PI * 2.0 * i) / count + random.nextDouble() * 0.08;
                double sideX = 0.5 + Math.cos(angle) * radius;
                double axis = -0.82 + progress * 0.90 + ring * 0.05;
                double sideZ = 0.5 + Math.sin(angle) * radius;
                double outwardX = Math.cos(angle) * (0.05 + progress * 0.07 + ring * 0.015);
                double outwardZ = Math.sin(angle) * (0.05 + progress * 0.07 + ring * 0.015);
                Vec3 pos = point(jetPos, facing, sideX, axis, sideZ);
                Vec3 velocity = vector(
                    facing,
                    outwardX + (random.nextDouble() - 0.5) * 0.01,
                    rise + random.nextDouble() * 0.015,
                    outwardZ + (random.nextDouble() - 0.5) * 0.01
                );
                level.addParticle(
                    AddonParticles.ROLLING_PLASMA.get(),
                    true,
                    pos.x,
                    pos.y,
                    pos.z,
                    velocity.x,
                    velocity.y,
                    velocity.z
                );
            }
        }
    }

    private static Vec3 point(BlockPos jetPos, Direction facing, double sideX, double axis, double sideZ) {
        Vec3 local = vector(facing, sideX, axis, sideZ);
        if (facing == Direction.DOWN || facing == Direction.WEST || facing == Direction.NORTH) {
            local = local.add(
                facing == Direction.WEST ? 1.0 : 0.0,
                facing == Direction.DOWN ? 1.0 : 0.0,
                facing == Direction.NORTH ? 1.0 : 0.0
            );
        }
        return new Vec3(jetPos.getX() + local.x, jetPos.getY() + local.y, jetPos.getZ() + local.z);
    }

    private static Vec3 vector(Direction facing, double sideX, double axis, double sideZ) {
        return switch (facing) {
            case DOWN -> new Vec3(sideX, -axis, sideZ);
            case EAST -> new Vec3(axis, sideX, sideZ);
            case WEST -> new Vec3(-axis, sideX, sideZ);
            case SOUTH -> new Vec3(sideX, sideZ, axis);
            case NORTH -> new Vec3(sideX, sideZ, -axis);
            default -> new Vec3(sideX, axis, sideZ);
        };
    }
}
