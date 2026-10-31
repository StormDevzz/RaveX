package ravex.utility.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class HudRendererUtility {

    private static final int ICON_SIZE = 10;
    private static final int RADIUS = 6;

    public static void drawBackground(GuiGraphics g, int x, int y, int w, int h) {
        drawBackground(g, x, y, w, h, 1f);
    }

    public static void drawBackground(GuiGraphics g, int x, int y, int w, int h, float alpha) {
        if (alpha <= 0.005f) return;
        Render2DUtility.drawBlurBackground(g, x, y, w, h, RADIUS, alpha);
    }

    public static void drawIcon(GuiGraphics g, Identifier icon, int x, int y, int tintColor) {
        if (icon == null) return;
        g.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0f, 0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, tintColor);
    }

    public static int getIconSize() {
        return ICON_SIZE;
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
