package com.mjmod.client;

import com.mjmod.network.MJNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Minimap-style coordinate form for Void Tears skill 3. The server validates
 * the dimension and coordinates, so any registered dimension from another mod
 * can be entered directly (for example, othermod:moon).
 */
public class VoidTearsTeleportGui extends Screen {
    private final boolean shiftCast;
    private EditBox dimensionBox;
    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;

    public VoidTearsTeleportGui() {
        this(false);
    }

    public VoidTearsTeleportGui(boolean shiftCast) {
        super(Component.literal("虚空之泪·削减空间"));
        this.shiftCast = shiftCast;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int fieldWidth = 220;
        int left = cx - fieldWidth / 2;
        int y = this.height / 2 - 74;

        dimensionBox = new EditBox(this.font, left, y, fieldWidth, 20,
                Component.literal("维度 ID"));
        dimensionBox.setMaxLength(256);
        if (this.minecraft != null && this.minecraft.player != null) {
            dimensionBox.setValue(this.minecraft.player.level().dimension().location().toString());
        }
        this.addRenderableWidget(dimensionBox);

        int coordWidth = 64;
        int gap = 8;
        int coordLeft = cx - (coordWidth * 3 + gap * 2) / 2;
        xBox = makeCoordinateBox(coordLeft, y + 44, coordWidth,
                this.minecraft != null && this.minecraft.player != null
                        ? Double.toString(this.minecraft.player.getX()) : "0");
        yBox = makeCoordinateBox(coordLeft + coordWidth + gap, y + 44, coordWidth,
                this.minecraft != null && this.minecraft.player != null
                        ? Double.toString(this.minecraft.player.getY()) : "64");
        zBox = makeCoordinateBox(coordLeft + (coordWidth + gap) * 2, y + 44, coordWidth,
                this.minecraft != null && this.minecraft.player != null
                        ? Double.toString(this.minecraft.player.getZ()) : "0");
        this.addRenderableWidget(xBox);
        this.addRenderableWidget(yBox);
        this.addRenderableWidget(zBox);

        this.addRenderableWidget(Button.builder(Component.literal("确认传送"),
                        button -> submitTeleport())
                .bounds(cx - 108, y + 88, 104, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("紧急传送"),
                        button -> emergencyTeleport())
                .bounds(cx + 4, y + 88, 104, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("关闭"),
                        button -> this.onClose())
                .bounds(cx - 45, y + 116, 90, 20).build());
    }

    private EditBox makeCoordinateBox(int x, int y, int width, String value) {
        EditBox box = new EditBox(this.font, x, y, width, 20, Component.literal("坐标"));
        box.setMaxLength(32);
        box.setValue(value);
        return box;
    }

    private void submitTeleport() {
        try {
            double x = Double.parseDouble(xBox.getValue().trim());
            double y = Double.parseDouble(yBox.getValue().trim());
            double z = Double.parseDouble(zBox.getValue().trim());
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || dimensionBox.getValue().trim().isEmpty()) {
                throw new NumberFormatException();
            }
            MJNetwork.sendVoidTearsTeleport(dimensionBox.getValue().trim(), x, y, z, shiftCast);
            this.onClose();
        } catch (NumberFormatException exception) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(Component.literal(
                        "§c请输入有效的维度 ID 和 X/Y/Z 数字坐标。"), true);
            }
        }
    }

    private void emergencyTeleport() {
        MJNetwork.sendVoidTearsEmergencyTeleport(shiftCast);
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int cx = this.width / 2;
        int panelLeft = cx - 150;
        int panelTop = this.height / 2 - 112;
        graphics.fill(panelLeft, panelTop, panelLeft + 300, panelTop + 174, 0xDD101522);
        graphics.renderOutline(panelLeft, panelTop, 300, 174, 0xFF77BBDD);
        graphics.drawCenteredString(this.font, "§b§l虚空之泪·削减空间", cx, panelTop + 10, 0xFFFFFF);
        graphics.drawString(this.font, "§7维度 ID（支持其他模组维度）", panelLeft + 40,
                panelTop + 30, 0xFFFFFF, false);
        graphics.drawString(this.font, "§7X", cx - 96, panelTop + 70, 0xFFFFFF, false);
        graphics.drawString(this.font, "§7Y", cx - 20, panelTop + 70, 0xFFFFFF, false);
        graphics.drawString(this.font, "§7Z", cx + 56, panelTop + 70, 0xFFFFFF, false);
        graphics.drawCenteredString(this.font,
                "§7确认传送按上方坐标；紧急传送随机移动到当前维度约 200 格外",
                cx, panelTop + 151, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
