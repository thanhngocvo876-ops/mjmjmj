package com.mjmod.client;

import com.mjmod.MJMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 素材纹理管理：
 * 1. 优先加载 [游戏目录]/mjmod/images/mj.png（玩家放自己的图片，无需重新编译）；
 * 2. 不存在时回退到内置占位图 mjmod:textures/gui/mj.png。
 */
public class MJTextureManager {
    public static final ResourceLocation FALLBACK_TEXTURE = new ResourceLocation(MJMod.MODID, "textures/gui/mj.png");
    private static final ResourceLocation DYNAMIC_TEXTURE = new ResourceLocation(MJMod.MODID, "dynamic/mj");

    private static int textureWidth = 256;
    private static int textureHeight = 256;

    /** 返回要渲染的纹理位置，并保证纹理已注册。 */
    public static ResourceLocation prepare() {
        Minecraft mc = Minecraft.getInstance();
        File imageFile = new File(new File(mc.gameDirectory, "mjmod"), "images/mj.png");
        if (imageFile.isFile()) {
            try {
                int[] size = readSize(imageFile.toPath());
                if (size != null) {
                    textureWidth = size[0];
                    textureHeight = size[1];
                }
                TextureManager tm = mc.getTextureManager();
                tm.register(DYNAMIC_TEXTURE, new FileTexture(imageFile.toPath()));
                return DYNAMIC_TEXTURE;
            } catch (Exception e) {
                MJMod.LOGGER.error("[MJ] 加载外部图片 {} 失败，回退到内置占位图: {}", imageFile, e.toString());
            }
        }
        // 回退：读取内置占位图尺寸
        Optional<Resource> resource = mc.getResourceManager().getResource(FALLBACK_TEXTURE);
        if (resource.isPresent()) {
            try (InputStream in = resource.get().open()) {
                NativeImage img = NativeImage.read(in);
                textureWidth = img.getWidth();
                textureHeight = img.getHeight();
                img.close();
            } catch (Exception ignored) {
            }
        }
        return FALLBACK_TEXTURE;
    }

    private static int[] readSize(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            NativeImage img = NativeImage.read(in);
            int w = img.getWidth();
            int h = img.getHeight();
            img.close();
            return new int[]{w, h};
        }
    }

    public static int getTextureWidth() {
        return textureWidth;
    }

    public static int getTextureHeight() {
        return textureHeight;
    }

    /** 直接从一个本地文件加载纹理。 */
    public static class FileTexture extends AbstractTexture {
        private final Path path;

        public FileTexture(Path path) {
            this.path = path;
        }

        @Override
        public void load(ResourceManager manager) throws IOException {
            try (InputStream stream = Files.newInputStream(this.path)) {
                NativeImage image = NativeImage.read(stream);
                try {
                    this.bind();
                    image.upload(0, 0, 0, false);
                } finally {
                    image.close();
                }
            }
        }
    }
}
