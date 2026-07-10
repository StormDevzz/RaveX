package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import ravex.modules.ModuleManager;
import ravex.modules.HudModule;
import ravex.parameter.*;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;
import ravex.utility.render.BlurUtility;

import java.util.List;

public class HudEditorScreen extends Screen {
    private final Screen parentScreen;

    private HudModule draggingHud   = null;
    private int       dragOffsetX   = 0;
    private int       dragOffsetY   = 0;

    private boolean snapToGrid = true;
    private static final int GRID = 8;

    private HudModule lastHovered    = null;
    private long      hoverStartMs   = 0;
    private float     tooltipAlpha   = 0f;

    private HudModule ctxModule = null;
    private int       ctxX      = 0;
    private int       ctxY      = 0;

    private long savedFlashMs = -1;

    private HudModule settingsModule = null;

    private boolean modulePanelOpen = true;
    private int modulePanelScroll = 0;

    public HudEditorScreen(Screen parentScreen) {
        super(Component.literal("RaveX HUD Editor"));
        this.parentScreen = parentScreen;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        ravex.utility.misc.GuiOptimizer.optimizeHudAnimations(ModuleManager.INSTANCE.getHudModules());

        if (settingsModule != null) {
            renderSettingsPopup(graphics, mouseX, mouseY);
            super.render(graphics, mouseX, mouseY, partialTicks);
            return;
        }

        int accentColor = ColorUtility.getActiveColor();
        long now        = System.currentTimeMillis();

        graphics.fillGradient(0, 0, this.width, this.height, 0xE8080810, 0xE80D0D1C);

        int gridCol = 0x09FFFFFF;
        for (int x = 0; x < this.width;  x += GRID) graphics.fill(x, 0, x + 1, this.height, gridCol);
        for (int y = 0; y < this.height; y += GRID) graphics.fill(0, y, this.width, y + 1,   gridCol);

        int cx = this.width / 2, cy = this.height / 2;
        graphics.fill(cx - 10, cy, cx + 10, cy + 1, ColorUtility.withAlpha(accentColor, 30));
        graphics.fill(cx, cy - 10, cx + 1, cy + 10, ColorUtility.withAlpha(accentColor, 30));

        if (draggingHud != null) {
            int nx = mouseX - dragOffsetX;
            int ny = mouseY - dragOffsetY;
            if (snapToGrid) {
                nx = Math.round((float) nx / GRID) * GRID;
                ny = Math.round((float) ny / GRID) * GRID;
            }
            nx = Math.max(0, Math.min(this.width  - draggingHud.getWidth(),  nx));
            ny = Math.max(0, Math.min(this.height - draggingHud.getHeight(), ny));
            draggingHud.setX(nx);
            draggingHud.setY(ny);
        }

        HudModule hoveredModule = null;
        List<HudModule> hudMods = ModuleManager.INSTANCE.getHudModules();

        for (HudModule hm : hudMods) {
            if (!hm.getEnabled()) continue;

            int x1 = hm.getTargetX(), y1 = hm.getTargetY();
            int x2 = x1 + hm.getWidth(), y2 = y1 + hm.getHeight();
            boolean hov = mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
            boolean dragging = (hm == draggingHud);

            if (hov) hoveredModule = hm;

            try { hm.render(graphics, 0f); } catch (Throwable ignored) {}

            int borderAlpha = dragging ? 255 : (hov ? 200 : 70);
            int borderCol   = ColorUtility.withAlpha(dragging ? accentColor : (hov ? accentColor : 0xFFFFFFFF), borderAlpha);

            if (dragging) {
                for (int s = 1; s <= 3; s++) {
                    graphics.fill(x1 - s - 2, y1 - s - 2, x2 + s + 2, y1 - 2, ColorUtility.withAlpha(accentColor, 12 / s));
                    graphics.fill(x1 - s - 2, y2 + 2, x2 + s + 2, y2 + s + 2, ColorUtility.withAlpha(accentColor, 12 / s));
                    graphics.fill(x1 - s - 2, y1 - 2, x1 - 2, y2 + 2, ColorUtility.withAlpha(accentColor, 12 / s));
                    graphics.fill(x2 + 2, y1 - 2, x2 + s + 2, y2 + 2, ColorUtility.withAlpha(accentColor, 12 / s));
                }
            }

            int pad = 2;
            graphics.fill(x1 - pad, y1 - pad, x2 + pad, y1 - pad + 1, borderCol);
            graphics.fill(x1 - pad, y2 + pad - 1, x2 + pad, y2 + pad, borderCol);
            graphics.fill(x1 - pad, y1 - pad, x1 - pad + 1, y2 + pad, borderCol);
            graphics.fill(x2 + pad - 1, y1 - pad, x2 + pad, y2 + pad, borderCol);

            int cSize = 4;
            graphics.fill(x1 - pad, y1 - pad, x1 - pad + cSize, y1 - pad + 1, accentColor);
            graphics.fill(x1 - pad, y1 - pad, x1 - pad + 1, y1 - pad + cSize, accentColor);
            graphics.fill(x2 + pad - cSize, y2 + pad - 1, x2 + pad, y2 + pad, accentColor);
            graphics.fill(x2 + pad - 1, y2 + pad - cSize, x2 + pad, y2 + pad, accentColor);

            String tag = hm.getName();
            int tagW = FontRenderUtility.getStringWidth(tag);
            int tagX = x1, tagY = y1 - pad - 11;
            if (tagY < 0) tagY = y2 + pad + 2;
            graphics.fill(tagX - 2, tagY - 1, tagX + tagW + 4, tagY + 9, ColorUtility.withAlpha(0xFF000000, 140));
            FontRenderUtility.drawString(graphics, tag, tagX, tagY, hov ? accentColor : 0xFFAAAAAA, false);
        }

        for (HudModule hm : hudMods) {
            if (hm.getEnabled()) continue;
            int x1 = hm.getTargetX(), y1 = hm.getTargetY();
            int x2 = x1 + hm.getWidth(), y2 = y1 + hm.getHeight();
            graphics.fill(x1, y1, x2, y2, 0x18FFFFFF);
            int textW = FontRenderUtility.getStringWidth(hm.getName());
            FontRenderUtility.drawString(graphics, hm.getName(), x1 + (hm.getWidth() - textW) / 2, y1 + hm.getHeight() / 2 - 4, 0x44FFFFFF, false);
        }

        if (hoveredModule != lastHovered) {
            lastHovered  = hoveredModule;
            hoverStartMs = now;
            tooltipAlpha = 0f;
        }
        if (hoveredModule != null && now - hoverStartMs >= 1200) {
            tooltipAlpha = Math.min(1f, tooltipAlpha + 0.08f);
            if (tooltipAlpha > 0.05f) {
                String desc = hoveredModule.getDescription();
                int tw = FontRenderUtility.getStringWidth(desc) + 10;
                int tx = mouseX + 12, ty = mouseY + 12;
                if (tx + tw > this.width)  tx = mouseX - tw - 4;
                if (ty + 18 > this.height) ty = mouseY - 20;
                int bg  = ColorUtility.withAlpha(0x050508, (int)(tooltipAlpha * 220));
                int brd = ColorUtility.withAlpha(accentColor, (int)(tooltipAlpha * 180));
                graphics.fill(tx, ty, tx + tw, ty + 18, bg);
                graphics.fill(tx, ty, tx + tw, ty + 1,  brd);
                FontRenderUtility.drawString(graphics, desc, tx + 5, ty + 5,
                        ColorUtility.withAlpha(0xE0E0E0, (int)(tooltipAlpha * 255)), false);
            }
        }

        graphics.fill(0, 0, this.width, 44, 0xCC06060F);
        graphics.fill(0, 43, this.width, 44, accentColor);
        for (int s = 1; s <= 4; s++) {
            graphics.fill(0, 44 + s - 1, this.width, 44 + s, ColorUtility.withAlpha(accentColor, 24 / s));
        }

        FontRenderUtility.drawString(graphics, FontRenderUtility.FontType.VANILLA, "HUD Editor",
                18, 9, 0xFFFFFFFF, true);
        FontRenderUtility.drawString(graphics, "Drag  |  G = snap  |  RMB = menu  |  ESC = save",
                18, 22, 0xFF666688, false);

        String snapLabel = snapToGrid ? "\u00A7aSnap: ON" : "\u00A7cSnap: OFF";
        FontRenderUtility.drawString(graphics, snapLabel, this.width - FontRenderUtility.getStringWidth("Snap: OFF") - 18, 16, 0xFFFFFFFF, false);

        int tbH = 40;
        int tbY = this.height - tbH;
        graphics.fill(0, tbY, this.width, this.height, 0xCC06060F);
        graphics.fill(0, tbY, this.width, tbY + 1, accentColor);

        int btnW = 100, btnH = 22;
        int btnX = (this.width - btnW) / 2;
        int btnY = tbY + (tbH - btnH) / 2;
        boolean btnHov = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        int btnBg = btnHov ? accentColor : 0xFF141424;
        Render2DEngine.drawRound(graphics, btnX, btnY, btnW, btnH, 4, btnBg);
        if (btnHov) {
            for (int s = 1; s <= 2; s++)
                Render2DEngine.drawRound(graphics, btnX - s, btnY - s, btnW + s * 2, btnH + s * 2, 5,
                        ColorUtility.withAlpha(accentColor, 25 / s));
        }

        String btnLabel = (savedFlashMs > 0 && now - savedFlashMs < 600) ? "\u00A7aSaved!" : "Save & Exit";
        int lblW = FontRenderUtility.getStringWidth(btnLabel);
        FontRenderUtility.drawString(graphics, btnLabel, btnX + (btnW - lblW) / 2, btnY + 7, 0xFFFFFFFF, false);

        renderModulePanel(graphics, mouseX, mouseY, accentColor);

        super.render(graphics, mouseX, mouseY, partialTicks);

        if (ctxModule != null) renderContextMenu(graphics, mouseX, mouseY, accentColor);
    }

