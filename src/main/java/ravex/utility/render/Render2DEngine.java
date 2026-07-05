package ravex.utility.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.awt.*;
import java.util.*;

public class Render2DEngine {

    private static final int RRT_SIZE = 64;
    private static final Map<Integer, Identifier> RRT_CACHE = new HashMap<>();
    private static final Map<Integer, int[]> CORNER_EDGES = new HashMap<>();
    private static final Map<Integer, Identifier> SMOOTH_CIRCLE_CACHE = new HashMap<>();

    private static int[] getCornerEdges(int r) {
        return CORNER_EDGES.computeIfAbsent(r, radius -> {
            int[] edges = new int[radius];
            for (int dy = 0; dy < radius; dy++) {
                edges[dy] = Math.max(0, radius - 1 - dy);
            }
            return edges;
        });
    }

    private static Identifier getCornerTexture(int r) {
        return RRT_CACHE.computeIfAbsent(r, radius -> {
            NativeImage img = new NativeImage(RRT_SIZE, RRT_SIZE, false);
            int ss = 4;
            float r2 = (radius - 0.5f) * (radius - 0.5f);
            for (int y = 0; y < RRT_SIZE; y++) {
                for (int x = 0; x < RRT_SIZE; x++) {
                    int a = 255;
                    if (x < radius && y < radius) {
                        a = calcCornerAA(x, y, radius, radius, ss, r2);
                    } else if (x >= RRT_SIZE - radius && y < radius) {
                        a = calcCornerAA(x, y, RRT_SIZE - radius, radius, ss, r2);
                    } else if (x < radius && y >= RRT_SIZE - radius) {
                        a = calcCornerAA(x, y, radius, RRT_SIZE - radius, ss, r2);
                    } else if (x >= RRT_SIZE - radius && y >= RRT_SIZE - radius) {
                        a = calcCornerAA(x, y, RRT_SIZE - radius, RRT_SIZE - radius, ss, r2);
                    }
                    img.setPixel(x, y, (a << 24) | 0x00FFFFFF);
                }
            }
            DynamicTexture tex = new DynamicTexture(() -> "rrt_" + radius, img);
            try {
                var field = AbstractTexture.class.getDeclaredField("sampler");
                field.setAccessible(true);
                var sampler = com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                field.set(tex, sampler);
            } catch (Exception e) {
                ravex.RaveX.LOGGER.warn("[R2D] Failed to set sampler: {}", e.getMessage());
            }
            Identifier id = Identifier.fromNamespaceAndPath("ravex", "rrt_" + radius);
            Minecraft.getInstance().getTextureManager().register(id, tex);
            return id;
        });
    }

    private static int calcCornerAA(int x, int y, float cx, float cy, int ss, float r2) {
        int total = 0;
        for (int sy = 0; sy < ss; sy++) {
            float py = y + (sy + 0.5f) / ss - cy;
            float py2 = py * py;
            for (int sx = 0; sx < ss; sx++) {
                float px = x + (sx + 0.5f) / ss - cx;
                if (px * px + py2 <= r2) total++;
            }
        }
        return Math.min(255, total * 255 / (ss * ss));
    }

