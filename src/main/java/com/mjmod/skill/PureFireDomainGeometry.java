package com.mjmod.skill;

/** Shared, Minecraft-independent dimensions and expansion math for the domain. */
public final class PureFireDomainGeometry {
    public static final int HALF_SIZE = 75;
    public static final int FIRST_COLUMN = -HALF_SIZE;
    public static final int LAST_COLUMN = HALF_SIZE - 1;
    public static final int MIN_Y = -3;
    public static final int MAX_Y = 57;
    public static final float GROW_TICKS = 40.0F;

    private PureFireDomainGeometry() {}

    /** The 150 columns align with integer world coordinates on either side of this boundary. */
    public static double anchor(double coordinate) {
        return Math.floor(coordinate + 0.5D);
    }

    /** Chebyshev distance of the block center to the domain's fixed center. */
    public static float ring(int dx, int dz) {
        return Math.max(Math.abs(dx + 0.5F), Math.abs(dz + 0.5F));
    }

    public static float growthRadius(float age) {
        float progress = Math.max(0.0F, Math.min(1.0F, age / GROW_TICKS));
        float ease = 1.0F - (1.0F - progress) * (1.0F - progress) * (1.0F - progress);
        return HALF_SIZE * ease;
    }

    public static boolean revealed(float ring, float age) {
        return ring <= growthRadius(age);
    }
}
