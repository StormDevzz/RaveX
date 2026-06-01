package ravex.utility.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ravex.mixin.render.GuiGraphicsAccessor;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

public class RavexFontRenderer {

    private static RavexFontRenderer sfMedium;
    private static RavexFontRenderer comfortaa;
    private static RavexFontRenderer sfBold;

    public static RavexFontRenderer getSfMedium() {
        if (sfMedium == null) {
            sfMedium = new RavexFontRenderer(loadAwtFont("/assets/ravex/font/sf_medium.ttf", 11f), 9);
        }
        return sfMedium;
    }

    public static RavexFontRenderer getComfortaa() {
        if (comfortaa == null) {
            comfortaa = new RavexFontRenderer(loadAwtFont("/assets/ravex/font/comfortaa.ttf", 13f), 11);
        }
        return comfortaa;
    }

    public static RavexFontRenderer getSfBold() {
        if (sfBold == null) {
            sfBold = new RavexFontRenderer(loadAwtFont("/assets/ravex/font/sf_bold.ttf", 11f), 9);
        }
        return sfBold;
    }

    private static Font loadAwtFont(String path, float size) {
        try {
            java.awt.Font font = java.awt.Font.createFont(
                java.awt.Font.TRUETYPE_FONT,
                RavexFontRenderer.class.getResourceAsStream(path)
            );
            return font.deriveFont(size);
        } catch (Exception e) {
            return new java.awt.Font("SansSerif", java.awt.Font.PLAIN, (int) size);
        }
    }

    private static final int MAX_CACHE_SIZE = 256;
    private int texCounter = 0;

    private final Font awtFont;
    private final int lineHeight;
    private final Map<String, CachedTexture> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedTexture> eldest) {
            if (size() > MAX_CACHE_SIZE) {
                CachedTexture val = eldest.getValue();
                if (val.texture != null) {
                    try {
                        Minecraft.getInstance().getTextureManager().release(val.textureId);
                        val.texture.close();
                    } catch (Exception ignored) {}
                }
                return true;
            }
            return false;
        }
    };

    public RavexFontRenderer(Font awtFont, int lineHeight) {
        this.awtFont = awtFont;
        this.lineHeight = lineHeight;
    }

    public synchronized void drawString(GuiGraphics graphics, String text, int x, int y, int color) {
        if (text == null || text.isEmpty()) return;
        String key = text;
        CachedTexture cached = cache.get(key);
        if (cached == null) {
            cached = renderText(text);
            cache.put(key, cached);
        }
        if (cached.texture == null) return;
        DynamicTexture tex = cached.texture;
        GpuTextureView view = tex.getTextureView();
        GpuSampler sampler = tex.getSampler();
        ((GuiGraphicsAccessor) graphics).invokeSubmitBlit(
            RenderPipelines.GUI_TEXTURED, view, sampler,
            x, y, x + cached.width, y + cached.height,
            0f, 0f, 1f, 1f,
            color
        );
    }

    private CachedTexture renderText(String text) {
        Graphics2D g2d = null;
        FontMetrics metrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            .createGraphics().getFontMetrics(awtFont);
        int w = metrics.stringWidth(text);
        int h = metrics.getHeight();
        if (w <= 0) w = 1;
        if (h <= 0) h = 1;

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setFont(awtFont);
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, 0, metrics.getAscent());

        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        for (int px = 0; px < w; px++) {
            for (int py = 0; py < h; py++) {
                int argb = image.getRGB(px, py);
                int na = (argb >> 24) & 0xFF;
                int nr = (argb >> 16) & 0xFF;
                int ng = (argb >> 8) & 0xFF;
                int nb = argb & 0xFF;
                nativeImage.setPixelABGR(px, py, (na << 24) | (nb << 16) | (ng << 8) | nr);
            }
        }

        Identifier texId = Identifier.fromNamespaceAndPath("ravex", "fontcache/" + texCounter);
        texCounter++;
        DynamicTexture texture = new DynamicTexture(() -> "fontcache", nativeImage);
        Minecraft.getInstance().getTextureManager().register(texId, texture);
        if (g2d != null) g2d.dispose();
        return new CachedTexture(texture, texId, w, h);
    }

    public synchronized int width(String text) {
        if (text == null || text.isEmpty()) return 0;
        CachedTexture cached = cache.get(text);
        if (cached != null) return cached.width;
        try {
            FontMetrics metrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                .createGraphics().getFontMetrics(awtFont);
            return metrics.stringWidth(text);
        } catch (Throwable t) {
            System.err.println("[RaveX] Font width failed for '" + text + "': " + t);
            return 200;
        }
    }

    public int height() {
        return lineHeight;
    }

    private record CachedTexture(DynamicTexture texture, Identifier textureId, int width, int height) {}
}
