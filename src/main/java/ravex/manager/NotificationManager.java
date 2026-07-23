package ravex.manager;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ravex.utility.notification.Notification;
import ravex.utility.render.FontRenderUtility;
import java.lang.reflect.Field;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {
    private static final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<ToastEntry> toasts = new CopyOnWriteArrayList<>();

    private static final int PANEL_PADDING_X = 6;
    private static final int PANEL_PADDING_Y = 3;
    private static final int LINE_HEIGHT = 10;
    private static final int GAP = 4;
    private static final int ACCENT_HEIGHT = 1;

    private static final int TOAST_MARGIN = 4;
    private static final int TOAST_ICON_SIZE = 24;
    private static final int TOAST_PADDING_X = 4;
    private static final int TOAST_PADDING_Y = 3;
    private static final int TOAST_GAP = 4;
    private static final int TOAST_DURATION = 3000;
    private static final int TOAST_SLIDE_IN = 300;
    private static final int TOAST_SLIDE_OUT = 500;

    private static Identifier enableIcon;
    private static Identifier disableIcon;
    private static boolean texturesLoaded = false;

    private static void ensureTextures() {
        if (texturesLoaded) return;
        try {
            enableIcon = loadTexture("enable");
            disableIcon = loadTexture("disable");
            texturesLoaded = true;
        } catch (Exception e) {
            ravex.RaveX.LOGGER.warn("[Notifications] Failed to load toast icons: {}", e.getMessage());
            texturesLoaded = true;
        }
    }

    private static Identifier loadTexture(String name) {
        try (java.io.InputStream stream = NotificationManager.class.getResourceAsStream("/assets/ravex/textures/" + name + ".png")) {
            if (stream != null) {
                NativeImage image = NativeImage.read(stream);
                NativeImage scaled = downscaleTo(image, 128);
                if (scaled != image) image.close();
                DynamicTexture tex = new DynamicTexture(() -> "toast_" + name, scaled);
                try {
                    GpuSampler sampler = com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                    for (Field f : AbstractTexture.class.getDeclaredFields()) {
                        if (GpuSampler.class.isAssignableFrom(f.getType())) {
                            f.setAccessible(true);
                            f.set(tex, sampler);
                            break;
                        }
                    }
                } catch (Exception ignored) {}
                Identifier id = Identifier.fromNamespaceAndPath("ravex", "toast_" + name);
                Minecraft.getInstance().getTextureManager().register(id, tex);
                return id;
            }
        } catch (Exception e) {
            ravex.RaveX.LOGGER.warn("[Notifications] Failed to load texture {}: {}", name, e.getMessage());
        }
        return null;
    }

    private static NativeImage downscaleTo(NativeImage image, int maxDim) {
        int sw = image.getWidth();
        int sh = image.getHeight();
        if (sw <= maxDim && sh <= maxDim) return image;
        float scale = Math.min((float) maxDim / sw, (float) maxDim / sh);
        int tw = Math.max(1, Math.round(sw * scale));
        int th = Math.max(1, Math.round(sh * scale));
        NativeImage dst = new NativeImage(tw, th, false);

        for (int dy = 0; dy < th; dy++) {
            int sy0 = dy * sh / th;
            int sy1 = Math.min(sh, (dy + 1) * sh / th);
            for (int dx = 0; dx < tw; dx++) {
                int sx0 = dx * sw / tw;
                int sx1 = Math.min(sw, (dx + 1) * sw / tw);

                int maxA = 0, sumA = 0, opaqueCount = 0, count = 0;
                for (int sy = sy0; sy < sy1; sy++) {
                    for (int sx = sx0; sx < sx1; sx++) {
                        int p = image.getPixel(sx, sy);
                        int a = (p >> 24) & 0xFF;
                        if (a == 0) continue;
                        if (a == 255) {
                            opaqueCount++;
                            sumA += a;
                        } else {
                            sumA += a;
                        }
                        if (a > maxA) maxA = a;
                        count++;
                    }
                }

                if (count == 0) {
                    dst.setPixel(dx, dy, 0);
                } else if (opaqueCount > 0 && opaqueCount == count) {

                    int r = 0, g = 0, b = 0, n = 0;
                    for (int sy = sy0; sy < sy1; sy++) {
                        for (int sx = sx0; sx < sx1; sx++) {
                            int p = image.getPixel(sx, sy);
                            int a = (p >> 24) & 0xFF;
                            if (a == 0) continue;
                            r += (p >> 16) & 0xFF;
                            g += (p >> 8) & 0xFF;
                            b += p & 0xFF;
                            n++;
                        }
                    }
                    r = r / n; g = g / n; b = b / n;
                    dst.setPixel(dx, dy, (0xFF << 24) | (r << 16) | (g << 8) | b);
                } else {

                    int r = 0, g = 0, b = 0, totalA = 0;
                    for (int sy = sy0; sy < sy1; sy++) {
                        for (int sx = sx0; sx < sx1; sx++) {
                            int p = image.getPixel(sx, sy);
                            int a = (p >> 24) & 0xFF;
                            if (a == 0) continue;
                            r += ((p >> 16) & 0xFF) * a;
                            g += ((p >> 8) & 0xFF) * a;
                            b += (p & 0xFF) * a;
                            totalA += a;
                        }
                    }
                    if (totalA > 0) {
                        r = r / totalA; g = g / totalA; b = b / totalA;
                        int outA = Math.max(maxA, Math.min(255, sumA / Math.max(1, count)));
                        dst.setPixel(dx, dy, (outA << 24) | (r << 16) | (g << 8) | b);
                    } else {
                        dst.setPixel(dx, dy, 0);
                    }
                }
            }
        }
        return dst;
    }

    public static void add(String text) {
        add(text, 0xFFD0D0E0, 3000);
    }

    public static void add(String text, int color) {
        add(text, color, 3000);
    }

    public static void add(String text, int color, int duration) {
        notifications.add(new Notification(text, color, duration));
        if (notifications.size() > 20) {
            notifications.remove(0);
        }
    }

    public static void addToast(String text, int color, boolean enabled, double opacity, int iconSize) {
        toasts.add(new ToastEntry(text, color, enabled, opacity, iconSize));
        if (toasts.size() > 6) {
            toasts.remove(0);
        }
    }

    public static void addToast(String text, int color, boolean enabled) {
        addToast(text, color, enabled, 0.85, 14);
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return;

        renderCenterNotifications(graphics, mc);
        renderToasts(graphics);
    }

    private static void renderCenterNotifications(GuiGraphics graphics, Minecraft mc) {
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        int startY = screenH / 3;
        int currentY = startY;

        for (Notification n : notifications) {
            if (n.isExpired()) {
                notifications.remove(n);
                continue;
            }

            float alpha = n.getAlpha();
            if (alpha <= 0.01f) continue;

            int textW = mc.font.width(n.text);
            int panelW = textW + PANEL_PADDING_X * 2;
            int panelH = LINE_HEIGHT + PANEL_PADDING_Y * 2;
            int panelX = (screenW - panelW) / 2;

            int bgColor = (int)(0xBB * alpha) << 24;
            if (bgColor == 0) continue;

            int accentColor = n.color;
            int accentAlpha = (int)(0xFF * alpha) << 24;
            accentColor = (accentColor & 0x00FFFFFF) | accentAlpha;

            graphics.fill(panelX, currentY, panelX + panelW, currentY + panelH, bgColor);
            graphics.fill(panelX, currentY, panelX + panelW, currentY + ACCENT_HEIGHT, accentColor);

            int textColor = (n.color & 0x00FFFFFF) | ((int)(0xFF * alpha) << 24);
            int textX = panelX + PANEL_PADDING_X;
            int textY = currentY + PANEL_PADDING_Y;
            graphics.drawString(mc.font, n.text, textX, textY, textColor, false);

            currentY += panelH + GAP;
        }
    }

    public static void renderToasts(GuiGraphics graphics) {
        ensureTextures();
        if (enableIcon == null || disableIcon == null) return;

        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        long now = System.currentTimeMillis();

        int currentY = screenH - TOAST_MARGIN;

        for (int i = toasts.size() - 1; i >= 0; i--) {
            ToastEntry t = toasts.get(i);
            long elapsed = now - t.startTime;
            long remaining = TOAST_DURATION - elapsed;

            if (elapsed > TOAST_DURATION) {
                toasts.remove(i);
                continue;
            }

            String displayText = t.text;
            int textW = FontRenderUtility.getStringWidth(displayText);
            int iconSize = t.iconSize;
            int panelW = textW + iconSize + TOAST_PADDING_X * 3;
            int panelH = Math.max(iconSize, FontRenderUtility.getFontHeight()) + TOAST_PADDING_Y * 2;

            float slideOffset;
            float alpha;
            if (elapsed < TOAST_SLIDE_IN) {
                float progress = (float) elapsed / TOAST_SLIDE_IN;
                progress = 1f - (1f - progress) * (1f - progress);
                slideOffset = (1f - progress) * (panelW + TOAST_MARGIN);
                alpha = progress;
            } else if (remaining < TOAST_SLIDE_OUT) {
                float progress = (float) remaining / TOAST_SLIDE_OUT;
                progress = progress * progress;
                slideOffset = (1f - progress) * (panelW + TOAST_MARGIN);
                alpha = progress;
            } else {
                slideOffset = 0f;
                alpha = 1f;
            }

            if (alpha <= 0.01f) continue;

            currentY -= panelH + TOAST_GAP;

            int panelX = (int) (screenW - TOAST_MARGIN + slideOffset - panelW);
            int panelY = currentY;

            int bgAlpha = (int)(t.opacity * 255f * alpha);
            int bgColor = bgAlpha << 24;
            if (bgColor == 0) continue;

            graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, bgColor);

            int textColor = (t.color & 0x00FFFFFF) | ((int)(0xFF * alpha) << 24);

            int iconX = panelX + TOAST_PADDING_X;
            int iconY = panelY + (panelH - iconSize) / 2;

            int iconColor = 0x00FFFFFF | ((int)(0xFF * alpha) << 24);
            Identifier iconTex = t.enabled ? enableIcon : disableIcon;
            graphics.blit(RenderPipelines.GUI_TEXTURED, iconTex,
                iconX, iconY, 0f, 0f, iconSize, iconSize, iconSize, iconSize,
                iconColor);

            int textX = iconX + iconSize + TOAST_PADDING_X;
            int textY2 = panelY + (panelH - FontRenderUtility.getFontHeight()) / 2;
            FontRenderUtility.drawString(graphics, displayText, textX, textY2, textColor, true);
        }
    }

    public static void clear() {
        notifications.clear();
        toasts.clear();
    }

    private static class ToastEntry {
        final String text;
        final int color;
        final boolean enabled;
        final long startTime;
        final double opacity;
        final int iconSize;

        ToastEntry(String text, int color, boolean enabled, double opacity, int iconSize) {
            this.text = text;
            this.color = color;
            this.enabled = enabled;
            this.opacity = opacity;
            this.iconSize = iconSize;
            this.startTime = System.currentTimeMillis();
        }
    }
}
