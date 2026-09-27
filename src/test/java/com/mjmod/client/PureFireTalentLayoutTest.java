package com.mjmod.client;

import java.util.Arrays;

/** Standalone regression runner: javac + java, no Minecraft client required. */
public final class PureFireTalentLayoutTest {
    private static final int NODE_SIZE = 28;

    public static void main(String[] args) {
        int[][] expected = {{2, 3, 4, 6, 7}, {1, 9}, {5, 8, 10}};
        String[] names = {"体术", "法术", "构建"};
        boolean[] seen = new boolean[11];
        for (int page = 0; page < expected.length; page++) {
            int[] skills = PureFireTalentLayout.skillsForPage(page);
            check(Arrays.equals(skills, expected[page]), "wrong skills in " + names[page]);
            check(names[page].equals(PureFireTalentLayout.pageName(page)), "wrong tab label");
            for (int skill : skills) {
                check(!seen[skill], "duplicate skill " + skill);
                seen[skill] = true;
                check(PureFireTalentLayout.pageOf(skill) == page, "wrong page for skill " + skill);
            }
        }
        for (int skill = 1; skill <= 10; skill++) {
            check(seen[skill], "missing skill " + skill);
        }
        for (int[] screen : new int[][] {{320, 240}, {427, 240}, {854, 480}, {1920, 1080}}) {
            assertNodesFit(screen[0], screen[1]);
        }
        check(PureFireTalentLayout.pageOf(10) == 2, "skill 10 must be on 构建");
        System.out.println("PureFireTalentLayoutTest PASS: three pages; all ten skills; compact layouts");
    }

    private static void assertNodesFit(int width, int height) {
        int bgW = PureFireTalentLayout.backgroundWidth(width, height);
        int bgH = PureFireTalentLayout.backgroundHeight(width, height);
        int bgX = width / 2 - bgW / 2;
        int bgY = PureFireTalentLayout.backgroundY(width, height);
        int slotY = bgY + bgH - 42;
        int centerY = PureFireTalentLayout.nodeCenterY(width, height);
        check(centerY - NODE_SIZE / 2 >= bgY + 80, "node obscures tabs/legend on " + width + "x" + height);
        check(centerY + NODE_SIZE / 2 <= slotY - 15, "node obscures shortcut bar on " + width + "x" + height);
        for (int page = 0; page < 3; page++) {
            int previousRight = Integer.MIN_VALUE;
            for (int skill : PureFireTalentLayout.skillsForPage(page)) {
                int centerX = PureFireTalentLayout.nodeCenterX(width, height, page, skill);
                int left = centerX - NODE_SIZE / 2 - 2;
                int right = centerX + NODE_SIZE / 2 + 2;
                check(left >= bgX + 4 && right <= bgX + bgW - 4,
                        "skill " + skill + " outside background on " + width + "x" + height);
                check(left > previousRight, "overlapping skill icons on page " + page);
                previousRight = right;
            }
        }
    }

    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }
}
