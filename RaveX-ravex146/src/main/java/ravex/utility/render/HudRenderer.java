package ravex.utility.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ravex.gui.clickgui.ColorUtility;

public class HudRenderer {

    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h, int accentColor) {
        g.fill(x, y, x + w, y + h, 0xBB060610);
    }

    public static void drawLabel(GuiGraphics g, String text, int x, int y, int color) {
        FontRenderUtility.drawString(g, text, x + 3, y + 4, ColorUtility.withAlpha(color, 200), false);
    }

    public static void drawText(GuiGraphics g, String text, int x, int y, int color, boolean shadow) {
        FontRenderUtility.drawString(g, text, x, y + 1, color, shadow);
    }

    public static int textWidth(String text) {
        return FontRenderUtility.getStringWidth(text);
    }

    public static int fontHeight() {
        return FontRenderUtility.getFontHeight();
    }
}