    private void renderModulePanel(GuiGraphics g, int mx, int my, int accentColor) {
        int pw = 130;
        int headerH = 18;
        List<HudModule> huds = ModuleManager.INSTANCE.getHudModules();
        int rowH = 14;
        int maxVisible = Math.min(huds.size(), 18);
        int ph = headerH + maxVisible * rowH + 4;
        int px = this.width - pw - 4;
        int py = 48;


        g.fill(px, py, px + pw, py + ph, 0xCC06060F);
        g.fill(px, py + ph - 1, px + pw, py + ph, accentColor);
        g.fill(px, py, px + 1, py + ph, ColorUtility.withAlpha(accentColor, 60));
        g.fill(px + pw - 1, py, px + pw, py + ph, ColorUtility.withAlpha(accentColor, 60));


        g.fill(px, py, px + pw, py + headerH, 0xCC0E0E22);
        g.fill(px, py + headerH - 1, px + pw, py + headerH, accentColor);

        String title = "HUD Modules (" + huds.size() + ")";
        FontRenderUtility.drawString(g, FontRenderUtility.FontType.VANILLA, title, px + 8, py + 4, 0xFFFFFFFF, true);


        int startIdx = Math.min(modulePanelScroll, Math.max(0, huds.size() - maxVisible));
        int cy = py + headerH + 2;
        for (int i = startIdx; i < huds.size() && i < startIdx + maxVisible; i++) {
            HudModule hud = huds.get(i);
            boolean enabled = hud.getEnabled();
            boolean hovered = mx >= px + 2 && mx <= px + pw - 2 && my >= cy && my <= cy + rowH;

            if (hovered)
                g.fill(px + 2, cy, px + pw - 2, cy + rowH, ColorUtility.withAlpha(accentColor, 30));

            int dotSize = 4;
            int dotX = px + 8;
            int dotY = cy + (rowH - dotSize) / 2;
            if (enabled) {
                g.fill(dotX, dotY, dotX + dotSize, dotY + dotSize, accentColor);
                g.fill(dotX, dotY, dotX + dotSize, dotY + 1, 0xFFFFFFFF);
            } else {
                g.fill(dotX, dotY, dotX + dotSize, dotY + dotSize, 0xFF404050);
            }

            String name = hud.getName();
            int textColor = enabled ? 0xFFFFFFF0 : 0xFF808090;
            FontRenderUtility.drawString(g, FontRenderUtility.FontType.VANILLA, name, px + 18, cy + 2, textColor, true);

            cy += rowH;
        }
    }

