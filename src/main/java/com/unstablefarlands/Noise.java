package com.unstablefarlands;

/** Tiny seeded hash/value noise. No Minecraft API use, so it never breaks between versions. */
public final class Noise {
    private final long seed;

    public Noise(long seed) { this.seed = seed; }

    private static long mix(long h) {
        h ^= h >>> 33; h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33; h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }

    /** Deterministic value in [0,1) for an integer coordinate. */
    public double hash(int x, int z, int salt) {
        long h = mix(seed ^ (x * 0x9E3779B97F4A7C15L) ^ mix(z * 0xC2B2AE3D27D4EB4FL + salt));
        return (h >>> 11) * (1.0 / (1L << 53));
    }

    public double value(double x, double z, int salt) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double fx = x - x0, fz = z - z0;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        double a = hash(x0, z0, salt), b = hash(x0 + 1, z0, salt);
        double c = hash(x0, z0 + 1, salt), d = hash(x0 + 1, z0 + 1, salt);
        double top = a + (b - a) * fx, bot = c + (d - c) * fx;
        return top + (bot - top) * fz;
    }

    /** Fractal noise in [0,1]. */
    public double fbm(double x, double z, int octaves, int salt) {
        double sum = 0, amp = 0.5, norm = 0, f = 1;
        for (int i = 0; i < octaves; i++) {
            sum += value(x * f, z * f, salt + i * 31) * amp;
            norm += amp; amp *= 0.5; f *= 2;
        }
        return sum / norm;
    }
}
