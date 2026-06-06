package ravex.utility.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class FontRenderUtility {
    public enum FontType {
        SF_MEDIUM, SF_BOLD, COMFORTAA, VANILLA
    }

    public static FontType getCurrentFontType() {
        return FontType.VANILLA;
    }

    public static void drawString(GuiGraphics graphics, String text, int x, int y, int color, boolean shadow) {
        graphics.drawString(Minecraft.getInstance().font, text, x, y, color, shadow);
    }

    public static void drawString(GuiGraphics graphics, FontType fontType, String text, int x, int y, int color, boolean shadow) {
        graphics.drawString(Minecraft.getInstance().font, text, x, y, color, shadow);
    }

    public static int getStringWidth(String text) {
        return Minecraft.getInstance().font.width(text);
    }

    public static int getStringWidth(FontType fontType, String text) {
        return Minecraft.getInstance().font.width(text);
    }

    public static int getFontHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }
}
