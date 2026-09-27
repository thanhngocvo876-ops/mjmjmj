package com.mjmod.skill;

/** Standalone bounds and reveal regression: run with javac + java. */
public final class PureFireDomainGeometryTest {
    public static void main(String[] args) {
        check(PureFireDomainGeometry.HALF_SIZE == 75, "half size");
        check(PureFireDomainGeometry.MAX_Y - PureFireDomainGeometry.MIN_Y == 60, "height");
        check(PureFireDomainGeometry.FIRST_COLUMN == -75, "first column");
        check(PureFireDomainGeometry.LAST_COLUMN == 74, "last column");
        check(PureFireDomainGeometry.LAST_COLUMN - PureFireDomainGeometry.FIRST_COLUMN + 1 == 150,
                "exactly 150 columns");
        check(PureFireDomainGeometry.anchor(10.2D) == 10.0D, "anchor rounds down to boundary");
        check(PureFireDomainGeometry.anchor(10.8D) == 11.0D, "anchor rounds up to boundary");
        check(PureFireDomainGeometry.ring(-75, -75) == 74.5F, "corner ring");
        check(PureFireDomainGeometry.ring(74, 74) == 74.5F, "opposite corner ring");
        check(PureFireDomainGeometry.ring(-1, -1) == 0.5F, "center ring");
        check(PureFireDomainGeometry.growthRadius(0.0F) == 0.0F, "closed at creation");
        check(PureFireDomainGeometry.growthRadius(40.0F) == 75.0F, "full width at 40 ticks");
        check(PureFireDomainGeometry.growthRadius(20.0F) > 0.0F, "grows outward");
        check(PureFireDomainGeometry.growthRadius(20.0F) < 75.0F, "does not grow instantly");
        check(!PureFireDomainGeometry.revealed(74.5F, 20.0F), "far corner hidden until wave arrives");
        check(PureFireDomainGeometry.revealed(74.5F, 40.0F), "far corner revealed after expansion");
        System.out.println("PureFireDomainGeometryTest PASS");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
