package ravex.gui.clickgui;

import ravex.modules.render.ClickGui;

public class ColorUtility {
    public static final int HEADER_COLOR = 0xFF14142B;
    public static final int PANEL_BORDER_COLOR = 0x44FFFFFF;
    public static final int BACKGROUND_START = 0x990A0A14;
    public static final int BACKGROUND_END = 0xDD0D0D1F;

    public static int getActiveColor() {
        String palette = ClickGui.INSTANCE.colorPalette.getValue();
        switch (palette) {
            case "Blue":
                return 0xFF1E88E5; // Royal Blue
            case "Green":
                return 0xFF43A047; // Emerald Green
            case "Gold":
                return 0xFFFFB300; // Gold Yellow
            case "Purple":
                return 0xFF8E24AA; // Deep Purple
            case "Rainbow":
                return getRainbowColor(0, 4000);
            case "Red":
            default:
                return 0xFFE63946; // Crimson Red
        }
    }

    public static int getRainbowColor(int index, int speed) {
        long time = System.currentTimeMillis();
        return (0xFF << 24) | (java.awt.Color.HSBtoRGB(((time + index * 500L) % speed) / (float) speed, 0.75f, 1.0f) & 0xFFFFFF);
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

    private static int indexForButton() {
        return 1;
    }
}
