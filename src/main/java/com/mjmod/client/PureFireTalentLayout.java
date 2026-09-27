package com.mjmod.client;

/** Shared page membership and geometry for the pure-fire talent tree. */
final class PureFireTalentLayout {
    private static final String[] PAGE_NAMES = {"体术", "法术", "构建"};
    private static final int[][] PAGE_SKILLS = {
            {2, 3, 4, 6, 7},
            {1, 9},
            {5, 8, 10}
    };

    private PureFireTalentLayout() {}

    static int pageCount() {
        return PAGE_NAMES.length;
    }

    static String pageName(int page) {
        return PAGE_NAMES[page];
    }

    static int[] skillsForPage(int page) {
        return PAGE_SKILLS[page].clone();
    }

    static int pageOf(int skillId) {
        for (int page = 0; page < PAGE_SKILLS.length; page++) {
            for (int skill : PAGE_SKILLS[page]) {
                if (skill == skillId) return page;
            }
        }
        return -1;
    }

    static int backgroundWidth(int width, int height) {
        int bgW = Math.min(width - 20, 560);
        int bgH = (int) (bgW * (297.0F / 512.0F));
        return bgH > height - 30 ? (int) ((height - 30) * (512.0F / 297.0F)) : bgW;
    }

    static int backgroundHeight(int width, int height) {
        int bgW = Math.min(width - 20, 560);
        return Math.min((int) (bgW * (297.0F / 512.0F)), height - 30);
    }

    static int backgroundY(int width, int height) {
        return height / 2 - backgroundHeight(width, height) / 2 - 10;
    }

    /** Nodes are centered between the page legend and the shortcut-bar heading. */
    static int nodeCenterY(int width, int height) {
        int bgY = backgroundY(width, height);
        int firstCenter = bgY + 86;
        int lastCenter = bgY + backgroundHeight(width, height) - 42 - 30;
        return (firstCenter + lastCenter) / 2;
    }

    static int nodeCenterX(int width, int height, int page, int skillId) {
        int[] skills = PAGE_SKILLS[page];
        int index = -1;
        for (int i = 0; i < skills.length; i++) {
            if (skills[i] == skillId) index = i;
        }
        if (index < 0) throw new IllegalArgumentException("Skill not present on page: " + skillId);
        int spacing = Math.min(56, (backgroundWidth(width, height) - 64) / Math.max(1, skills.length - 1));
        return width / 2 + (2 * index - skills.length + 1) * spacing / 2;
    }
}
