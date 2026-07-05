package ravex.utility.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class Render2DEngine {

    private static final int RRT_SIZE = 64;
    private static final Map<Integer, Identifier> RRT_CACHE = new HashMap<>();
    private static final Map<Integer, int[]> CORNER_EDGES = new HashMap<>();
    private static final Map<Integer, Identifier> SMOOTH_CIRCLE_CACHE = new HashMap<>();

    private static final int SHADOW_SIZE = 32;
    private static final Map<String, Identifier> SHADOW_CACHE = new HashMap<>();

    private static final Deque<int[]> SCISSOR_STACK = new ArrayDeque<>();

    public static void pushScissor(GuiGraphics graphics, int x, int y, int width, int height) {
        if (SCISSOR_STACK.isEmpty()) {
            graphics.enableScissor(x, y, x + width, y + height);
        } else {
            int[] parent = SCISSOR_STACK.peek();
            int nx = Math.max(parent[0], x);
            int ny = Math.max(parent[1], y);
            int nw = Math.min(parent[0] + parent[2], x + width) - nx;
            int nh = Math.min(parent[1] + parent[3], y + height) - ny;
            if (nw > 0 && nh > 0) {
                graphics.enableScissor(nx, ny, nx + nw, ny + nh);
            }
        }
        SCISSOR_STACK.push(new int[]{x, y, width, height});
    }

    public static void popScissor(GuiGraphics graphics) {
        if (!SCISSOR_STACK.isEmpty()) {
            SCISSOR_STACK.pop();
        }
        if (SCISSOR_STACK.isEmpty()) {
            graphics.disableScissor();
        } else {
            int[] parent = SCISSOR_STACK.peek();
            graphics.enableScissor(parent[0], parent[1], parent[0] + parent[2], parent[1] + parent[3]);
        }
    }

    private static int[] getCornerEdges(int r) {
        return CORNER_EDGES.computeIfAbsent(r, radius -> {
            int[] edges = new int[radius];
            for (int dy = 0; dy < radius; dy++) {
                float x = (float) Math.sqrt(radius * radius - (radius - dy) * (radius - dy));
                edges[dy] = Math.max(0, radius - 1 - (int) Math.floor(x));
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
            setLinearSampler(tex);
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
            setLinearSampler(tex);
            Identifier id = Identifier.fromNamespaceAndPath("ravex", "smooth_circle_" + d);
            Minecraft.getInstance().getTextureManager().register(id, tex);
            return id;
        });
    }

    private static void setLinearSampler(AbstractTexture tex) {
        try {
            Field f = AbstractTexture.class.getDeclaredField("sampler");
            f.setAccessible(true);
            GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            f.set(tex, sampler);
        } catch (Exception e) {
            ravex.RaveX.LOGGER.warn("[R2D] Failed to set sampler: {}", e.getMessage());
        }
    }

    public static void drawRect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;
        int a = (color >> 24) & 0xFF;
        if (a == 0) return;
        graphics.fill(x, y, x + width, y + height, color);
    }

    public static void drawRect(GuiGraphics graphics, float x, float y, float width, float height, int color) {
        drawRect(graphics, (int) x, (int) y, (int) Math.ceil(width), (int) Math.ceil(height), color);
    }

    public static void drawGradientRect(GuiGraphics graphics, int x, int y, int width, int height, int startColor, int endColor) {
        if (width <= 0 || height <= 0) return;
        graphics.fillGradient(x, y, x + width, y + height, startColor, endColor);
    }

    public static void drawGradientRectHorizontal(GuiGraphics graphics, int x, int y, int width, int height, int startColor, int endColor) {
        if (width <= 0 || height <= 0) return;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + width / 2f, y + height / 2f);
        graphics.pose().rotate(90f * 0.017453292f);
        graphics.fillGradient(-height / 2, -width / 2, height / 2, width / 2, startColor, endColor);
        graphics.pose().popMatrix();
    }

    public static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int thickness, int color) {
        if (thickness <= 0) return;
        drawRect(graphics, x, y, width, thickness, color);
        drawRect(graphics, x, y + height - thickness, width, thickness, color);
        drawRect(graphics, x, y + thickness, thickness, height - thickness * 2, color);
        drawRect(graphics, x + width - thickness, y + thickness, thickness, height - thickness * 2, color);
    }

    public static void drawRound(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        if (width <= 1 || height <= 1) { drawRect(graphics, x, y, width, height, color); return; }
        if (radius <= 0) { drawRect(graphics, x, y, width, height, color); return; }
        int maxR = Math.min(width, height) / 2;
        if (radius > maxR) radius = maxR;
        if (radius <= 0) { drawRect(graphics, x, y, width, height, color); return; }

        int a = (color >> 24) & 0xFF;
        if (a == 0) return;

        Identifier tex = getCornerTexture(radius);

        if (tex != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, radius, radius, RRT_SIZE, RRT_SIZE, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + width - radius, y, RRT_SIZE - radius, 0f, radius, radius, RRT_SIZE, RRT_SIZE, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y + height - radius, 0f, RRT_SIZE - radius, radius, radius, RRT_SIZE, RRT_SIZE, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + width - radius, y + height - radius, RRT_SIZE - radius, RRT_SIZE - radius, radius, radius, RRT_SIZE, RRT_SIZE, color);
            drawRect(graphics, x + radius, y, width - radius * 2, radius, color);
            drawRect(graphics, x + radius, y + height - radius, width - radius * 2, radius, color);
            drawRect(graphics, x, y + radius, width, height - radius * 2, color);
        } else {
            drawRect(graphics, x + radius, y, width - radius * 2, height, color);
            drawRect(graphics, x, y + radius, radius, height - radius * 2, color);
            drawRect(graphics, x + width - radius, y + radius, radius, height - radius * 2, color);
            int[] edges = getCornerEdges(radius);
            int x1 = x, x2 = x + width;
            int y1 = y, y2 = y + height;
            for (int dy = 0; dy < radius; dy++) {
                int xe = edges[dy];
                if (xe >= radius) continue;
                drawRect(graphics, x1 + xe, y1 + dy, radius - xe, 1, color);
                drawRect(graphics, x2 - radius, y1 + dy, radius - xe, 1, color);
                drawRect(graphics, x1 + xe, y2 - dy - 1, radius - xe, 1, color);
                drawRect(graphics, x2 - radius, y2 - dy - 1, radius - xe, 1, color);
            }
        }
    }

    public static void drawRound(GuiGraphics graphics, float x, float y, float w, float h, float r, int color) {
        drawRound(graphics, (int) x, (int) y, (int) Math.ceil(w), (int) Math.ceil(h), Math.round(r), color);
    }

    public static void drawSmoothRound(GuiGraphics graphics, float x, float y, float w, float h, float r, int color) {
        drawRound(graphics, x, y, w, h, r, color);
    }

    public static void drawRoundGradient(GuiGraphics graphics, int x, int y, int width, int height, int radius, int cTL, int cTR, int cBL, int cBR) {
        if (width <= 1 || height <= 1) return;
        int maxR = Math.min(width, height) / 2;
        if (radius > maxR) radius = maxR;
        if (radius <= 0) radius = 1;

        int cTM = ColorUtility.interpolate(cTL, cTR, 0.5f);
        int cBM = ColorUtility.interpolate(cBL, cBR, 0.5f);
        int cML = ColorUtility.interpolate(cTL, cBL, 0.5f);
        int cMR = ColorUtility.interpolate(cTR, cBR, 0.5f);

        drawRect(graphics, x + radius, y, width - radius * 2, radius, cTM);
        drawRect(graphics, x + radius, y + height - radius, width - radius * 2, radius, cBM);
        drawRect(graphics, x, y + radius, radius, height - radius * 2, cML);
        drawRect(graphics, x + width - radius, y + radius, radius, height - radius * 2, cMR);
        drawGradientRect(graphics, x + radius, y + radius, width - radius * 2, height - radius * 2,
            ColorUtility.interpolate(cTL, cTR, 0.5f), ColorUtility.interpolate(cBL, cBR, 0.5f));

        Identifier tex = getCornerTexture(radius);
        if (tex != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, radius, radius, RRT_SIZE, RRT_SIZE, cTL);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + width - radius, y, RRT_SIZE - radius, 0f, radius, radius, RRT_SIZE, RRT_SIZE, cTR);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y + height - radius, 0f, RRT_SIZE - radius, radius, radius, RRT_SIZE, RRT_SIZE, cBL);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + width - radius, y + height - radius, RRT_SIZE - radius, RRT_SIZE - radius, radius, radius, RRT_SIZE, RRT_SIZE, cBR);
        }
    }

    public static void drawRoundGradient(GuiGraphics graphics, int x, int y, int width, int height, int radius, Color c1, Color c2, Color c3, Color c4) {
        drawRoundGradient(graphics, x, y, width, height, radius, c1.getRGB(), c2.getRGB(), c3.getRGB(), c4.getRGB());
    }

    public static void drawRoundBorder(GuiGraphics graphics, int x, int y, int width, int height, int radius, int thickness, int color) {
        if (thickness <= 0) return;
        int outerAlpha = (color >> 24) & 0xFF;
        if (outerAlpha == 0) return;
        drawRound(graphics, x - thickness, y - thickness, width + thickness * 2, height + thickness * 2, radius + thickness, color);
        int innerColor = color & 0x00FFFFFF;
        drawRound(graphics, x, y, width, height, radius, innerColor);
    }

    public static void drawRoundBorder(GuiGraphics graphics, float x, float y, float w, float h, float r, float thickness, int color) {
        drawRoundBorder(graphics, (int) x, (int) y, (int) Math.ceil(w), (int) Math.ceil(h), Math.round(r), Math.round(thickness), color);
    }

    public static void fillCircle(GuiGraphics graphics, int x, int y, int radius, int color) {
        int d = radius * 2;
        Identifier tex = getSmoothCircle(d);
        if (tex != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x - radius, y - radius, 0f, 0f, d, d, d, d, color);
        }
    }

    public static void drawCircleOutline(GuiGraphics graphics, int x, int y, int radius, int thickness, int color) {
        int outer = radius + thickness;
        int inner = radius;
        fillCircle(graphics, x, y, outer, color);
        int innerColor = color & 0x00FFFFFF;
        fillCircle(graphics, x, y, inner, innerColor);
    }

    public static void drawArc(GuiGraphics graphics, int cx, int cy, int radius, int startAngle, int arcLength, int thickness, int color) {
        if (thickness <= 0 || arcLength <= 0 || radius <= 0) return;
        int a = (color >> 24) & 0xFF;
        if (a == 0) return;

        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);

        int segments = Math.max(4, arcLength / 3);
        float angleStep = arcLength / (float) segments;

        for (int i = 0; i < segments; i++) {
            float angle1 = (startAngle + i * angleStep) * 0.017453292f;
            float angle2 = (startAngle + (i + 1) * angleStep) * 0.017453292f;

            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2);
            float sin2 = (float) Math.sin(angle2);

            int x1 = Math.round(cos1 * (radius - thickness));
            int y1 = Math.round(sin1 * (radius - thickness));
            int x2 = Math.round(cos1 * radius);
            int y2 = Math.round(sin1 * radius);
            int x3 = Math.round(cos2 * radius);
            int y3 = Math.round(sin2 * radius);
            int x4 = Math.round(cos2 * (radius - thickness));
            int y4 = Math.round(sin2 * (radius - thickness));

            graphics.pose().pushMatrix();
            graphics.pose().translate(0, 0);
            drawTriangle(graphics, x1, y1, x2, y2, x3, y3, color);
            drawTriangle(graphics, x1, y1, x3, y3, x4, y4, color);
            graphics.pose().popMatrix();
        }
        graphics.pose().popMatrix();
    }

    private static void drawTriangle(GuiGraphics graphics, int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        int minX = Math.min(Math.min(x1, x2), x3);
        int minY = Math.min(Math.min(y1, y2), y3);
        int maxX = Math.max(Math.max(x1, x2), x3);
        int maxY = Math.max(Math.max(y1, y2), y3);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (pointInTriangle(x, y, x1, y1, x2, y2, x3, y3)) {
                    graphics.fill(x, y, x + 1, y + 1, color);
                }
            }
        }
    }

    private static boolean pointInTriangle(int px, int py, int x1, int y1, int x2, int y2, int x3, int y3) {
        float d1 = sign(px, py, x1, y1, x2, y2);
        float d2 = sign(px, py, x2, y2, x3, y3);
        float d3 = sign(px, py, x3, y3, x1, y1);
        boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);
        return !(hasNeg && hasPos);
    }

    private static float sign(int px, int py, int x1, int y1, int x2, int y2) {
        return (px - x2) * (y1 - y2) - (x1 - x2) * (py - y2);
    }

    public static void drawLine(GuiGraphics graphics, float x1, float y1, float x2, float y2, float width, int color) {
        int a = (color >> 24) & 0xFF;
        if (a == 0) return;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;
        dx /= len;
        dy /= len;
        float px = -dy * width / 2;
        float py = dx * width / 2;

        int ix1 = (int) (x1 + px);
        int iy1 = (int) (y1 + py);
        int ix2 = (int) (x1 - px);
        int iy2 = (int) (y1 - py);
        int ix3 = (int) (x2 - px);
        int iy3 = (int) (y2 - py);
        int ix4 = (int) (x2 + px);
        int iy4 = (int) (y2 + py);

        drawTriangle(graphics, ix1, iy1, ix2, iy2, ix3, iy3, color);
        drawTriangle(graphics, ix1, iy1, ix3, iy3, ix4, iy4, color);
    }

    public static void drawBlurredShadow(GuiGraphics graphics, float x, float y, float width, float height, int radius, int color) {
        int a = (color >> 24) & 0xFF;
        if (a == 0) return;
        float softness = Math.max(1, radius);

        String key = (int) width + "_" + (int) height + "_" + (int) softness;
        Identifier shadowTex = SHADOW_CACHE.get(key);

        if (shadowTex == null) {
            int texW = (int) (width + softness * 4);
            int texH = (int) (height + softness * 4);
            texW = Math.max(1, texW);
            texH = Math.max(1, texH);

            NativeImage img = new NativeImage(texW, texH, false);
            float cx = texW / 2f;
            float cy = texH / 2f;
            float halfW = width / 2f;
            float halfH = height / 2f;

            for (int py = 0; py < texH; py++) {
                for (int px = 0; px < texW; px++) {
                    float dx = Math.abs(px - cx) - halfW;
                    float dy = Math.abs(py - cy) - halfH;
                    float dist = (float) Math.sqrt(Math.max(0, dx) * Math.max(0, dx) + Math.max(0, dy) * Math.max(0, dy));
                    float alpha = Math.max(0, 1 - dist / softness);
                    alpha = alpha * alpha * (3 - 2 * alpha);
                    int aa = Math.min(255, Math.round(alpha * 255));
                    if (aa > 0) {
                        img.setPixel(px, py, (aa << 24) | 0x00FFFFFF);
                    }
                }
            }

            DynamicTexture tex = new DynamicTexture(() -> "shadow_" + key, img);
            setLinearSampler(tex);
            Identifier id = Identifier.fromNamespaceAndPath("ravex", "shadow_" + key);
            Minecraft.getInstance().getTextureManager().register(id, tex);
            SHADOW_CACHE.put(key, id);
            shadowTex = id;
        }

        if (shadowTex != null) {
            int drawX = (int) (x - softness * 2);
            int drawY = (int) (y - softness * 2);
            int drawW = (int) (width + softness * 4);
            int drawH = (int) (height + softness * 4);
            graphics.blit(RenderPipelines.GUI_TEXTURED, shadowTex, drawX, drawY,
                0f, 0f, drawW, drawH, drawW, drawH, color);
        }
    }



    public static void drawRoundedRectWithBorder(GuiGraphics graphics, int x, int y, int width, int height, int radius, int fillColor, int borderColor, int borderWidth) {
        drawRound(graphics, x, y, width, height, radius, fillColor);
        drawRoundBorder(graphics, x, y, width, height, radius, borderWidth, borderColor);
    }

    public static void drawCheckmark(GuiGraphics graphics, int x, int y, int size, int color) {
        int s = size / 3;
        int x1 = x, y1 = y + s;
        int x2 = x + s, y2 = y + s * 2;
        int x3 = x + s * 2, y3 = y;
        drawLine(graphics, x1, y1, x2, y2, 2f, color);
        drawLine(graphics, x2, y2, x3, y3, 2f, color);
    }

    public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static int injectAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    public static int applyOpacity(int color, float opacity) {
        int a = Math.max(0, Math.min(255, Math.round(((color >> 24) & 0xFF) * opacity)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    public static int interpolateColorInt(int color1, int color2, float amount) {
        return ColorUtility.interpolate(color1, color2, amount);
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        int c = ColorUtility.interpolate(color1.getRGB(), color2.getRGB(), amount);
        return new Color(c, true);
    }

    public static int rainbowInt(int speed, int index, float saturation, float brightness, float opacity) {
        return ColorUtility.rainbow(speed, index, saturation, brightness, opacity);
    }

    public static int fadeInt(int speed, int index, int color, float alpha) {
        return ColorUtility.fade(speed, index, color, alpha);
    }

    public static int twoColorEffectInt(int cl1, int cl2, double speed, double count) {
        return ColorUtility.twoColor(cl1, cl2, speed, count);
    }

    public static Color skyRainbow(int speed, int index) {
        int c = ColorUtility.rainbow(speed, index, 0.5f, 1.0f, 1.0f);
        return new Color(c, true);
    }

    public static Color rainbow(int speed, int index, float saturation, float brightness, float opacity) {
        int c = ColorUtility.rainbow(speed, index, saturation, brightness, opacity);
        return new Color(c, true);
    }

    public static Color fade(int speed, int index, Color color, float alpha) {
        int c = ColorUtility.fade(speed, index, color.getRGB(), alpha);
        return new Color(c, true);
    }

    public static Color twoColorEffect(Color cl1, Color cl2, double speed, double count) {
        int c = ColorUtility.twoColor(cl1.getRGB(), cl2.getRGB(), speed, count);
        return new Color(c, true);
    }

    public static Color interpolateColorsBackAndForth(int speed, int index, Color start, Color end, boolean trueColor) {
        int c = ColorUtility.backAndForth(speed, index, start.getRGB(), end.getRGB());
        return new Color(c, true);
    }

    public static Color getAnalogousColor(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return new Color(Color.HSBtoRGB(hsb[0] - 0.84f, hsb[1], hsb[2]));
    }

    public static Color darker(Color color, float factor) {
        return new Color(
            Math.max((int) (color.getRed() * factor), 0),
            Math.max((int) (color.getGreen() * factor), 0),
            Math.max((int) (color.getBlue() * factor), 0),
            color.getAlpha()
        );
    }

    public static Color applyOpacity(Color color, float opacity) {
        int a = Math.max(0, Math.min(255, (int) (color.getAlpha() * opacity)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    public static Color injectAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return (int) (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
        return (float) (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static float fastAnimation(float current, float target, float speed) {
        return current + (target - current) * Math.min(1, speed * 0.05f);
    }
}
