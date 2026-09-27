package com.mjmod.client.gui;

import com.mjmod.MJMod;
import com.mjmod.network.MJNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

/** Lightweight visual-novel dialogue box with a typewriter subtitle effect. */
public class LeechDialogueScreen extends Screen {
    private final int entityId;
    private String speaker;
    private String line;
    private int affection;
    private int revealTicks;
    private boolean dialogueMusicStopped;

    public LeechDialogueScreen(int entityId, String speaker, String line, int affection) {
        super(Component.literal(speaker));
        this.entityId = entityId;
        this.speaker = speaker;
        this.line = line;
        this.affection = affection;
    }

    @Override
    protected void init() {
        revealTicks = 0;
        int buttonWidth = 66;
        int gap = 6;
        int totalWidth = buttonWidth * 4 + gap * 3;
        int startX = (width - totalWidth) / 2;
        int buttonY = height - 70;
        addRenderableWidget(Button.builder(Component.literal("送礼"),
                        button -> interact("gift"))
                .bounds(startX, buttonY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("讲笑话"),
                        button -> interact("joke"))
                .bounds(startX + (buttonWidth + gap), buttonY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("亲吻"),
                        button -> interact("kiss"))
                .bounds(startX + (buttonWidth + gap) * 2, buttonY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("留在此地"),
                        button -> interact("stay"))
                .bounds(startX + (buttonWidth + gap) * 3, buttonY, buttonWidth, 20).build());
    }

    @Override
    public void tick() {
        if (revealTicks < line.length() + 8) revealTicks++;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int width = this.width;
        int height = this.height;
        graphics.fill(0, 0, width, height, 0x28000000);
        int boxTop = height - 145;
        graphics.fill(28, boxTop, width - 28, height - 24, 0xE0101A2E);
        graphics.fill(28, boxTop, width - 28, boxTop + 3, 0xFF6BCBFF);
        graphics.drawString(this.font, Component.literal(speaker), 48, boxTop + 18,
                0xFFBDEBFF, true);
        int visible = Math.min(line.length(), Math.max(0, revealTicks - 4));
        String shown = line.substring(0, visible);
        int y = boxTop + 43;
        for (String part : shown.split("\\n", -1)) {
            graphics.drawString(this.font, Component.literal(part), 48, y, 0xFFF2F8FF, false);
            y += 16;
        }
        graphics.drawString(this.font, Component.literal("好感度  " + affection + " / 100"),
                width - 170, boxTop + 18, 0xFFFFD979, false);
        graphics.drawString(this.font, Component.literal("选择互动 · ESC 关闭"),
                48, height - 43, 0xFF9DB5C7, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Let the four interaction widgets consume their own clicks before the
        // dialogue background handles reveal/close clicks.
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 || button == 1) {
            if (revealTicks < line.length() + 8) revealTicks = line.length() + 8;
            else onClose();
            return true;
        }
        return false;
    }

    @Override
    public void removed() {
        stopDialogueMusic();
        MJNetwork.sendLeechInteraction(entityId, "close");
        super.removed();
    }

    public int getLeechEntityId() {
        return entityId;
    }

    /** Updates the current visual-novel page without closing it or stacking screens. */
    public void updateDialogue(String speaker, String line, int affection) {
        this.speaker = speaker;
        this.line = line;
        this.affection = affection;
        this.revealTicks = 0;
    }

    private void interact(String action) {
        MJNetwork.sendLeechInteraction(entityId, action);
    }

    private void stopDialogueMusic() {
        if (dialogueMusicStopped || minecraft == null) return;
        dialogueMusicStopped = true;
        minecraft.getSoundManager().stop(
                new ResourceLocation(MJMod.MODID, "leech_dialogue"), SoundSource.MUSIC);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