    private void renderContextMenu(GuiGraphics g, int mx, int my, int accentColor) {
        int menuW = 120, itemH = 22;
        boolean hasParams = !ctxModule.getParameters().isEmpty();
        int itemCount = hasParams ? 4 : 3;
        int menuH = itemH * itemCount + 4;
        int menuX = Math.min(ctxX, this.width  - menuW - 4);
        int menuY = Math.min(ctxY, this.height - menuH - 4);

        g.fill(menuX, menuY, menuX + menuW, menuY + menuH, 0xF00C0C1C);
        g.fill(menuX, menuY, menuX + menuW, menuY + 1,     accentColor);
        g.fill(menuX, menuY, menuX + 1,     menuY + menuH, ColorUtility.withAlpha(accentColor, 60));
        g.fill(menuX + menuW - 1, menuY, menuX + menuW, menuY + menuH, ColorUtility.withAlpha(accentColor, 60));

        String[] items;
        if (hasParams) {
            items = new String[] {
                ctxModule.getEnabled() ? "\u00A7cDisable" : "\u00A7aEnable",
                "\u00A77Settings",
                "Reset Position",
                "Reset Size"
            };
        } else {
            items = new String[] {
                ctxModule.getEnabled() ? "\u00A7cDisable" : "\u00A7aEnable",
                "Reset Position",
                "Reset Size"
            };
        }
        for (int i = 0; i < items.length; i++) {
            int iy = menuY + 2 + i * itemH;
            boolean hov = mx >= menuX && mx <= menuX + menuW && my >= iy && my <= iy + itemH;
            if (hov) g.fill(menuX + 1, iy, menuX + menuW - 1, iy + itemH, ColorUtility.withAlpha(accentColor, 30));
            FontRenderUtility.drawString(g, items[i], menuX + 10, iy + 7, 0xFFD0D0E0, false);
        }
    }