    public static Identifier getSmoothCircle(int diameter) {
        return SMOOTH_CIRCLE_CACHE.computeIfAbsent(diameter, d -> {
            int ss = 8;
            int ssSize = d * ss;
            NativeImage ssImg = new NativeImage(ssSize, ssSize, false);
            int cx = ssSize / 2, cy = ssSize / 2;
            float rad2 = (d / 2f - 0.5f) * (d / 2f - 0.5f) * ss * ss;
            for (int y = 0; y < ssSize; y++) {
                for (int x = 0; x < ssSize; x++) {
                    float dx = x - cx + 0.5f;
                    float dy = y - cy + 0.5f;
                    boolean inside = dx * dx + dy * dy <= rad2;
                    ssImg.setPixel(x, y, inside ? 0xFFFFFFFF : 0x00000000);
                }
            }
            NativeImage out = new NativeImage(d, d, false);
            for (int y = 0; y < d; y++) {
                for (int x = 0; x < d; x++) {
                    int totalA = 0;
                    for (int sy = 0; sy < ss; sy++) {
                        for (int sx = 0; sx < ss; sx++) {
                            int px = ssImg.getPixel(x * ss + sx, y * ss + sy);
                            totalA += (px >> 24) & 0xFF;
                        }
                    }
                    int avgA = totalA / (ss * ss);
                    out.setPixel(x, y, (avgA << 24) | 0x00FFFFFF);
                }
            }
            DynamicTexture tex = new DynamicTexture(() -> "smooth_circle_" + d, out);
            try {
                var field = AbstractTexture.class.getDeclaredField("sampler");
                field.setAccessible(true);
                var sampler = com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                field.set(tex, sampler);
            } catch (Exception e) {
                ravex.RaveX.LOGGER.warn("[R2D] Failed to set circle sampler: {}", e.getMessage());
            }
            Identifier id = Identifier.fromNamespaceAndPath("ravex", "smooth_circle_" + d);
            Minecraft.getInstance().getTextureManager().register(id, tex);
            return id;
        });
    }

