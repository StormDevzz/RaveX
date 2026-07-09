package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.parameter.Parameter;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;
import ravex.utility.render.TextureLoader;

import java.util.ArrayList;
import java.util.List;
import ravex.manager.ModuleManager;

public class SettingsPanel {
    private double x;
    private double y;
    private boolean dragging;
    private double dragOffX;
    private double dragOffY;
    private final List<ParameterElement> paramElements = new ArrayList<>();

    public SettingsPanel() {
        x = -1;
        y = -1;
        Module hud = ModuleManager.get(Hud.class);
        for (Parameter<?> p : hud.getParameters()) {
            paramElements.add(new ParameterElement(p));
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void render(GuiGraphics g, int mx, int my, int screenW, int screenH) {
        int pw = 130;
        int headerH = 18;
        int totalContent = 0;
        for (ParameterElement pe : paramElements) {
            totalContent += pe.getHeight();
        }
        int ph = headerH + 4 + totalContent;

        if (x < 0) x = (screenW - pw) / 2;
        if (y < 0) y = (screenH - ph) / 2;

        if (x + pw > screenW) x = screenW - pw - 4;
        if (x < 4) x = 4;
        if (y + ph > screenH) y = screenH - ph - 4;
        if (y < 4) y = 4;

        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        int baseAlpha = ModuleManager.get(Hud.class).editorOpacity.getValue().intValue();
        int accentColor = ColorUtility.getActiveColor();
        int radius = 6;

        Render2DEngine.drawRound(g, ix, iy, pw, ph, radius,
            ColorUtility.withAlpha(ColorUtility.PANEL_BODY_END, baseAlpha));

        if (ModuleManager.get(ravex.modules.client.ClickGui.class).outlines.getValue()) {
            int borderColor = ModuleManager.get(ravex.modules.client.ClickGui.class).outlineColor.getValue();
            Render2DEngine.drawRound(g, ix - 1, iy - 1, pw + 2, ph + 2, radius, borderColor);
        }

        boolean headerHov = mx >= ix && mx <= ix + pw && my >= iy && my <= iy + headerH;
        if (headerHov) {
            Render2DEngine.drawRound(g, ix, iy, pw, headerH, radius, ColorUtility.withAlpha(accentColor, 15));
        }
        g.fill(ix, iy + headerH - 1, ix + pw, iy + headerH, ColorUtility.withAlpha(accentColor, 40));

        Identifier palTex = TextureLoader.getPaletteTexture();
        if (palTex != null) {
            int iconSize = 14;
            g.blit(palTex, ix + 4, iy + 2, ix + 4 + iconSize, iy + 2 + iconSize,
                0.0f, 1.0f, 0.0f, 1.0f);
        }
        FontRenderUtility.drawString(g, "Hud Settings", ix + 22, iy + 3,
            ColorUtility.withAlpha(0xFFD0D0E0, 255), true);

        int cy = iy + headerH + 2;
        for (ParameterElement pe : paramElements) {
            int peH = pe.getHeight();
            if (peH <= 0) continue;
            pe.render(g, ix + 4, cy, pw - 8, peH, mx, my);
            cy += peH;
        }
    }

    public boolean mouseClicked(int mx, int my, int btn) {
        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        int pw = 130;
        int headerH = 18;

        if (btn == 0 && mx >= ix && mx <= ix + pw && my >= iy && my <= iy + headerH) {
            dragging = true;
            dragOffX = mx - x;
            dragOffY = my - y;
            return true;
        }

        if (mx < ix || mx > ix + pw || my < iy) return false;

        int cy = iy + headerH + 2;
        for (ParameterElement pe : paramElements) {
            int peH = pe.getHeight();
            if (peH <= 0) continue;
            if (my >= cy && my <= cy + peH) {
                if (pe.mouseClicked(mx, my, btn, ix + 4, cy, pw - 8, peH)) return true;
            }
            cy += peH;
        }
        return true;
    }

    public void mouseReleased() {
        dragging = false;
    }

    public boolean isDragging() { return dragging; }

    public void updateDrag(int mx, int my) {
        if (dragging) {
            x = mx - dragOffX;
            y = my - dragOffY;
        }
    }
}