    private void renderSettingsPopup(GuiGraphics g, int mx, int my) {
        int pw = 220;
        int params = settingsModule.getParameters().size();
        int itemH = 22;
        int ph = Math.min(params * itemH + 40, this.height - 60);
        int px = (this.width - pw) / 2;
        int py = (this.height - ph) / 2;

        g.fill(0, 0, this.width, this.height, 0xAA000000);
        g.fill(px, py, px + pw, py + ph, 0xF0101028);
        g.fill(px, py, px + pw, py + 1, ColorUtility.getActiveColor());

        FontRenderUtility.drawString(g, "\u00A77" + settingsModule.getName() + " Settings", px + 10, py + 8, 0xFFFFFFFF, false);

        int yOff = py + 26;
        List<Parameter<?>> pars = settingsModule.getParameters();

        int hoveredIdx = -1;
        if (mx >= px + 5 && mx <= px + pw - 5 && my >= yOff && my <= yOff + params * itemH) {
            hoveredIdx = (my - yOff) / itemH;
        }

        for (int i = 0; i < params && i < 20; i++) {
            Parameter<?> p = pars.get(i);
            boolean hov = i == hoveredIdx;
            if (hov) g.fill(px + 3, yOff + i * itemH, px + pw - 3, yOff + (i + 1) * itemH, 0x22FFFFFF);

            String label = p.getName();
            String valueStr = getParamDisplay(p);
            int lw = FontRenderUtility.getStringWidth(label);
            FontRenderUtility.drawString(g, label, px + 10, yOff + i * itemH + 7, 0xFFB0B0C0, false);
            FontRenderUtility.drawString(g, valueStr, px + pw - 10 - FontRenderUtility.getStringWidth(valueStr),
                    yOff + i * itemH + 7, ColorUtility.getActiveColor(), false);
        }


        FontRenderUtility.drawString(g, "\u00A77Click to toggle  |  Right-click to close",
                px + 10, py + ph - 14, 0xFF606070, false);
    }

    private String getParamDisplay(Parameter<?> p) {
        if (p instanceof BooleanParameter bp) return bp.getValue() ? "\u00A7aON" : "\u00A7cOFF";
        if (p instanceof ColorParameter cp) {
            int c = cp.getValue();
            return String.format("#%06X", c & 0xFFFFFF);
        }
        if (p instanceof NumberParameter np) return String.valueOf(np.getValue());
        if (p instanceof ModeParameter mp) return mp.getValue();
        if (p instanceof StringParameter sp) return "\"" + sp.getValue() + "\"";
        return String.valueOf(p.getValue());
    }

