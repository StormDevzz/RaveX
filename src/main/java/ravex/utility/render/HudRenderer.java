package ravex.utility.render;

<<<<<<< HEAD
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
=======
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.gui.clickgui.ColorUtility;

public class HudRenderer {

<<<<<<< HEAD
    private static final int ICON_SIZE = 10;
    private static final int RADIUS = 4;
    private static final int BG_COLOR = 0x800C0C0C;

    public static void drawBackground(GuiGraphics g, int x, int y, int w, int h) {
        Render2DEngine.drawRound(g, x, y, w, h, RADIUS, BG_COLOR);
    }

    public static void drawIcon(GuiGraphics g, Identifier icon, int x, int y, int tintColor) {
        if (icon == null) return;
        g.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0f, 0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, tintColor);
    }

    public static int getIconSize() {
        return ICON_SIZE;
=======
    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h, int accentColor) {
        g.fill(x, y, x + w, y + h, 0xBB060610);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
