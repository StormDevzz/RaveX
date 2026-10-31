package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import ravex.manager.ModuleManager;
import ravex.modules.client.ClickGui;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.ScreenIcons;

// shared look of the sub screens, flat panels and soft shadows
public final class SubScreenStyle {
    private SubScreenStyle() {}

    private static ClickGui cfg() {
        return ModuleManager.get(ClickGui.class);
    }

    public static int panelAlpha() {
        return (int) cfg().panelOpacity;
    }

    public static void background(GuiGraphics graphics, int width, int height) {
        ClickGui c = cfg();
        if (!c.drawBackground) return;
        int a = (int) c.backgroundOpacity;
        Render2DUtility.drawGradientRect(graphics, 0, 0, width, height,
            ColorUtility.withAlpha(ColorUtility.BACKGROUND_START, a),
            ColorUtility.withAlpha(ColorUtility.BACKGROUND_END, a));
    }

    public static void header(GuiGraphics graphics, int width, String title, String subtitle, int activeColor) {
        header(graphics, width, title, subtitle, activeColor, null);
    }

    // header with an optional icon left of the title.
    public static void header(GuiGraphics graphics, int width, String title, String subtitle, int activeColor, Identifier icon) {
        int textX = 20;
        if (icon != null) {
            ScreenIcons.ensureLoaded();
            graphics.blit(RenderPipelines.GUI_TEXTURED, icon, 20, 7, 0f, 0f, 24, 24, 100, 100, 100, 100, 0xFFFFFFFF);
            textX = 52;
        }
        FontRenderUtility.drawString(graphics, title, textX, 8, 0xFFFFFFFF, true);
        FontRenderUtility.drawString(graphics, subtitle, textX, 24, 0xFF7A7A96, false);
    }

    public static void statusPill(GuiGraphics graphics, int screenWidth, String message, int activeColor) {
        if (message == null || message.isEmpty()) return;
        int tw = FontRenderUtility.getStringWidth(message);
        int x = screenWidth - tw - 36;
        int y = 10;
        Render2DUtility.drawRound(graphics, x, y, tw + 20, 15, 7, ColorUtility.withAlpha(0x114222, 170));
        Render2DUtility.drawRoundBorder(graphics, x, y, tw + 20, 15, 7, 1, ColorUtility.withAlpha(activeColor, 70));
        FontRenderUtility.drawString(graphics, message, x + 10, y + 3, 0xFFAAFFAA, false);
    }

    public static void card(GuiGraphics graphics, int x, int y, int w, int h, int activeColor) {
        ClickGui c = cfg();
        int r = Math.max(8, Math.min((int) c.cornerRadius, h / 2));
        Render2DUtility.drawGaussianShadow(graphics, x, y, w, h, Math.max(6, r), 0x38000000);
        Render2DUtility.drawRound(graphics, x, y, w, h, r, ColorUtility.withAlpha(ColorUtility.PANEL_BODY_END, (int) c.panelOpacity));
        if (c.outlines) {
            Render2DUtility.drawRoundBorder(graphics, x, y, w, h, r, 1, c.outlineColor);
        }
    }

    public static void modalBackdrop(GuiGraphics graphics, int width, int height) {
        int a = Math.min(200, (int) cfg().backgroundOpacity + 80);
        Render2DUtility.drawRect(graphics, 0, 0, width, height, ColorUtility.withAlpha(0x000000, a));
    }

    public static void modal(GuiGraphics graphics, int x, int y, int w, int h, int activeColor) {
        ClickGui c = cfg();
        int r = Math.max(8, Math.min((int) c.cornerRadius + 2, h / 2));
        int pa = Math.min(240, (int) c.panelOpacity + 140);
        Render2DUtility.drawGaussianShadow(graphics, x, y, w, h, r + 2, 0x88000000);
        Render2DUtility.drawRound(graphics, x, y, w, h, r, ColorUtility.withAlpha(0x0C0C1C, pa));
        if (c.outlines) {
            Render2DUtility.drawRoundBorder(graphics, x, y, w, h, r, 1, c.outlineColor);
        }
    }

    public static void titleAccent(GuiGraphics graphics, int x, int y, String title, int activeColor) {
        int w = Math.max(24, (int) (FontRenderUtility.getStringWidth(title) * 0.85f));
        Render2DUtility.drawRound(graphics, x, y, w, 3, 1, ColorUtility.withAlpha(activeColor, 190));
    }

    public static void button(GuiGraphics graphics, int x, int y, int w, int h, boolean hovered, boolean primary, int activeColor) {
        int ba = (int) cfg().buttonOpacity;
        int r = Math.max(6, h / 2);
        int bg;
        if (primary) {
            bg = ColorUtility.withAlpha(activeColor, hovered ? 215 : Math.max(ba + 50, 130));
        } else {
            bg = ColorUtility.withAlpha(0x1C1C2C, hovered ? Math.max(ba * 2, 205) : Math.max(ba + 70, 125));
        }
        Render2DUtility.drawRound(graphics, x, y, w, h, r, bg);
        Render2DUtility.drawRoundBorder(graphics, x, y, w, h, r, 1,
            ColorUtility.withAlpha(0xFFFFFFFF, hovered ? 28 : 15));
    }

    public static void input(GuiGraphics graphics, int x, int y, int w, int h, int activeColor) {
        int pa = panelAlpha();
        Render2DUtility.drawRound(graphics, x, y, w, h, 6, ColorUtility.withAlpha(0x141426, Math.min(255, pa + 130)));
        Render2DUtility.drawRoundBorder(graphics, x, y, w, h, 6, 1, ColorUtility.withAlpha(0xFFFFFF, 16));
    }

    public static void row(GuiGraphics graphics, int x, int y, int w, int h, boolean selected, boolean hovered, int activeColor) {
        int r = Math.min(6, h / 2);
        if (selected) {
            Render2DUtility.drawRound(graphics, x, y, w, h, r, ColorUtility.withAlpha(activeColor, 44));
            Render2DUtility.drawRound(graphics, x, y + 3, 2, h - 6, 1, activeColor);
        } else if (hovered) {
            Render2DUtility.drawRound(graphics, x, y, w, h, r, ColorUtility.withAlpha(0xFFFFFF, 10));
        }
    }

    public static void toolbarPill(GuiGraphics graphics, int x, int y, int w, int h) {
        Render2DUtility.drawGaussianShadow(graphics, x, y, w, h, 8, 0x44000000);
        Render2DUtility.drawRound(graphics, x, y, w, h, 10,
            ColorUtility.withAlpha(ColorUtility.PANEL_BODY_END, Math.max(panelAlpha(), 70)));
    }
}
