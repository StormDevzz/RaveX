package ravex.gui.clickgui;

import ravex.modules.client.ClickGui;
import ravex.utility.render.Render2DEngine;
import ravex.manager.ModuleManager;

public class ColorUtility {
    public static final int HEADER_COLOR = 0x330A0A12;
    public static final int HEADER_GRADIENT_END = 0x330A0A12;
    public static final int PANEL_BORDER_COLOR = 0x228A8A8A;
    public static final int BACKGROUND_START = 0x180C0C0C;
    public static final int BACKGROUND_END = 0x30101010;
    public static final int PANEL_BODY_START = 0x22101210;
    public static final int PANEL_BODY_END = 0x30101210;
    public static final int SHADOW_COLOR = 0x20000000;

    private static int cachedActiveColor = 0xFF40A9F8;
    private static long lastColorCacheTime = 0;
    private static final long COLOR_CACHE_INTERVAL = 50;

    public static int getActiveColor() {
        long now = System.currentTimeMillis();
        if (now - lastColorCacheTime < COLOR_CACHE_INTERVAL) {
            return cachedActiveColor;
        }
        lastColorCacheTime = now;
        cachedActiveColor = computeActiveColor();
        return cachedActiveColor;
    }

    private static int computeActiveColor() {
        return getColorRGB(0);
    }

    public static void invalidateColorCache() {
        lastColorCacheTime = 0;
    }

    public static int getColorRGB(int index) {
        ClickGui cfg = ModuleManager.get(ClickGui.class);
        String mode = cfg.colorMode.getValue();
        int speed = cfg.colorSpeed.getValue().intValue();
        int c1 = cfg.color1.getValue();
        int c2 = cfg.color2.getValue();

        return switch (mode) {
            case "Rainbow" -> Render2DEngine.rainbowInt(speed, index, 1f, 1f, 1f);
            case "Fade" -> Render2DEngine.fadeInt(speed, index, c1, 1f);
            case "DoubleColor" -> Render2DEngine.twoColorEffectInt(c1, c2, speed, index);
            default -> 0xFF000000 | c1;
        };
    }

    public static java.awt.Color getColor(int index) {
        int argb = getColorRGB(index);
        return new java.awt.Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
    }

    public static int getRainbowColor(int index, int speed) {
        long time = System.currentTimeMillis();
        return (0xFF << 24) | (java.awt.Color.HSBtoRGB(((time + index * 500L) % speed) / (float) speed, 0.75f, 1.0f) & 0xFFFFFF);
    }

    public static void getActiveColorRGB(float[] out) {
        int color = getActiveColor();
        out[0] = ((color >> 16) & 0xFF) / 255.0f;
        out[1] = ((color >> 8) & 0xFF) / 255.0f;
        out[2] = (color & 0xFF) / 255.0f;
    }

    public static int getModuleColor(boolean enabled, boolean hovered) {
        if (enabled) {
            return getColorRGB(0);
        }
        return hovered ? 0xFF202035 : 0xFF0D0D14;
    }

    public static int getTextColor(boolean enabled, boolean hovered) {
        if (enabled) {
            return 0xFFD0D0E0;
        }
        return hovered ? 0xFFD0D0E0 : 0xFF8F8FA0;
    }

    public static int darker(int color, float factor) {
        int a = color & 0xFF000000;
        int r = Math.max(0, (int)(((color >> 16) & 0xFF) * factor));
        int g = Math.max(0, (int)(((color >> 8) & 0xFF) * factor));
        int b = Math.max(0, (int)((color & 0xFF) * factor));
        return a | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }
}
