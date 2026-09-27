package ravex.utility.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.lang.reflect.Field;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Render2DUtility {

    private static final int RRT_SIZE = 128;
    private static Identifier rrtHdTex = null;
    private static final Map<String, Identifier> ROUND_RECT_OUTLINE_CACHE = new HashMap<>();
    private static final Map<Integer, int[]> CORNER_EDGES = new HashMap<>();
    private static final Map<String, Identifier> SMOOTH_RING_CACHE = new HashMap<>();
    private static final Map<String, Identifier> SMOOTH_TRI_CACHE = new HashMap<>();
    private static Identifier smoothBarTex = null;

    private static final int SHADOW_SIZE = 32;
    private static final Map<String, Identifier> SHADOW_CACHE = new HashMap<>();

    private static int SCISSOR_DEPTH = 0;

    public static void pushScissor(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.enableScissor(x, y, x + Math.max(0, width), y + Math.max(0, height));
        SCISSOR_DEPTH++;
    }

    public static void popScissor(GuiGraphics graphics) {
        if (SCISSOR_DEPTH <= 0) {
            return;
        }
        SCISSOR_DEPTH--;
        try {
            graphics.disableScissor();
        } catch (IllegalStateException e) {
            SCISSOR_DEPTH = 0;
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
        if (rrtHdTex == null) {
            int size = 128;
            NativeImage img = new NativeImage(size, size, false);
            float cx = size / 2f;
            float cy = size / 2f;
            float rVal = size / 2f - 0.5f;
            float r2 = rVal * rVal;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int a = calcCornerAA(x, y, cx, cy, 0, r2);
                    img.setPixel(x, y, (a << 24) | 0x00FFFFFF);
                }
            }
            DynamicTexture tex = new DynamicTexture(() -> "rrt_hd", img);
            setLinearSampler(tex);
            rrtHdTex = Identifier.fromNamespaceAndPath("ravex", "rrt_hd");
            MinecraftWrapper.getWrapper().getTextureManager().register(rrtHdTex, tex);
        }
        return rrtHdTex;
    }

    private static Identifier invertedCornerTex = null;

    private static Identifier getInvertedCornerTexture() {
        if (invertedCornerTex == null) {
            int size = 128;
            NativeImage img = new NativeImage(size, size, false);
            int ss = 4;
            float cx = size / 2f;
            float cy = size / 2f;
            float rVal = size / 2f - 0.5f;
            float r2 = rVal * rVal;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int a = 255 - calcCornerAA(x, y, cx, cy, ss, r2);
                    img.setPixel(x, y, (a << 24) | 0x00FFFFFF);
                }
            }
            DynamicTexture tex = new DynamicTexture(() -> "rrt_hd_inv", img);
            setLinearSampler(tex);
            invertedCornerTex = Identifier.fromNamespaceAndPath("ravex", "rrt_hd_inv");
            MinecraftWrapper.getWrapper().getTextureManager().register(invertedCornerTex, tex);
        }
        return invertedCornerTex;
    }

    private static Identifier getCornerOutlineTexture(int radius, int thickness) {
        String key = radius + "_" + thickness;
        return ROUND_RECT_OUTLINE_CACHE.computeIfAbsent(key, k -> {
            int size = 128;
            NativeImage img = new NativeImage(size, size, false);
            int ss = 4;
            float cx = size / 2f;
            float cy = size / 2f;
            float outerR = size / 2f - 0.5f;
            float texThickness = (size / 2f) * thickness / (float) radius;
            float innerR = Math.max(0f, outerR - texThickness);
            float outerR2 = outerR * outerR;
            float innerR2 = innerR * innerR;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int a = calcCornerOutlineAA(x, y, cx, cy, ss, outerR2, innerR2);
                    img.setPixel(x, y, (a << 24) | 0x00FFFFFF);
                }
            }
            DynamicTexture tex = new DynamicTexture(() -> "rro_" + key, img);
            setLinearSampler(tex);
            Identifier id = Identifier.fromNamespaceAndPath("ravex", "rro_" + key);
            MinecraftWrapper.getWrapper().getTextureManager().register(id, tex);
            return id;
        });
    }

    private static int calcCornerOutlineAA(int x, int y, float cx, float cy, int ss, float outerR2, float innerR2) {
        int total = 0;
        for (int sy = 0; sy < ss; sy++) {
            float py = y + (sy + 0.5f) / ss - cy;
            float py2 = py * py;
            for (int sx = 0; sx < ss; sx++) {
                float px = x + (sx + 0.5f) / ss - cx;
                float dist2 = px * px + py2;
                if (dist2 <= outerR2 && dist2 >= innerR2) {
                    total++;
                }
            }
        }
        return Math.min(255, total * 255 / (ss * ss));
    }

    private static int calcCornerAA(int x, int y, float cx, float cy, int ss, float r2) {
        float dx = x + 0.5f - cx;
        float dy = y + 0.5f - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float r = (float) Math.sqrt(r2);
        float alpha = (r + 0.5f) - dist;
        if (alpha <= 0f) return 0;
        if (alpha >= 1f) return 255;
        return Math.round(alpha * 255);
    }

    private static Identifier smoothCircleTex = null;

    private static Identifier chevronTex = null;

    public static Identifier getChevron() {
        if (chevronTex == null) {
            int size = 128;
            NativeImage img = new NativeImage(size, size, false);
            float cx = size / 2f;
            float cy = size / 2f;
            float halfW = size * 0.16f;
            float halfH = size * 0.26f;
            float tipX = cx + halfW;
            float tipY = cy;
            float upX = cx - halfW;
            float upY = cy - halfH;
            float dnX = cx - halfW;
            float dnY = cy + halfH;
            float thickness = size * 0.08f;
            float hw = thickness / 2f;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float px = x + 0.5f;
                    float py = y + 0.5f;
                    float d1 = distToSeg(px, py, upX, upY, tipX, tipY);
                    float d2 = distToSeg(px, py, dnX, dnY, tipX, tipY);
                    float d = Math.min(d1, d2);
                    float delta = d - hw;
                    float alpha;
                    if (delta <= -0.75f) {
                        alpha = 1f;
                    } else if (delta >= 0.75f) {
                        alpha = 0f;
                    } else {
                        alpha = 0.5f - delta / 1.5f;
                    }
                    int a = (int) (alpha * 255);
                    img.setPixel(x, y, (a << 24) | 0x00FFFFFF);
                }
            }
            DynamicTexture tex = new DynamicTexture(() -> "chevron", img);
            setLinearSampler(tex);
            chevronTex = Identifier.fromNamespaceAndPath("ravex", "chevron_gen");
            MinecraftWrapper.getWrapper().getTextureManager().register(chevronTex, tex);
        }
        return chevronTex;
    }

    public static void drawChevron(GuiGraphics graphics, float cx, float cy, float size, float angleDeg, int color) {
        int a = (color >> 24) & 0xFF;
        if (a == 0) return;
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        if (Math.abs(angleDeg) > 0.01f) {
            pose.rotate((float) Math.toRadians(angleDeg));
        }
        pose.translate(-size / 2f, -size / 2f);
        int isz = Math.round(size);
        graphics.blit(RenderPipelines.GUI_TEXTURED, getChevron(), 0, 0, 0f, 0f, isz, isz, isz, isz, color);
        pose.popMatrix();
    }

    private static float distToSeg(float px, float py, float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        float lenSq = dx * dx + dy * dy;
        if (lenSq < 0.001f) return (float) Math.sqrt((px - ax) * (px - ax) + (py - ay) * (py - ay));
        float t = Math.max(0f, Math.min(1f, ((px - ax) * dx + (py - ay) * dy) / lenSq));
        float projX = ax + t * dx;
        float projY = ay + t * dy;
        float ex = px - projX;
        float ey = py - projY;
        return (float) Math.sqrt(ex * ex + ey * ey);
    }

    public static Identifier getSmoothCircle() {
        if (smoothCircleTex == null) {
            int size = 128;
            NativeImage img = new NativeImage(size, size, false);
            int ss = 8;
            float cx = size / 2f;
            float cy = size / 2f;
            float rVal = size / 2f - 0.5f;
            float r2 = rVal * rVal;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int a = calcCornerAA(x, y, cx, cy, ss, r2);
                    img.setPixel(x, y, (a << 24) | 0x00FFFFFF);
                }
            }
            DynamicTexture tex = new DynamicTexture(() -> "smooth_circle_hd", img);
            setLinearSampler(tex);
            smoothCircleTex = Identifier.fromNamespaceAndPath("ravex", "smooth_circle_hd");
            MinecraftWrapper.getWrapper().getTextureManager().register(smoothCircleTex, tex);
        }
        return smoothCircleTex;
    }

    public static Identifier getSmoothRing(float thicknessRatio) {
        String key = String.format("%.2f", thicknessRatio);
        return SMOOTH_RING_CACHE.computeIfAbsent(key, k -> {
            int size = 128;
            NativeImage img = new NativeImage(size, size, false);
            int ss = 8;
            float cx = size / 2f;
            float cy = size / 2f;
            float outerR = size / 2f - 0.5f;
            float innerR = Math.max(0f, outerR - (size / 2f) * thicknessRatio);
            float outerR2 = outerR * outerR;
            float innerR2 = innerR * innerR;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int a = calcCornerOutlineAA(x, y, cx, cy, ss, outerR2, innerR2);
                    img.setPixel(x, y, (a << 24) | 0x00FFFFFF);
                }
            }
            DynamicTexture tex = new DynamicTexture(() -> "smooth_ring_" + key, img);
            setLinearSampler(tex);
            Identifier id = Identifier.fromNamespaceAndPath("ravex", "smooth_ring_" + key);
            MinecraftWrapper.getWrapper().getTextureManager().register(id, tex);
            return id;
        });
    }

    public static Identifier getSmoothTriangle(float thicknessRatio) {
        String key = String.format("%.2f", thicknessRatio);
        return SMOOTH_TRI_CACHE.computeIfAbsent(key, k -> {
            int size = 128;
            NativeImage img = new NativeImage(size, size, false);
            int ss = 8;
            float tipX = size * 0.5f;
            float tipY = 0f;
            float leftX = size * 0.1f;
            float leftY = size * 0.8f;
            float rightX = size * 0.9f;
            float rightY = size * 0.8f;
            float maxDist = Math.max(1f, thicknessRatio * size * 0.5f);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int a = calcTriangleOutlineAA(x, y, ss, tipX, tipY, leftX, leftY, rightX, rightY, maxDist);
                    img.setPixel(x, y, (a << 24) | 0x00FFFFFF);
                }
            }
            DynamicTexture tex = new DynamicTexture(() -> "smooth_tri_" + key, img);
            setLinearSampler(tex);
            Identifier id = Identifier.fromNamespaceAndPath("ravex", "smooth_tri_" + key);
            MinecraftWrapper.getWrapper().getTextureManager().register(id, tex);
            return id;
        });
    }

    private static int calcTriangleOutlineAA(int px, int py, int ss,
            float ax, float ay, float bx, float by, float cx, float cy, float maxDist) {
        int total = 0;
        for (int sy = 0; sy < ss; sy++) {
            float fy = py + (sy + 0.5f) / ss;
            for (int sx = 0; sx < ss; sx++) {
                float fx = px + (sx + 0.5f) / ss;
                if (!pointInTriangle(fx, fy, ax, ay, bx, by, cx, cy)) continue;
                float d = Math.min(
                        distToSeg(fx, fy, ax, ay, bx, by),
                        Math.min(distToSeg(fx, fy, bx, by, cx, cy),
                                distToSeg(fx, fy, cx, cy, ax, ay)));
                if (d <= maxDist) total++;
            }
        }
        return Math.min(255, total * 255 / (ss * ss));
    }

    public static Identifier getSmoothBar() {
        if (smoothBarTex == null) {
            int w = 64;
            int h = 16;
            NativeImage img = new NativeImage(w, h, false);
            int ss = 4;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int cov = 0;
                    for (int sy = 0; sy < ss; sy++) {
                        float fy = y + (sy + 0.5f) / ss;
                        for (int sx = 0; sx < ss; sx++) {
                            float fx = x + (sx + 0.5f) / ss;
                            if (fx > 0.5f && fx < w - 0.5f && fy > 0.5f && fy < h - 0.5f) cov++;
                        }
                    }
                    int a = Math.min(255, cov * 255 / (ss * ss));
                    img.setPixel(x, y, (a << 24) | 0x00FFFFFF);
                }
            }
            DynamicTexture tex = new DynamicTexture(() -> "smooth_bar", img);
            setLinearSampler(tex);
            smoothBarTex = Identifier.fromNamespaceAndPath("ravex", "smooth_bar");
            MinecraftWrapper.getWrapper().getTextureManager().register(smoothBarTex, tex);
        }
        return smoothBarTex;
    }

    public static void setLinearSampler(AbstractTexture tex) {
        try {
            GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            for (Field f : AbstractTexture.class.getDeclaredFields()) {
                if (GpuSampler.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    f.set(tex, sampler);
                    return;
                }
            }
            ravex.RaveX.LOGGER.warn("[R2D] No GpuSampler field found on AbstractTexture");
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
        graphics.pose().rotate(-90f * 0.017453292f);
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
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, radius, radius, 64, 64, 128, 128, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + width - radius, y, 64f, 0f, radius, radius, 64, 64, 128, 128, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y + height - radius, 0f, 64f, radius, radius, 64, 64, 128, 128, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + width - radius, y + height - radius, 64f, 64f, radius, radius, 64, 64, 128, 128, color);
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

    public static void drawRoundCutout(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        if (width <= 1 || height <= 1) return;
        int maxR = Math.min(width, height) / 2;
        if (radius > maxR) radius = maxR;
        if (radius <= 0) return;
        int a = (color >> 24) & 0xFF;
        if (a == 0) return;

        Identifier tex = getInvertedCornerTexture();
        if (tex != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, radius, radius, 64, 64, 128, 128, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + width - radius, y, 64f, 0f, radius, radius, 64, 64, 128, 128, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y + height - radius, 0f, 64f, radius, radius, 64, 64, 128, 128, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + width - radius, y + height - radius, 64f, 64f, radius, radius, 64, 64, 128, 128, color);
        }
        drawRect(graphics, x + radius, y, width - radius * 2, radius, color);
        drawRect(graphics, x + radius, y + height - radius, width - radius * 2, radius, color);
        drawRect(graphics, x, y + radius, radius, height - radius * 2, color);
        drawRect(graphics, x + width - radius, y + radius, radius, height - radius * 2, color);
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
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, radius, radius, 64, 64, 128, 128, cTL);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + width - radius, y, 64f, 0f, radius, radius, 64, 64, 128, 128, cTR);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y + height - radius, 0f, 64f, radius, radius, 64, 64, 128, 128, cBL);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + width - radius, y + height - radius, 64f, 64f, radius, radius, 64, 64, 128, 128, cBR);
        }
    }

    public static void drawRoundGradient(GuiGraphics graphics, int x, int y, int width, int height, int radius, Color c1, Color c2, Color c3, Color c4) {
        drawRoundGradient(graphics, x, y, width, height, radius, c1.getRGB(), c2.getRGB(), c3.getRGB(), c4.getRGB());
    }

    public static void drawRoundBorder(GuiGraphics graphics, int x, int y, int width, int height, int radius, int thickness, int color) {
        if (thickness <= 0) return;
        int outerAlpha = (color >> 24) & 0xFF;
        if (outerAlpha == 0) return;

        int maxR = Math.min(width, height) / 2;
        if (radius > maxR) radius = maxR;
        if (radius <= 0) {
            drawBorder(graphics, x, y, width, height, thickness, color);
            return;
        }

        Identifier tex = getCornerOutlineTexture(radius, thickness);
        if (tex != null) {

            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, radius, radius, 64, 64, 128, 128, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + width - radius, y, 64f, 0f, radius, radius, 64, 64, 128, 128, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y + height - radius, 0f, 64f, radius, radius, 64, 64, 128, 128, color);
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + width - radius, y + height - radius, 64f, 64f, radius, radius, 64, 64, 128, 128, color);


            drawRect(graphics, x + radius, y, width - radius * 2, thickness, color);
            drawRect(graphics, x + radius, y + height - thickness, width - radius * 2, thickness, color);
            drawRect(graphics, x, y + radius, thickness, height - radius * 2, color);
            drawRect(graphics, x + width - thickness, y + radius, thickness, height - radius * 2, color);
        }
    }

    public static void drawRoundBorder(GuiGraphics graphics, float x, float y, float w, float h, float r, float thickness, int color) {
        drawRoundBorder(graphics, (int) x, (int) y, (int) Math.ceil(w), (int) Math.ceil(h), Math.round(r), Math.round(thickness), color);
    }

    private static final Map<String, Identifier> SMOOTH_BORDER_CACHE = new HashMap<>();

    public static void drawSmoothRoundOutline(GuiGraphics graphics, int x, int y, int width, int height, int radius, int thickness, int color) {
        if (width <= 1 || height <= 1 || thickness <= 0) return;
        int a = (color >> 24) & 0xFF;
        if (a == 0) return;
        int maxR = Math.min(width, height) / 2;
        if (radius > maxR) radius = maxR;
        if (radius <= 0) {
            drawBorder(graphics, x, y, width, height, thickness, color);
            return;
        }
        int ss = 4;
        String key = width + "_" + height + "_" + radius + "_" + thickness;
        Identifier tex = SMOOTH_BORDER_CACHE.get(key);
        if (tex == null) {
            int sw = width * ss;
            int sh = height * ss;
            NativeImage img = new NativeImage(sw, sh, true);
            float r = radius * ss;
            float half = thickness * ss / 2f;
            float plateau = Math.max(0f, half - 1f);
            for (int py = 0; py < sh; py++) {
                for (int px = 0; px < sw; px++) {
                    float fx = px + 0.5f;
                    float fy = py + 0.5f;
                    float cx = Math.max(r, Math.min(sw - r, fx));
                    float cy = Math.max(r, Math.min(sh - r, fy));
                    float dx = fx - cx;
                    float dy = fy - cy;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy) - r + half;
                    float alpha = Math.max(0, Math.min(1, 1.0f - Math.max(0f, Math.abs(dist) - plateau)));
                    int aa = Math.round(alpha * 255);
                    img.setPixel(px, py, (aa << 24) | 0x00FFFFFF);
                }
            }
            DynamicTexture dt = new DynamicTexture(() -> "smooth_border_" + key, img);
            setLinearSampler(dt);
            tex = Identifier.fromNamespaceAndPath("ravex", "smooth_border_" + key);
            MinecraftWrapper.getWrapper().getTextureManager().register(tex, dt);
            SMOOTH_BORDER_CACHE.put(key, tex);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, width, height,
            width * ss, height * ss, width * ss, height * ss, color);
    }

    private static final Map<String, Identifier> ROUND_FILL_CACHE = new HashMap<>();

    public static void drawRoundQ(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        if (width <= 1 || height <= 1 || radius <= 0) {
            drawRect(graphics, x, y, width, height, color);
            return;
        }
        int maxR = Math.min(width, height) / 2;
        if (radius > maxR) radius = maxR;
        if (((color >> 24) & 0xFF) == 0) return;
        int ss = 4;
        while (ss > 1 && (long) width * ss * height * ss > 8_000_000L) ss--;
        String key = width + "_" + height + "_" + radius;
        Identifier tex = ROUND_FILL_CACHE.get(key);
        if (tex == null) {
            int sw = width * ss;
            int sh = height * ss;
            NativeImage img = new NativeImage(sw, sh, true);
            float r = radius * ss;
            for (int py = 0; py < sh; py++) {
                float fy = py + 0.5f;
                float cy = Math.max(r, Math.min(sh - r, fy));
                float dy = fy - cy;
                for (int px = 0; px < sw; px++) {
                    float fx = px + 0.5f;
                    float cx = Math.max(r, Math.min(sw - r, fx));
                    float dx = fx - cx;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy) - r;
                    float alpha = Math.max(0f, Math.min(1f, 0.5f - dist));
                    int aa = Math.round(alpha * 255);
                    img.setPixel(px, py, (aa << 24) | 0x00FFFFFF);
                }
            }
            DynamicTexture dt = new DynamicTexture(() -> "round_fill_" + key, img);
            setLinearSampler(dt);
            tex = Identifier.fromNamespaceAndPath("ravex", "round_fill_" + key);
            MinecraftWrapper.getWrapper().getTextureManager().register(tex, dt);
            ROUND_FILL_CACHE.put(key, tex);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, width, height,
            width * ss, height * ss, width * ss, height * ss, color);
    }

    public static void drawRoundQ(GuiGraphics graphics, float x, float y, float w, float h, float r, int color) {
        drawRoundQ(graphics, (int) x, (int) y, (int) Math.ceil(w), (int) Math.ceil(h), Math.round(r), color);
    }

    public static void fillCircle(GuiGraphics graphics, int x, int y, int radius, int color) {
        if (radius <= 0) return;
        int d = radius * 2;
        Identifier tex = getSmoothCircle();
        if (tex != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x - radius, y - radius, 0f, 0f, d, d, 128, 128, color);
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

    private static final Map<String, Identifier> SMOOTH_ARC_CACHE = new HashMap<>();

    public static void drawSmoothArc(GuiGraphics graphics, float cx, float cy, float radius, float thickness, float startAngleDeg, float arcDeg, int color) {
        int a = (color >> 24) & 0xFF;
        if (a == 0 || arcDeg <= 0 || radius <= 0 || thickness <= 0) return;
        int outerR = Math.max(2, Math.round(radius));
        int thick = Math.max(1, Math.round(thickness));
        int arc = Math.min(360, Math.max(1, Math.round(arcDeg)));
        Identifier tex = getSmoothArcTexture(outerR, thick, arc);
        int texSize = outerR * 2 + 8;
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        if (Math.abs(startAngleDeg) > 0.01f) {
            pose.rotate((float) Math.toRadians(startAngleDeg));
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, tex, -texSize / 2, -texSize / 2, 0f, 0f, texSize, texSize, texSize, texSize, color);
        pose.popMatrix();
    }

    private static Identifier getSmoothArcTexture(int outerR, int thickness, int arcDeg) {
        String key = outerR + "_" + thickness + "_" + arcDeg;
        return SMOOTH_ARC_CACHE.computeIfAbsent(key, k -> {
            int padding = 4;
            int size = outerR * 2 + padding * 2;
            NativeImage img = new NativeImage(size, size, false);
            float cx = size / 2f;
            float cy = size / 2f;
            float outerRF = outerR;
            float innerR = Math.max(0, outerR - thickness);
            float endRad = (float) Math.toRadians(arcDeg);
            boolean fullCircle = arcDeg >= 360;
            int ss = 8;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int coverage = calcArcCoverageSmooth(x, y, cx, cy, outerRF, innerR, endRad, fullCircle, ss);
                    img.setPixel(x, y, (coverage << 24) | 0x00FFFFFF);
                }
            }
            DynamicTexture tex = new DynamicTexture(() -> "smooth_arc_" + key, img);
            setLinearSampler(tex);
            Identifier id = Identifier.fromNamespaceAndPath("ravex", "smooth_arc_" + key);
            MinecraftWrapper.getWrapper().getTextureManager().register(id, tex);
            return id;
        });
    }

    private static int calcArcCoverageSmooth(int px, int py, float cx, float cy, float outerR, float innerR, float endAngleRad, boolean fullCircle, int ss) {
        float total = 0;
        float outerR2 = outerR * outerR;
        float innerR2 = innerR * innerR;
        float invSs = 1.0f / ss;
        float halfThick = (outerR - innerR) / 2f;
        float edgeSoftness = Math.max(0.5f, Math.min(1.0f, halfThick * 0.3f));
        float angleSoftness = 0.04f;
        for (int sy = 0; sy < ss; sy++) {
            float fy = py + (sy + 0.5f) * invSs - cy;
            float fy2 = fy * fy;
            for (int sx = 0; sx < ss; sx++) {
                float fx = px + (sx + 0.5f) * invSs - cx;
                float dist2 = fx * fx + fy2;
                float dist = (float) Math.sqrt(dist2);

                float innerEdge = Math.max(0, Math.min(1, (dist - innerR + edgeSoftness) / edgeSoftness));
                float outerEdge = Math.max(0, Math.min(1, (outerR - dist + edgeSoftness) / edgeSoftness));
                float ringAlpha = innerEdge * outerEdge;
                if (ringAlpha <= 0) continue;

                if (fullCircle) {
                    total += ringAlpha;
                    continue;
                }

                float angle = (float) Math.atan2(fy, fx);
                if (angle < 0) angle += 2 * (float) Math.PI;

                float startDist = angle;
                float endDist = endAngleRad - angle;
                float arcAlpha;
                if (angle <= endAngleRad) {
                    arcAlpha = Math.min(1f, Math.min(startDist, endDist) / angleSoftness + 1.0f);
                    arcAlpha = Math.min(1f, arcAlpha);
                } else {
                    float pastEnd = angle - endAngleRad;
                    arcAlpha = Math.max(0, 1.0f - pastEnd / angleSoftness);
                }

                total += ringAlpha * arcAlpha;
            }
        }
        int result = Math.round(total * 255 / (ss * ss));
        return Math.min(255, Math.max(0, result));
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
            MinecraftWrapper.getWrapper().getTextureManager().register(id, tex);
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



    private static class BlurredShadow {
        final Identifier textureId;
        final int width;
        final int height;
        BlurredShadow(Identifier textureId, int width, int height) {
            this.textureId = textureId;
            this.width = width;
            this.height = height;
        }
    }

    private static final Map<Integer, BlurredShadow> GAUSSIAN_SHADOW_CACHE = new HashMap<>();

    private static int gaussianShadowKey(int width, int height, int radius) {
        return width * height + width * radius;
    }

    private static final Map<String, Identifier> GLOW_CACHE = new HashMap<>();

    private static String glowKey(int w, int h, float radius) {
        return w + "_" + h + "_" + (int) radius;
    }

    public static void drawGlow(GuiGraphics graphics, float x, float y, float width, float height, float radius, int color) {
        int a = (color >> 24) & 0xFF;
        if (a == 0 || width <= 0 || height <= 0) return;

        int pad = (int) Math.ceil(radius * 2);
        int texW = (int) Math.ceil(width) + pad * 2;
        int texH = (int) Math.ceil(height) + pad * 2;
        texW = Math.max(1, texW);
        texH = Math.max(1, texH);

        String key = glowKey(texW, texH, radius);
        Identifier texId = GLOW_CACHE.get(key);

        if (texId == null) {
            NativeImage img = new NativeImage(texW, texH, false);
            float cx = texW / 2f;
            float cy = texH / 2f;
            float hw = width / 2f;
            float hh = height / 2f;
            float sigma = radius / 2f;
            float sigma2 = 2f * sigma * sigma;

            for (int py = 0; py < texH; py++) {
                for (int px = 0; px < texW; px++) {
                    float dx = Math.abs(px - cx) - hw;
                    float dy = Math.abs(py - cy) - hh;
                    float dist = (float) Math.sqrt(Math.max(0, dx) * Math.max(0, dx) + Math.max(0, dy) * Math.max(0, dy));
                    float alpha = (float) Math.exp(-dist * dist / sigma2);
                    int aa = Math.min(255, Math.round(alpha * 255));
                    if (aa > 0) {
                        img.setPixel(px, py, (aa << 24) | 0x00FFFFFF);
                    }
                }
            }

            DynamicTexture dt = new DynamicTexture(() -> "glow_" + key, img);
            setLinearSampler(dt);
            texId = Identifier.fromNamespaceAndPath("ravex", "glow_" + key);
            MinecraftWrapper.getWrapper().getTextureManager().register(texId, dt);
            GLOW_CACHE.put(key, texId);
        }

        int drawX = (int) (x - pad);
        int drawY = (int) (y - pad);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texId, drawX, drawY, 0f, 0f, texW, texH, texW, texH, color);
    }

    private static final Map<Integer, Identifier> CIRCLE_GLOW_CACHE = new HashMap<>();

    public static void drawCircleGlow(GuiGraphics graphics, float cx, float cy, float radius, int color) {
        int a = (color >> 24) & 0xFF;
        if (a == 0 || radius <= 0) return;

        int d = (int) Math.ceil(radius * 2);
        int key = d;
        Identifier texId = CIRCLE_GLOW_CACHE.get(key);

        if (texId == null) {
            NativeImage img = new NativeImage(d, d, false);
            float mid = d / 2f;
            float invSigma2 = 8f / (radius * radius);
            for (int py = 0; py < d; py++) {
                for (int px = 0; px < d; px++) {
                    float dx = px + 0.5f - mid;
                    float dy = py + 0.5f - mid;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    float alpha = (float) Math.exp(-dist * dist * invSigma2);
                    int aa = Math.min(255, Math.round(alpha * 255));
                    if (aa > 0) {
                        img.setPixel(px, py, (aa << 24) | 0x00FFFFFF);
                    }
                }
            }

            DynamicTexture dt = new DynamicTexture(() -> "cglow_" + key, img);
            setLinearSampler(dt);
            texId = Identifier.fromNamespaceAndPath("ravex", "cglow_" + key);
            MinecraftWrapper.getWrapper().getTextureManager().register(texId, dt);
            CIRCLE_GLOW_CACHE.put(key, texId);
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, texId, (int) (cx - radius), (int) (cy - radius), 0f, 0f, d, d, d, d, color);
    }

    private static final Map<String, Identifier> PULSE_RING_CACHE = new HashMap<>();

    public static void drawPulseRing(GuiGraphics graphics, float cx, float cy, float outerRadius, float thickness, int color) {
        int a = (color >> 24) & 0xFF;
        if (a == 0 || outerRadius <= 0 || thickness <= 0) return;

        int d = (int) Math.ceil(outerRadius * 2);
        String key = d + "_" + (int) thickness;
        Identifier texId = PULSE_RING_CACHE.get(key);

        if (texId == null) {
            NativeImage img = new NativeImage(d, d, false);
            float mid = d / 2f;
            float invSigma2 = 4f / (thickness * thickness);
            float innerR = outerRadius - thickness;

            for (int py = 0; py < d; py++) {
                for (int px = 0; px < d; px++) {
                    float dx = px + 0.5f - mid;
                    float dy = py + 0.5f - mid;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    float ringDist = Math.max(0, Math.abs(dist - outerRadius + thickness / 2f) - thickness / 2f);
                    float alpha = (float) Math.exp(-ringDist * ringDist * invSigma2);
                    int aa = Math.min(255, Math.round(alpha * 255));
                    if (aa > 0) {
                        img.setPixel(px, py, (aa << 24) | 0x00FFFFFF);
                    }
                }
            }

            DynamicTexture dt = new DynamicTexture(() -> "ring_" + key, img);
            setLinearSampler(dt);
            texId = Identifier.fromNamespaceAndPath("ravex", "ring_" + key);
            MinecraftWrapper.getWrapper().getTextureManager().register(texId, dt);
            PULSE_RING_CACHE.put(key, texId);
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, texId, (int) (cx - outerRadius), (int) (cy - outerRadius), 0f, 0f, d, d, d, d, color);
    }

    public static void drawGaussianShadow(GuiGraphics graphics, float x, float y, float width, float height, int radius, int color) {
        int a = (color >> 24) & 0xFF;
        if (a == 0 || width <= 0 || height <= 0) return;

        int padding = radius * 2;
        int texW = (int) Math.ceil(width) + padding * 2;
        int texH = (int) Math.ceil(height) + padding * 2;
        texW = Math.max(1, texW);
        texH = Math.max(1, texH);

        int key = gaussianShadowKey(texW, texH, radius);
        BlurredShadow shadow = GAUSSIAN_SHADOW_CACHE.get(key);

        if (shadow == null) {
            BufferedImage img = new BufferedImage(texW, texH, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = img.createGraphics();
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(padding, padding, (int) Math.ceil(width), (int) Math.ceil(height));
            g.dispose();

            BufferedImage blurred = GaussianFilter.blur(img, radius);

            NativeImage nativeImg = new NativeImage(texW, texH, false);
            for (int py = 0; py < texH; py++) {
                for (int px = 0; px < texW; px++) {
                    int rgb = blurred.getRGB(px, py);
                    int aa = (rgb >> 24) & 0xFF;
                    nativeImg.setPixel(px, py, (aa << 24) | 0x00FFFFFF);
                }
            }

            DynamicTexture tex = new DynamicTexture(() -> "gshadow_" + key, nativeImg);
            setLinearSampler(tex);
            Identifier id = Identifier.fromNamespaceAndPath("ravex", "gshadow_" + key);
            MinecraftWrapper.getWrapper().getTextureManager().register(id, tex);
            shadow = new BlurredShadow(id, texW, texH);
            GAUSSIAN_SHADOW_CACHE.put(key, shadow);
        }

        int drawX = (int) (x - padding);
        int drawY = (int) (y - padding);
        graphics.blit(RenderPipelines.GUI_TEXTURED, shadow.textureId, drawX, drawY, 0f, 0f, shadow.width, shadow.height, shadow.width, shadow.height, color);
    }

    public static void drawGradientBlurredShadow(GuiGraphics graphics, float x, float y, float width, float height, int radius, int color1, int color2) {
        int a1 = (color1 >> 24) & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;
        if (a1 == 0 && a2 == 0) return;

        int padding = radius * 2;
        int texW = (int) Math.ceil(width) + padding * 2;
        int texH = (int) Math.ceil(height) + padding * 2;
        texW = Math.max(1, texW);
        texH = Math.max(1, texH);

        int key = gaussianShadowKey(texW, texH, radius);
        BlurredShadow shadow = GAUSSIAN_SHADOW_CACHE.get(key);

        if (shadow == null) {
            BufferedImage img = new BufferedImage(texW, texH, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = img.createGraphics();
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(padding, padding, (int) Math.ceil(width), (int) Math.ceil(height));
            g.dispose();

            BufferedImage blurred = GaussianFilter.blur(img, radius);

            NativeImage nativeImg = new NativeImage(texW, texH, false);
            for (int py = 0; py < texH; py++) {
                for (int px = 0; px < texW; px++) {
                    int rgb = blurred.getRGB(px, py);
                    int aa = (rgb >> 24) & 0xFF;
                    nativeImg.setPixel(px, py, (aa << 24) | 0x00FFFFFF);
                }
            }

            DynamicTexture tex = new DynamicTexture(() -> "gshadow_" + key, nativeImg);
            setLinearSampler(tex);
            Identifier id = Identifier.fromNamespaceAndPath("ravex", "gshadow_" + key);
            MinecraftWrapper.getWrapper().getTextureManager().register(id, tex);
            shadow = new BlurredShadow(id, texW, texH);
            GAUSSIAN_SHADOW_CACHE.put(key, shadow);
        }

        int drawX = (int) (x - padding);
        int drawY = (int) (y - padding);
        graphics.blit(RenderPipelines.GUI_TEXTURED, shadow.textureId, drawX, drawY, 0f, 0f, shadow.width, shadow.height, shadow.width, shadow.height, color1);
    }

    public static void drawRoundedRectWithBorder(GuiGraphics graphics, int x, int y, int width, int height, int radius, int fillColor, int borderColor, int borderWidth) {
        drawRound(graphics, x, y, width, height, radius, fillColor);
        drawRoundBorder(graphics, x, y, width, height, radius, borderWidth, borderColor);
    }

    private static final int RR_PAD = 2;
    private static final int RR_CACHE_MAX = 64;
    private static final long RR_RELEASE_DELAY_MS = 3000L;
    private static final Map<Identifier, Long> RR_PENDING_RELEASE = new HashMap<>();
    private static final Map<String, Identifier> PERFECT_RR_CACHE = new LinkedHashMap<String, Identifier>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Identifier> eldest) {
            boolean evict = size() > RR_CACHE_MAX;
            if (evict) deferPixelPerfectRelease(eldest.getValue());
            return evict;
        }
    };

    private static void deferPixelPerfectRelease(Identifier id) {
        if (id == null || RR_PENDING_RELEASE.containsKey(id)) return;
        RR_PENDING_RELEASE.put(id, System.currentTimeMillis());
    }

    private static void flushPixelPerfectReleases() {
        if (RR_PENDING_RELEASE.isEmpty()) return;
        long now = System.currentTimeMillis();
        var textureManager = MinecraftWrapper.getWrapper().getTextureManager();
        RR_PENDING_RELEASE.entrySet().removeIf(entry -> {
            if (now - entry.getValue() < RR_RELEASE_DELAY_MS) return false;
            if (PERFECT_RR_CACHE.containsValue(entry.getKey())) return true;
            try {
                textureManager.release(entry.getKey());
            } catch (Throwable ignored) {}
            return true;
        });
    }

    private static final class PixelPerfectSnap {
        final int devW;
        final int devH;
        final int devR;
        final int devT;
        final int texW;
        final int texH;
        final float tx;
        final float ty;
        final float sx;
        final float sy;

        PixelPerfectSnap(int devW, int devH, int devR, int devT, int texW, int texH,
                         float tx, float ty, float sx, float sy) {
            this.devW = devW;
            this.devH = devH;
            this.devR = devR;
            this.devT = devT;
            this.texW = texW;
            this.texH = texH;
            this.tx = tx;
            this.ty = ty;
            this.sx = sx;
            this.sy = sy;
        }
    }

    private static float[] guiUnitDeviceScale() {
        var window = MinecraftWrapper.getWrapper().getWindow();
        float scale = window.getGuiScale();
        if (scale <= 0f) scale = 1f;
        int width = Math.max(1, window.getWidth());
        int height = Math.max(1, window.getHeight());
        int fbWidth = 0;
        int fbHeight = 0;
        try (var stack = MemoryStack.stackPush()) {
            var wBuf = stack.mallocInt(1);
            var hBuf = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(window.handle(), wBuf, hBuf);
            fbWidth = wBuf.get(0);
            fbHeight = hBuf.get(0);
        } catch (Throwable ignored) {}
        if (fbWidth <= 0 || fbHeight <= 0) {
            fbWidth = width;
            fbHeight = height;
        }
        return new float[]{fbWidth * scale / width, fbHeight * scale / height};
    }

    private static PixelPerfectSnap snapPixelPerfect(GuiGraphics graphics, int x, int y, int width, int height,
                                                     int radius, int thickness) {
        var pose = graphics.pose();
        float a = pose.m00;
        float b = pose.m20;
        float c = pose.m11;
        float d = pose.m21;
        if (a <= 0f || c <= 0f || Math.abs(pose.m10) > 1e-3f || Math.abs(pose.m01) > 1e-3f) {
            a = 1f;
            b = 0f;
            c = 1f;
            d = 0f;
        }
        float[] base = guiUnitDeviceScale();
        float baseX = base[0];
        float baseY = base[1];
        float kx = baseX * a;
        float ky = baseY * c;
        int left = Math.round(baseX * (a * x + b));
        int top = Math.round(baseY * (c * y + d));
        int right = Math.round(baseX * (a * (x + width) + b));
        int bottom = Math.round(baseY * (c * (y + height) + d));
        int devW = Math.max(1, Math.abs(right - left));
        int devH = Math.max(1, Math.abs(bottom - top));
        int devR = Math.max(0, Math.round(radius * kx));
        int devT = Math.max(1, Math.round(thickness * Math.min(kx, ky)));
        int texW = devW + RR_PAD * 2;
        int texH = devH + RR_PAD * 2;
        float sx = texW / (width * kx);
        float sy = texH / (height * ky);
        float tx = ((left - RR_PAD) / baseX - b) / a - sx * x;
        float ty = ((top - RR_PAD) / baseY - d) / c - sy * y;
        return new PixelPerfectSnap(devW, devH, devR, devT, texW, texH, tx, ty, sx, sy);
    }

    private static int roundedRectCoverage(float fx, float fy, float width, float height, float r) {
        if (width <= 0f || height <= 0f) return 0;
        float alpha = 0.5f - roundedRectSdf(fx + 0.5f, fy + 0.5f, width, height, r);
        if (alpha <= 0f) return 0;
        if (alpha >= 1f) return 255;
        return Math.round(alpha * 255);
    }

    private static Identifier registerPixelPerfect(String key, PixelPerfectSnap snap, boolean border) {
        int texW = snap.texW;
        int texH = snap.texH;
        NativeImage img = new NativeImage(texW, texH, true);
        float r = snap.devR;
        float innerW = snap.devW - snap.devT * 2f;
        float innerH = snap.devH - snap.devT * 2f;
        float innerR = Math.max(0f, r - snap.devT);
        for (int py = 0; py < texH; py++) {
            float fy = py - RR_PAD;
            for (int px = 0; px < texW; px++) {
                float fx = px - RR_PAD;
                int aa;
                if (border) {
                    int outer = roundedRectCoverage(fx, fy, snap.devW, snap.devH, r);
                    int inner = roundedRectCoverage(fx - snap.devT, fy - snap.devT, innerW, innerH, innerR);
                    aa = Math.max(0, outer - inner);
                } else {
                    aa = roundedRectCoverage(fx, fy, snap.devW, snap.devH, r);
                }
                img.setPixel(px, py, (aa << 24) | 0x00FFFFFF);
            }
        }
        DynamicTexture dt = new DynamicTexture(() -> (border ? "perfect_rrb_" : "perfect_rr_") + key, img);
        setLinearSampler(dt);
        Identifier id = Identifier.fromNamespaceAndPath("ravex", (border ? "perfect_rrb_" : "perfect_rr_") + key);
        MinecraftWrapper.getWrapper().getTextureManager().register(id, dt);
        PERFECT_RR_CACHE.put(key, id);
        return id;
    }

    private static void blitPixelPerfect(GuiGraphics graphics, Identifier tex, PixelPerfectSnap snap,
                                         int x, int y, int width, int height, int color) {
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(snap.tx, snap.ty);
        pose.scale(snap.sx, snap.sy);
        graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, width, height, snap.texW, snap.texH, snap.texW, snap.texH, color);
        pose.popMatrix();
    }

    private static float roundedRectSdf(float fx, float fy, float width, float height, float r) {
        float b = Math.min(r, Math.min(width, height) * 0.5f);
        float qx = Math.abs(fx - width * 0.5f) - (width * 0.5f - b);
        float qy = Math.abs(fy - height * 0.5f) - (height * 0.5f - b);
        float ox = Math.max(qx, 0f);
        float oy = Math.max(qy, 0f);
        float outside = (float) Math.sqrt(ox * ox + oy * oy);
        float inside = Math.min(Math.max(qx, qy), 0f);
        return outside + inside - b;
    }

    public static void drawPixelPerfectRound(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        if (width <= 1 || height <= 1) { drawRect(graphics, x, y, width, height, color); return; }
        if (radius <= 0) { drawRect(graphics, x, y, width, height, color); return; }
        int maxR = Math.min(width, height) / 2;
        if (radius > maxR) radius = maxR;
        if (radius <= 0) { drawRect(graphics, x, y, width, height, color); return; }
        int a = (color >> 24) & 0xFF;
        if (a == 0) return;

        flushPixelPerfectReleases();
        PixelPerfectSnap snap = snapPixelPerfect(graphics, x, y, width, height, radius, 1);
        String key = "f_" + snap.devW + "_" + snap.devH + "_" + snap.devR;
        Identifier tex = PERFECT_RR_CACHE.get(key);
        if (tex == null) tex = registerPixelPerfect(key, snap, false);
        blitPixelPerfect(graphics, tex, snap, x, y, width, height, color);
    }

    public static void drawPixelPerfectRoundBorder(GuiGraphics graphics, int x, int y, int width, int height, int radius, int thickness, int color) {
        if (width <= 1 || height <= 1 || thickness <= 0) return;
        int a = (color >> 24) & 0xFF;
        if (a == 0) return;
        int maxR = Math.min(width, height) / 2;
        if (radius > maxR) radius = maxR;
        if (radius <= 0) {
            drawBorder(graphics, x, y, width, height, thickness, color);
            return;
        }
        if (thickness * 2 >= width || thickness * 2 >= height) {
            drawPixelPerfectRound(graphics, x, y, width, height, radius, color);
            return;
        }

        flushPixelPerfectReleases();
        PixelPerfectSnap snap = snapPixelPerfect(graphics, x, y, width, height, radius, thickness);
        String key = "b_" + snap.devW + "_" + snap.devH + "_" + snap.devR + "_" + snap.devT;
        Identifier tex = PERFECT_RR_CACHE.get(key);
        if (tex == null) tex = registerPixelPerfect(key, snap, true);
        blitPixelPerfect(graphics, tex, snap, x, y, width, height, color);
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

    private static Identifier sharpArrowTex = null;

    public static Identifier getSharpArrowTexture() {
        if (sharpArrowTex == null) {
            int size = 128;
            NativeImage img = new NativeImage(size, size, true);
            float cx = size / 2.0f;
            float cy = size / 2.0f;
            float h = size * 0.68f;
            float halfW = h * 0.38f;
            float tipX = cx, tipY = cy - h * 0.5f;
            float leftX = cx - halfW, leftY = cy + h * 0.5f;
            float rightX = cx + halfW, rightY = cy + h * 0.5f;
            float notchX = cx, notchY = cy + h * 0.18f;

            int ss = 6;
            float invSs = 1.0f / ss;

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int cov = 0;
                    for (int sy = 0; sy < ss; sy++) {
                        float fy = y + (sy + 0.5f) * invSs;
                        for (int sx = 0; sx < ss; sx++) {
                            float fx = x + (sx + 0.5f) * invSs;
                            if (pointInTriangle(fx, fy, tipX, tipY, leftX, leftY, notchX, notchY)
                                    || pointInTriangle(fx, fy, tipX, tipY, notchX, notchY, rightX, rightY)) {
                                cov++;
                            }
                        }
                    }
                    int alpha = (int) (Math.min(1.0f, (float) cov / (ss * ss)) * 255);
                    img.setPixel(x, y, (alpha << 24) | 0x00FFFFFF);
                }
            }

            for (int y = 0; y < size; y++) {
                img.setPixel(0, y, 0);
                img.setPixel(1, y, 0);
                img.setPixel(size - 2, y, 0);
                img.setPixel(size - 1, y, 0);
            }
            for (int x = 0; x < size; x++) {
                img.setPixel(x, 0, 0);
                img.setPixel(x, 1, 0);
                img.setPixel(x, size - 2, 0);
                img.setPixel(x, size - 1, 0);
            }

            DynamicTexture tex = new DynamicTexture(() -> "sharp_arrow_th", img);
            setLinearSampler(tex);
            sharpArrowTex = Identifier.fromNamespaceAndPath("ravex", "sharp_arrow_th");
            MinecraftWrapper.getWrapper().getTextureManager().register(sharpArrowTex, tex);
        }
        return sharpArrowTex;
    }

    public static void drawSmoothCircle(GuiGraphics graphics, float cx, float cy, float radius, int color) {
        int a = (color >> 24) & 0xFF;
        if (a == 0 || radius <= 0) return;
        int d = Math.round(radius * 2f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, getSmoothCircle(), (int) (cx - radius), (int) (cy - radius), 0f, 0f, d, d, d, d, color);
    }

    public static void drawSmoothRing(GuiGraphics graphics, float cx, float cy, float radius, float thicknessRatio, int color) {
        int a = (color >> 24) & 0xFF;
        if (a == 0 || radius <= 0) return;
        int d = Math.round(radius * 2f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, getSmoothRing(thicknessRatio), (int) (cx - radius), (int) (cy - radius), 0f, 0f, d, d, d, d, color);
    }

    private static boolean pointInTriangle(float px, float py, float ax, float ay, float bx, float by, float cx, float cy) {
        float d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by);
        float d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy);
        float d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay);
        boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);
        return !(hasNeg && hasPos);
    }

    public static void drawSharpArrow(GuiGraphics graphics, float cx, float cy, float angle, float size, int color) {
        int a = (color >> 24) & 0xFF;
        if (a == 0) return;
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        if (Math.abs(angle) > 0.001f) {
            pose.rotate(angle);
        }
        pose.translate(-size / 2f, -size / 2f);
        int isz = Math.round(size);
        graphics.blit(RenderPipelines.GUI_TEXTURED, getSharpArrowTexture(), 0, 0, 0f, 0f, isz, isz, isz, isz, color);
        pose.popMatrix();
    }
}
