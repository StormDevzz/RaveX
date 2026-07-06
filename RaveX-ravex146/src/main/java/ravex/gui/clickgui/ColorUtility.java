package ravex.gui.clickgui;

import ravex.modules.render.ClickGui;
import ravex.utility.render.Render2DEngine;

import java.awt.*;

public class ColorUtility {
    public static final int HEADER_COLOR = 0xFF000000;
    public static final int HEADER_GRADIENT_END = 0xFF000000;
    public static final int PANEL_BORDER_COLOR = 0xCCFFFFFF;
    public static final int BACKGROUND_START = 0x99000000;
    public static final int BACKGROUND_END = 0xDD000000;
    public static final int PANEL_BODY_START = 0xCC000000;
    public static final int PANEL_BODY_END = 0xE6000000;
    public static final int SHADOW_COLOR = 0x60000000;

    public static int getActiveColor() {
        return getColor(0).getRGB();
    }

    public static Color getColor(int index) {
        ClickGui cfg = ClickGui.INSTANCE;
        String mode = cfg.colorMode.getValue();
        int speed = cfg.colorSpeed.getValue().intValue();
        Color c1 = new Color(cfg.color1.getValue());
        Color c2 = new Color(cfg.color2.getValue());

        return switch (mode) {
            case "Rainbow" -> Render2DEngine.rainbow(speed, index, 1f, 1f, 1f);
            case "Fade" -> Render2DEngine.fade(speed, index, c1, 1f);
            case "DoubleColor" -> Render2DEngine.twoColorEffect(c1, c2, speed, index);
            default -> c1;
        };
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
            return getColor(0).getRGB();
        }
        return hovered ? 0xFF1A1A1A : 0xFF000000;
    }

    public static int getTextColor(boolean enabled, boolean hovered) {
        if (enabled) {
            return 0xFFFFFFFF;
        }
        return hovered ? 0xFFFFFFFF : 0xFFCCCCCC;
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
