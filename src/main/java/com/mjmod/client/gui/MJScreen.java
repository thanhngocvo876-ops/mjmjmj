package com.mjmod.client.gui;

import com.mjmod.client.MJTextureManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 素材展示界面：居中显示图片，按屏幕大小等比缩放。
 */
public class MJScreen extends Screen {
    private final ResourceLocation texture;

    public MJScreen() {
        super(Component.literal("MJ 素材"));
        this.texture = MJTextureManager.prepare();
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.literal("关闭"), b -> this.onClose())
                .bounds(this.width / 2 - 40, this.height - 40, 80, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int imgW = MJTextureManager.getTextureWidth();
        int imgH = MJTextureManager.getTextureHeight();
        if (imgW <= 0 || imgH <= 0) {
            return;
        }

        double scale = Math.min((this.width * 0.75) / imgW, (this.height * 0.7) / imgH);
        int w = (int) (imgW * scale);
        int h = (int) (imgH * scale);
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2 - 10;

        graphics.blit(this.texture, x, y, w, h, 0.0F, 0.0F, imgW, imgH, imgW, imgH);

        graphics.drawCenteredString(this.font,
                Component.literal("输入 mj 显示素材 | 把图片放到 [游戏目录]/mjmod/images/mj.png 即可替换"),
                this.width / 2, this.height - 18, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
