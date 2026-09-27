package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import ravex.manager.ModuleManager;
import ravex.modules.client.ClickGui;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;

final class SubScreenStyle {
    private SubScreenStyle() {}

    private static ClickGui cfg() {
        return ModuleManager.get(ClickGui.class);
    }

    static int panelAlpha() {
        return (int) cfg().panelOpacity;
    }

    static void background(GuiGraphics graphics, int width, int height) {
        ClickGui c = cfg();
        if (!c.drawBackground) return;
        int a = (int) c.backgroundOpacity;
        Render2DUtility.drawGradientRect(graphics, 0, 0, width, height,
            ColorUtility.withAlpha(ColorUtility.BACKGROUND_START, a),
            ColorUtility.withAlpha(ColorUtility.BACKGROUND_END, a));
    }

    static void header(GuiGraphics graphics, int width, String title, String subtitle, int activeColor) {
        int pa = panelAlpha();
        Render2DUtility.drawGradientRect(graphics, 0, 0, width, 40,
            ColorUtility.withAlpha(0x08081A, Math.min(230, pa + 60)), ColorUtility.withAlpha(0x08081A, 0));
        Render2DUtility.drawRect(graphics, 0, 39, width, 1, ColorUtility.withAlpha(activeColor, 90));
        FontRenderUtility.drawString(graphics, title, 18, 8, 0xFFFFFFFF, true);
        FontRenderUtility.drawString(graphics, subtitle, 18, 23, 0xFF7070A0, false);
    }

    static void statusPill(GuiGraphics graphics, int screenWidth, String message, int activeColor) {
        if (message == null || message.isEmpty()) return;
        int tw = FontRenderUtility.getStringWidth(message);
        int x = screenWidth - tw - 36;
        int y = 10;
        Render2DUtility.drawRound(graphics, x, y, tw + 20, 15, 7, ColorUtility.withAlpha(0x114222, 170));
        Render2DUtility.drawRoundBorder(graphics, x, y, tw + 20, 15, 7, 1, ColorUtility.withAlpha(activeColor, 70));
        FontRenderUtility.drawString(graphics, message, x + 10, y + 3, 0xFFAAFFAA, false);
    }

    static void card(GuiGraphics graphics, int x, int y, int w, int h, int activeColor) {
        ClickGui c = cfg();
        int r = Math.max(2, Math.min((int) c.cornerRadius, h / 2));
        Render2DUtility.drawGaussianShadow(graphics, x, y, w, h, Math.max(4, r), 0x66000000);
        Render2DUtility.drawRound(graphics, x, y, w, h, r, ColorUtility.withAlpha(ColorUtility.PANEL_BODY_END, (int) c.panelOpacity));
        Render2DUtility.drawRect(graphics, x + r, y, w - r * 2, 1, ColorUtility.withAlpha(activeColor, 70));
        if (c.outlines) {
            Render2DUtility.drawRoundBorder(graphics, x, y, w, h, r, 1, c.outlineColor);
        }
    }

    static void modalBackdrop(GuiGraphics graphics, int width, int height) {
        int a = Math.min(200, (int) cfg().backgroundOpacity + 80);
        Render2DUtility.drawRect(graphics, 0, 0, width, height, ColorUtility.withAlpha(0x000000, a));
    }

    static void modal(GuiGraphics graphics, int x, int y, int w, int h, int activeColor) {
        ClickGui c = cfg();
        int r = Math.max(4, Math.min((int) c.cornerRadius + 2, h / 2));
        int pa = Math.min(240, (int) c.panelOpacity + 140);
        Render2DUtility.drawGaussianShadow(graphics, x, y, w, h, r + 2, 0x99000000);
        Render2DUtility.drawRound(graphics, x, y, w, h, r, ColorUtility.withAlpha(0x0C0C1C, pa));
        Render2DUtility.drawRect(graphics, x + r, y, w - r * 2, 2, activeColor);
        if (c.outlines) {
            Render2DUtility.drawRoundBorder(graphics, x, y, w, h, r, 1, c.outlineColor);
        }
    }

    static void titleAccent(GuiGraphics graphics, int x, int y, String title, int activeColor) {
        int w = Math.max(24, (int) (FontRenderUtility.getStringWidth(title) * 0.85f));
        Render2DUtility.drawRound(graphics, x, y, w, 3, 1, ColorUtility.withAlpha(activeColor, 190));
    }

    static void button(GuiGraphics graphics, int x, int y, int w, int h, boolean hovered, boolean primary, int activeColor) {
        int ba = (int) cfg().buttonOpacity;
        int r = Math.max(3, Math.min(7, h / 2));
        int bg;
        if (primary) {
            bg = ColorUtility.withAlpha(activeColor, hovered ? 210 : Math.max(ba + 50, 110));
        } else {
            bg = ColorUtility.withAlpha(0x202030, hovered ? Math.max(ba * 2, 200) : Math.max(ba + 70, 120));
        }
        Render2DUtility.drawRound(graphics, x, y, w, h, r, bg);
    }

    static void input(GuiGraphics graphics, int x, int y, int w, int h, int activeColor) {
        int pa = panelAlpha();
        Render2DUtility.drawRound(graphics, x, y, w, h, 4, ColorUtility.withAlpha(0x141426, Math.min(255, pa + 130)));
        Render2DUtility.drawRect(graphics, x + 1, y + h - 1, w - 2, 1, ColorUtility.withAlpha(activeColor, 140));
    }

    static void row(GuiGraphics graphics, int x, int y, int w, int h, boolean selected, boolean hovered, int activeColor) {
        if (selected) {
            Render2DUtility.drawRound(graphics, x, y, w, h, 4, ColorUtility.withAlpha(activeColor, 55));
            Render2DUtility.drawRect(graphics, x, y + 3, 2, h - 6, activeColor);
        } else if (hovered) {
            Render2DUtility.drawRound(graphics, x, y, w, h, 4, ColorUtility.withAlpha(0xFFFFFF, 14));
        }
    }

    static void toolbarPill(GuiGraphics graphics, int x, int y, int w, int h) {
        Render2DUtility.drawGaussianShadow(graphics, x, y, w, h, 8, 0x55000000);
        Render2DUtility.drawRound(graphics, x, y, w, h, 10,
            ColorUtility.withAlpha(ColorUtility.PANEL_BODY_END, Math.max(panelAlpha(), 70)));
    }
}
