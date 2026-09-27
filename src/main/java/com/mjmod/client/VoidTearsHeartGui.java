package com.mjmod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 悲情之心状态界面（P 键打开）。数值只由服务端指令修改。 */
public class VoidTearsHeartGui extends Screen {
    public VoidTearsHeartGui() {
        super(Component.literal("虚空之泪·悲情之心"));
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        renderBackground(gg);
        int cx = width / 2;
        int top = height / 2 - 65;
        int heart = Math.max(0, ClientEvents.CLIENT_TRAGIC_HEART);
        int level = ClientEvents.CLIENT_VOID_TEARS_PERMISSION
                ? Math.min(4, 1 + heart / 25) : 0;
        int bonus = (heart / 25) * 5;

        gg.drawCenteredString(font, "§b§l虚空之泪·悲情之心", cx, top, 0xFFFFFF);
        gg.drawCenteredString(font, level == 0 ? "§7尚未获得虚空之泪权限"
                : "§e虚空之泪等级：§bLV" + level, cx, top + 22, 0xFFFFFF);
        gg.drawCenteredString(font, "§d悲情之心：§f" + heart + "%", cx, top + 40, 0xFFFFFF);

        int barW = 220, barH = 12, bx = cx - barW / 2, by = top + 58;
        gg.fill(bx, by, bx + barW, by + barH, 0xAA101020);
        int fill = (int) (barW * (Math.min(100, heart) / 100.0F));
        gg.fill(bx, by, bx + fill, by + barH, 0xFF8ED8FF);
        gg.renderOutline(bx, by, barW, barH, 0xFFFFFFFF);
        gg.drawCenteredString(font, "§7每满25%伤害 +5；100%后仍可继续叠加", cx, by + 20, 0xFFFFFF);
        gg.drawCenteredString(font, "§c当前虚空之泪额外伤害：+" + bonus, cx, by + 36, 0xFFFFFF);
        gg.drawCenteredString(font, "§8数值只能通过管理员指令修改 · 按 Esc 关闭", cx, by + 58, 0xFFFFFF);
        super.render(gg, mouseX, mouseY, partialTick);
    }
}
