package ravex.utility.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class FontRenderUtility {
    private static boolean useCustom = false;

    public enum FontType {
        SF_MEDIUM, SF_BOLD, COMFORTAA, VANILLA
    }

    public static void setCustomEnabled(boolean enabled) {
        useCustom = enabled;
    }

    public static void drawString(GuiGraphics graphics, String text, int x, int y, int color, boolean shadow) {
        drawString(graphics, FontType.SF_MEDIUM, text, x, y, color, shadow);
    }

    public static void drawString(GuiGraphics graphics, FontType fontType, String text, int x, int y, int color, boolean shadow) {
        if (useCustom) {
            RavexFontRenderer renderer = getRenderer(fontType);
            if (renderer != null) {
                try {
                    renderer.drawString(graphics, text, x, y, color);
                    if (shadow) {
                        renderer.drawString(graphics, text, x + 1, y + 1, (color & 0xFCFCFC) >> 1 | (color & 0xFF000000));
                    }
                    return;
                } catch (Throwable t) {
                    System.err.println("[RaveX] Custom font render failed for '" + text + "': " + t);
                    t.printStackTrace();
                }
            }
        }
        graphics.drawString(Minecraft.getInstance().font, text, x, y, color, shadow);
    }

    public static int getStringWidth(String text) {
        return getStringWidth(FontType.SF_MEDIUM, text);
    }

    public static int getStringWidth(FontType fontType, String text) {
        if (useCustom) {
            try {
                RavexFontRenderer renderer = getRenderer(fontType);
                if (renderer != null) return renderer.width(text);
            } catch (Throwable t) {
                System.err.println("[RaveX] Font getStringWidth failed: " + t);
            }
        }
        return Minecraft.getInstance().font.width(text);
    }

    public static int getFontHeight() {
        if (useCustom) {
            try {
                RavexFontRenderer renderer = getRenderer(FontType.SF_MEDIUM);
                if (renderer != null) return renderer.height();
            } catch (Throwable t) {
                System.err.println("[RaveX] Font getFontHeight failed: " + t);
            }
        }
        return Minecraft.getInstance().font.lineHeight;
    }

    public static boolean isCustom() {
        return useCustom;
    }

    private static RavexFontRenderer getRenderer(FontType type) {
        return switch (type) {
            case SF_MEDIUM -> RavexFontRenderer.getSfMedium();
            case SF_BOLD   -> RavexFontRenderer.getSfBold();
            case COMFORTAA -> RavexFontRenderer.getComfortaa();
            default        -> null;
        };
    }
}
