package com.mjmod.client;

import com.mjmod.network.MJNetwork;
import com.mjmod.skill.VoidTearsSkillManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * UID coordinate-point screen for Void Tears skill 3.
 *
 * Points are server-owned; this screen only displays the last synchronized
 * snapshot and sends select/delete/add actions back to the server.
 */
public class VoidTearsPointsGui extends Screen {
    private static final int PANEL_WIDTH = 390;
    private static final int ROW_HEIGHT = 34;
    private int scrollOffset;

    public VoidTearsPointsGui() {
        super(Component.literal("虚空之泪坐标点"));
    }

    @Override
    protected void init() {
        int bottom = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.literal("添加当前位置"),
                        button -> MJNetwork.addVoidTearsPoint())
                .bounds(this.width / 2 - 185, bottom, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("清除选择"),
                        button -> MJNetwork.clearVoidTearsPointSelection())
                .bounds(this.width / 2 - 78, bottom, 78, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("刷新"),
                        button -> MJNetwork.requestVoidTearsPoints())
                .bounds(this.width / 2 + 5, bottom, 55, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("关闭"),
                        button -> this.onClose())
                .bounds(this.width / 2 + 65, bottom, 60, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int left = this.width / 2 - PANEL_WIDTH / 2;
        int top = 18;
        int bottom = this.height - 36;
        graphics.fill(left, top, left + PANEL_WIDTH, bottom, 0xDD101522);
        graphics.renderOutline(left, top, PANEL_WIDTH, bottom - top, 0xFF77BBDD);
        graphics.drawCenteredString(this.font, "§b§l虚空之泪·UID 坐标点", this.width / 2,
                top + 8, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                "§7Shift+Z：无选择时记录坐标，有选择时跨维度传送",
                this.width / 2, top + 23, 0xFFFFFF);

        List<VoidTearsSkillManager.CoordinatePoint> points = ClientEvents.VOID_TEARS_POINTS;
        int listTop = top + 43;
        int visibleRows = Math.max(1, (bottom - listTop - 5) / ROW_HEIGHT);
        int start = Math.min(scrollOffset, Math.max(0, points.size() - visibleRows));
        int end = Math.min(points.size(), start + visibleRows);
        if (points.isEmpty()) {
            graphics.drawCenteredString(this.font, "§8暂无坐标点，请用 Shift+Z 记录当前位置",
                    this.width / 2, listTop + 22, 0xFFFFFF);
        }
        for (int index = start; index < end; index++) {
            VoidTearsSkillManager.CoordinatePoint point = points.get(index);
            int row = index - start;
            int y = listTop + row * ROW_HEIGHT;
            boolean selected = point.uid().equals(ClientEvents.VOID_TEARS_SELECTED_POINT);
            int color = selected ? 0xAA24526B : 0xAA202838;
            graphics.fill(left + 8, y, left + PANEL_WIDTH - 8, y + ROW_HEIGHT - 3, color);
            graphics.renderOutline(left + 8, y, PANEL_WIDTH - 16, ROW_HEIGHT - 3,
                    selected ? 0xFF7DE0FF : 0xFF46546A);
            String marker = selected ? "§a[已选] " : "§7[ ] ";
            graphics.drawString(this.font, marker + "UID " + shortUid(point.uid()),
                    left + 14, y + 4, 0xFFFFFF, false);
            String coords = String.format("§f%s  §7(%.1f, %.1f, %.1f)",
                    shortDimension(point.dimension()), point.x(), point.y(), point.z());
            graphics.drawString(this.font, coords, left + 14, y + 17, 0xFFFFFF, false);
            graphics.drawString(this.font, "§b选择", left + PANEL_WIDTH - 92, y + 5,
                    0xFFFFFF, false);
            graphics.drawString(this.font, "§c删除", left + PANEL_WIDTH - 48, y + 5,
                    0xFFFFFF, false);
        }
        if (points.size() > visibleRows) {
            graphics.drawString(this.font,
                    "§7滚轮查看  " + (start + 1) + "-" + end + "/" + points.size(),
                    left + 12, bottom - 15, 0xFFFFFF, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int left = this.width / 2 - PANEL_WIDTH / 2;
            int top = 18;
            int bottom = this.height - 36;
            int listTop = top + 43;
            int visibleRows = Math.max(1, (bottom - listTop - 5) / ROW_HEIGHT);
            List<VoidTearsSkillManager.CoordinatePoint> points = ClientEvents.VOID_TEARS_POINTS;
            int start = Math.min(scrollOffset, Math.max(0, points.size() - visibleRows));
            int row = (int) ((mouseY - listTop) / ROW_HEIGHT);
            if (row >= 0 && row < visibleRows && mouseY >= listTop) {
                int index = start + row;
                if (index < points.size()) {
                    VoidTearsSkillManager.CoordinatePoint point = points.get(index);
                    if (mouseX >= left + PANEL_WIDTH - 104 && mouseX < left + PANEL_WIDTH - 52) {
                        MJNetwork.selectVoidTearsPoint(point.uid());
                        return true;
                    }
                    if (mouseX >= left + PANEL_WIDTH - 52 && mouseX < left + PANEL_WIDTH - 8) {
                        MJNetwork.deleteVoidTearsPoint(point.uid());
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int listTop = 18 + 43;
        int bottom = this.height - 36;
        int visibleRows = Math.max(1, (bottom - listTop - 5) / ROW_HEIGHT);
        int max = Math.max(0, ClientEvents.VOID_TEARS_POINTS.size() - visibleRows);
        scrollOffset = Math.max(0, Math.min(max, scrollOffset + (amount > 0 ? -1 : 1)));
        return true;
    }

    private static String shortUid(String uid) {
        return uid.length() > 12 ? uid.substring(0, 12) + "…" : uid;
    }

    private static String shortDimension(String dimension) {
        return dimension.length() > 22 ? "…" + dimension.substring(dimension.length() - 21) : dimension;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
