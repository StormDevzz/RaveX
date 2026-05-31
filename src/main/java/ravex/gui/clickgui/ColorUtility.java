package ravex.gui.clickgui;

import ravex.modules.render.ClickGui;

public class ColorUtility {
    public static final int HEADER_COLOR = 0xFF12122A;
    public static final int HEADER_GRADIENT_END = 0xFF181838;
    public static final int PANEL_BORDER_COLOR = 0x33FFFFFF;
    public static final int BACKGROUND_START = 0x99070712;
    public static final int BACKGROUND_END = 0xDD0A0A1E;
    public static final int PANEL_BODY_START = 0xCC0B0B18;
    public static final int PANEL_BODY_END = 0xE00E0E22;

    public static final int SHADOW_COLOR = 0x60000000;

    public static int getActiveColor() {
        String palette = ClickGui.INSTANCE.colorPalette.getValue();
        switch (palette) {
            case "Blue":
                return 0xFF1E88E5;
            case "Green":
                return 0xFF43A047;
            case "Gold":
                return 0xFFFFB300;
            case "Purple":
                return 0xFF8E24AA;
            case "Rainbow":
                return getRainbowColor(0, 4000);
            case "Custom":
                return ClickGui.INSTANCE.accentColor.getValue();
            case "Red":
            default:
                return 0xFFE63946;
        }
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
            String palette = ClickGui.INSTANCE.colorPalette.getValue();
            switch (palette) {
                case "Blue":
                    return 0xFF0D47A1;
                case "Green":
                    return 0xFF1B5E20;
                case "Gold":
                    return 0xFFE65100;
                case "Purple":
                    return 0xFF4A148C;
                case "Rainbow":
                    return getRainbowColor(indexForButton(), 6000);
                case "Red":
                default:
                    return 0xFF8B0000;
            }
        }
        return hovered ? 0xFF202035 : 0xFF0D0D14;
    }

    public static int getTextColor(boolean enabled, boolean hovered) {
        if (enabled) {
            String palette = ClickGui.INSTANCE.colorPalette.getValue();
            switch (palette) {
                case "Blue":
                    return 0xFF90CAF9;
                case "Green":
                    return 0xFFA5D6A7;
                case "Gold":
                    return 0xFFFFE082;
                case "Purple":
                    return 0xFFE1BEE7;
                case "Rainbow":
                    return 0xFFFFFFFF;
                case "Red":
                default:
                    return 0xFFFF6B6B;
            }
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

    private static int indexForButton() {
        return 1;
    }
}