    private void toggleParam(HudModule mod, int index) {
        List<Parameter<?>> pars = mod.getParameters();
        if (index < 0 || index >= pars.size()) return;
        Parameter<?> p = pars.get(index);
        if (p instanceof BooleanParameter bp) {
            bp.setValue(!bp.getValue());
        } else if (p instanceof ColorParameter cp) {
            int c = cp.getValue();
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            r = (r + 40) % 256;
            cp.setValue((0xFF << 24) | (r << 16) | (g << 8) | b);
        } else if (p instanceof NumberParameter np) {
            double v = np.getValue() + np.getStep();
            if (v > np.getMax()) v = np.getMin();
            np.setValue(v);
        } else if (p instanceof ModeParameter mp) {
            List<String> modes = mp.getModes();
            int idx = modes.indexOf(mp.getValue());
            idx = (idx + 1) % modes.size();
            mp.setValue(modes.get(idx));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        int mx = (int) event.x(), my = (int) event.y();

        if (settingsModule != null) {
            int pw = 220, itemH = 22;
            int ph = Math.min(settingsModule.getParameters().size() * itemH + 40, this.height - 60);
            int px = (this.width - pw) / 2;
            int py = (this.height - ph) / 2;

            if (event.button() == 1) {
                settingsModule = null;
                return true;
            }

            if (event.button() == 0) {
                if (mx >= px && mx <= px + pw && my >= py + 26 && my <= py + ph - 16) {
                    int idx = (my - py - 26) / itemH;
                    if (idx < settingsModule.getParameters().size()) {
                        toggleParam(settingsModule, idx);
                    }
                }
                return true;
            }
            return true;
        }

        int accentColor = ColorUtility.getActiveColor();

        if (ctxModule != null) {
            int menuW = 120, itemH = 22;
            boolean hasParams = !ctxModule.getParameters().isEmpty();
            int itemCount = hasParams ? 4 : 3;
            int menuH = itemH * itemCount + 4;
            int menuX = Math.min(ctxX, this.width  - menuW - 4);
            int menuY = Math.min(ctxY, this.height - menuH - 4);

            if (mx >= menuX && mx <= menuX + menuW && my >= menuY && my <= menuY + menuH) {
                int rel = (my - menuY - 2) / itemH;
                if (rel == 0) {
                    ctxModule.toggle();
                } else if (rel == 1 && hasParams) {
                    settingsModule = ctxModule;
                } else if ((rel == 1 && !hasParams) || (rel == 2 && hasParams)) {
                    ctxModule.setX(Math.max(10, this.width / 50));
                    ctxModule.setY(Math.max(10, this.height / 50));
                } else if ((rel == 2 && !hasParams) || (rel == 3 && hasParams)) {
                    ctxModule.setWidth(80);
                    ctxModule.setHeight(14);
                }
                ctxModule = null;
                return true;
            }
            ctxModule = null;
            return true;
        }


        int pw = 130, headerH = 18, rowH = 14;
        int px = this.width - pw - 4, py = 48;
        int maxVisible = Math.min(ModuleManager.INSTANCE.getHudModules().size(), 18);
        int ph = headerH + maxVisible * rowH + 4;
        if (mx >= px && mx <= px + pw && my >= py && my <= py + ph) {
            if (event.button() == 0 && my >= py + headerH + 2) {
                int idx = (my - py - headerH - 2) / rowH + modulePanelScroll;
                List<HudModule> huds = ModuleManager.INSTANCE.getHudModules();
                if (idx >= 0 && idx < huds.size()) {
                    huds.get(idx).toggle();
                }
                return true;
            }
            return true;
        }

        if (event.button() == 1) {
            for (HudModule hm : ModuleManager.INSTANCE.getHudModules()) {
                if (mx >= hm.getTargetX() && mx <= hm.getTargetX() + hm.getWidth() &&
                    my >= hm.getTargetY() && my <= hm.getTargetY() + hm.getHeight()) {
                    ctxModule = hm;
                    ctxX = mx;
                    ctxY = my;
                    return true;
                }
            }
        }

        if (event.button() == 0) {
            int tbH = 40, tbY = this.height - tbH;
            int btnW = 100, btnH = 22;
            int btnX = (this.width - btnW) / 2;
            int btnY = tbY + (tbH - btnH) / 2;

            if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                doSave();
                return true;
            }

            for (HudModule hm : ModuleManager.INSTANCE.getHudModules()) {
                if (!hm.getEnabled()) continue;
                if (mx >= hm.getTargetX() && mx <= hm.getTargetX() + hm.getWidth() &&
                    my >= hm.getTargetY() && my <= hm.getTargetY() + hm.getHeight()) {
                    draggingHud  = hm;
                    dragOffsetX  = mx - hm.getTargetX();
                    dragOffsetY  = my - hm.getTargetY();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) draggingHud = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int pw = 130, headerH = 18, rowH = 14;
        int px = this.width - pw - 4, py = 48;
        int maxVisible = Math.min(ModuleManager.INSTANCE.getHudModules().size(), 18);
        int ph = headerH + maxVisible * rowH + 4;
        if (mouseX >= px && mouseX <= px + pw && mouseY >= py && mouseY <= py + ph) {
            int total = ModuleManager.INSTANCE.getHudModules().size();
            if (verticalAmount < 0) modulePanelScroll = Math.min(total - maxVisible, modulePanelScroll + 1);
            else if (verticalAmount > 0) modulePanelScroll = Math.max(0, modulePanelScroll - 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (settingsModule != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_Q) {
                settingsModule = null;
                return true;
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) { doSave(); return true; }
        if (key == GLFW.GLFW_KEY_G)      { snapToGrid = !snapToGrid; return true; }
        return super.keyPressed(event);
    }

    private void doSave() {
        savedFlashMs = System.currentTimeMillis();
        ravex.manager.ConfigManager.INSTANCE.save("default");
        this.minecraft.setScreen(null);
    }

    @Override
    public void removed() {

        BlurUtility.disable();
        super.removed();
    }
}