    public static void drawRect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, color);
    }

    public static void drawGradientRect(GuiGraphics graphics, int x, int y, int width, int height, int startColor, int endColor) {
        graphics.fillGradient(x, y, x + width, y + height, startColor, endColor);
    }

    public static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int thickness, int color) {
        graphics.fill(x, y, x + width, y + thickness, color);
        graphics.fill(x, y + height - thickness, x + width, y + height, color);
        graphics.fill(x, y + thickness, x + thickness, y + height - thickness, color);
        graphics.fill(x + width - thickness, y + thickness, x + width, y + height - thickness, color);
    }

    public static void drawRound(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        if (width <= 1 || height <= 1) { graphics.fill(x, y, x + width, y + height, color); return; }
        if (radius <= 0) { graphics.fill(x, y, x + width, y + height, color); return; }
        int maxR = Math.min(width, height) / 2;
        if (radius > maxR) radius = maxR;
        if (radius <= 0) { graphics.fill(x, y, x + width, y + height, color); return; }

        int a = (color >> 24) & 0xFF;
        if (a == 0) return;

        Identifier tex = getCornerTexture(radius);

        if (tex != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, radius, radius, RRT_SIZE, RRT_SIZE, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + width - radius, y, RRT_SIZE - radius, 0f, radius, radius, RRT_SIZE, RRT_SIZE, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y + height - radius, 0f, RRT_SIZE - radius, radius, radius, RRT_SIZE, RRT_SIZE, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + width - radius, y + height - radius, RRT_SIZE - radius, RRT_SIZE - radius, radius, radius, RRT_SIZE, RRT_SIZE, color);
            graphics.fill(x + radius, y, x + width - radius, y + radius, color);
            graphics.fill(x + radius, y + height - radius, x + width - radius, y + height, color);
            graphics.fill(x, y + radius, x + width, y + height - radius, color);
        } else {
            graphics.fill(x + radius, y, x + width - radius, y + height, color);
            graphics.fill(x, y + radius, x + radius, y + height - radius, color);
            graphics.fill(x + width - radius, y + radius, x + width, y + height - radius, color);
            int[] edges = getCornerEdges(radius);
            int x1 = x, x2 = x + width;
            int y1 = y, y2 = y + height;
            for (int dy = 0; dy < radius; dy++) {
                int xe = edges[dy];
                if (xe >= radius) continue;
                graphics.fill(x1 + xe, y1 + dy, x1 + radius, y1 + dy + 1, color);
                graphics.fill(x2 - radius, y1 + dy, x2 - xe, y1 + dy + 1, color);
                graphics.fill(x1 + xe, y2 - dy - 1, x1 + radius, y2 - dy, color);
                graphics.fill(x2 - radius, y2 - dy - 1, x2 - xe, y2 - dy, color);
            }
        }
    }

    public static void drawRound(GuiGraphics graphics, int x, int y, int width, int height, int radius, int r, int g, int b, int a) {
        int color = (a << 24) | (r << 16) | (g << 8) | b;
        drawRound(graphics, x, y, width, height, radius, color);
    }

    public static void drawSmoothRound(GuiGraphics graphics, float x, float y, float w, float h, float r, int color) {
        if (w <= 0f || h <= 0f) return;
        r = Math.max(0f, Math.min(r, Math.min(w, h) / 2.0f));
        if (r <= 0f) { graphics.fill((int) x, (int) y, (int) (x + w), (int) (y + h), color); return; }
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        int iw = (int) Math.ceil(x + w) - ix;
        int ih = (int) Math.ceil(y + h) - iy;
        drawRound(graphics, ix, iy, iw, ih, Math.round(r), color);
    }

    public static void drawRoundGradient(GuiGraphics graphics, int x, int y, int width, int height, int radius, Color c1, Color c2, Color c3, Color c4) {
        graphics.fillGradient(x, y, x + width, y + height, c1.getRGB(), c3.getRGB());
    }

    public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static Color injectAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    public static Color applyOpacity(Color color, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
    }

    public static int applyOpacity(int colorInt, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        int a = (colorInt >> 24) & 0xFF;
        int r = (colorInt >> 16) & 0xFF;
        int g = (colorInt >> 8) & 0xFF;
        int b = colorInt & 0xFF;
        return ((int) (a * opacity) << 24) | (r << 16) | (g << 8) | b;
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        return new Color(
            interpolateInt(color1.getRed(), color2.getRed(), amount),
            interpolateInt(color1.getGreen(), color2.getGreen(), amount),
            interpolateInt(color1.getBlue(), color2.getBlue(), amount),
            interpolateInt(color1.getAlpha(), color2.getAlpha(), amount)
        );
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return (int) (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
        return (float) (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static Color darker(Color color, float factor) {
        return new Color(
            Math.max((int) (color.getRed() * factor), 0),
            Math.max((int) (color.getGreen() * factor), 0),
            Math.max((int) (color.getBlue() * factor), 0),
            color.getAlpha()
        );
    }

    public static int rainbowInt(int speed, int index, float saturation, float brightness, float opacity) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        float hue = angle / 360f;
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        int a = Math.max(0, Math.min(255, (int) (opacity * 255)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static int fadeInt(int speed, int index, int color, float alpha) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], angle / 360f);
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static int twoColorEffectInt(int cl1, int cl2, double speed, double count) {
        int angle = (int) (((System.currentTimeMillis()) / speed + count) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return interpolateColorCInt(cl1, cl2, angle / 360f);
    }

    public static int interpolateColorCInt(int color1, int color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        int a1 = (color1 >> 24) & 0xFF, r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF, r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * amount);
        int r = (int) (r1 + (r2 - r1) * amount);
        int g = (int) (g1 + (g2 - g1) * amount);
        int b = (int) (b1 + (b2 - b1) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static Color skyRainbow(int speed, int index) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        return Color.getHSBColor((float) angle / 360f, 0.5f, 1.0f);
    }

    public static Color rainbow(int speed, int index, float saturation, float brightness, float opacity) {
        int argb = rainbowInt(speed, index, saturation, brightness, opacity);
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
    }

    public static Color fade(int speed, int index, Color color, float alpha) {
        int argb = fadeInt(speed, index, color.getRGB(), alpha);
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
    }

    public static Color twoColorEffect(Color cl1, Color cl2, double speed, double count) {
        int argb = twoColorEffectInt(cl1.getRGB(), cl2.getRGB(), speed, count);
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
    }

    public static Color interpolateColorsBackAndForth(int speed, int index, Color start, Color end, boolean trueColor) {
        int angle = (int) (((System.currentTimeMillis()) / speed + index) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return interpolateColorC(start, end, angle / 360f);
    }

    public static Color getAnalogousColor(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return new Color(Color.HSBtoRGB(hsb[0] - 0.84f, hsb[1], hsb[2]));
    }

    public static float fastAnimation(float current, float target, float speed) {
        return current + (target - current) * Math.min(1, speed * 0.05f);
    }
}
