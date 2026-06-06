package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import ravex.modules.ModuleManager;
import ravex.modules.HudModule;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;

import java.util.List;

/**
 * Premium HUD layout editor.
 * Left-click to drag elements · G to toggle grid snap · ESC to save & exit
 * Right-click on element → toggle on/off
 */
public class HudEditorScreen extends Screen {
    private final Screen parentScreen;

    // Dragging
    private HudModule draggingHud   = null;
    private int       dragOffsetX   = 0;
    private int       dragOffsetY   = 0;

    // Grid
    private boolean snapToGrid = true;
    private static final int GRID = 8;

    // Hover tooltip
    private HudModule lastHovered    = null;
    private long      hoverStartMs   = 0;
    private float     tooltipAlpha   = 0f;

    // Context menu (right-click)
    private HudModule ctxModule = null;
    private int       ctxX      = 0;
    private int       ctxY      = 0;

    // Save flash
    private long savedFlashMs = -1;

    public HudEditorScreen(Screen parentScreen) {
        super(Component.literal("RaveX HUD Editor"));
        this.parentScreen = parentScreen;
    }

    @Override public boolean isPauseScreen() { return false; }

    // ─── Render ──────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

        int accentColor = ColorUtility.getActiveColor();
        long now        = System.currentTimeMillis();

        // ── Background ──────────────────────────────────────────────────────────
        graphics.fillGradient(0, 0, this.width, this.height, 0xE8080810, 0xE80D0D1C);

        // Fine dot-grid
        int gridCol = 0x09FFFFFF;
        for (int x = 0; x < this.width;  x += GRID) graphics.fill(x, 0, x + 1, this.height, gridCol);
        for (int y = 0; y < this.height; y += GRID) graphics.fill(0, y, this.width, y + 1,   gridCol);

        // Brighter cross at center
        int cx = this.width / 2, cy = this.height / 2;
        graphics.fill(cx - 10, cy, cx + 10, cy + 1, ColorUtility.withAlpha(accentColor, 30));
        graphics.fill(cx, cy - 10, cx + 1, cy + 10, ColorUtility.withAlpha(accentColor, 30));

        // ── Drag logic ──────────────────────────────────────────────────────────
        if (draggingHud != null) {
            int nx = mouseX - dragOffsetX;
            int ny = mouseY - dragOffsetY;
            if (snapToGrid) {
                nx = Math.round((float) nx / GRID) * GRID;
                ny = Math.round((float) ny / GRID) * GRID;
            }
            // Clamp to screen
            nx = Math.max(0, Math.min(this.width  - draggingHud.getWidth(),  nx));
            ny = Math.max(0, Math.min(this.height - draggingHud.getHeight(), ny));
            draggingHud.setX(nx);
            draggingHud.setY(ny);
        }

        // ── HUD modules ─────────────────────────────────────────────────────────
        HudModule hoveredModule = null;
        List<HudModule> hudMods = ModuleManager.INSTANCE.getHudModules();

        for (HudModule hm : hudMods) {
            if (!hm.getEnabled()) continue;

            int x1 = hm.getX(), y1 = hm.getY();
            int x2 = x1 + hm.getWidth(), y2 = y1 + hm.getHeight();
            boolean hov = mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
            boolean dragging = (hm == draggingHud);

            if (hov) hoveredModule = hm;

            // Render the element itself (so user sees it while dragging)
            try { hm.render(graphics, 0f); } catch (Throwable ignored) {}

            // Overlay bounding box
            int borderAlpha = dragging ? 255 : (hov ? 200 : 70);
            int borderCol   = ColorUtility.withAlpha(dragging ? accentColor : (hov ? accentColor : 0xFFFFFFFF), borderAlpha);

            // Outer glow when dragging
            if (dragging) {
                for (int s = 1; s <= 3; s++) {
                    graphics.fill(x1 - s - 2, y1 - s - 2, x2 + s + 2, y1 - 2,     ColorUtility.withAlpha(accentColor, 12 / s));
                    graphics.fill(x1 - s - 2, y2 + 2,     x2 + s + 2, y2 + s + 2, ColorUtility.withAlpha(accentColor, 12 / s));
                    graphics.fill(x1 - s - 2, y1 - 2,     x1 - 2,     y2 + 2,     ColorUtility.withAlpha(accentColor, 12 / s));
                    graphics.fill(x2 + 2,     y1 - 2,     x2 + s + 2, y2 + 2,     ColorUtility.withAlpha(accentColor, 12 / s));
                }
            }

            // Border frame (2px gap around element)
            int pad = 2;
            graphics.fill(x1 - pad, y1 - pad, x2 + pad, y1 - pad + 1, borderCol); // top
            graphics.fill(x1 - pad, y2 + pad - 1, x2 + pad, y2 + pad, borderCol); // bottom
            graphics.fill(x1 - pad, y1 - pad, x1 - pad + 1, y2 + pad, borderCol); // left
            graphics.fill(x2 + pad - 1, y1 - pad, x2 + pad, y2 + pad, borderCol); // right

            // Corner accents
            int cSize = 4;
            graphics.fill(x1 - pad, y1 - pad, x1 - pad + cSize, y1 - pad + 1, accentColor);
            graphics.fill(x1 - pad, y1 - pad, x1 - pad + 1, y1 - pad + cSize, accentColor);
            graphics.fill(x2 + pad - cSize, y2 + pad - 1, x2 + pad, y2 + pad, accentColor);
            graphics.fill(x2 + pad - 1, y2 + pad - cSize, x2 + pad, y2 + pad, accentColor);

            // Name tag
            String tag = hm.getName();
            int tagW = FontRenderUtility.getStringWidth(tag);
            int tagX = x1, tagY = y1 - pad - 11;
            if (tagY < 0) tagY = y2 + pad + 2;
            graphics.fill(tagX - 2, tagY - 1, tagX + tagW + 4, tagY + 9, ColorUtility.withAlpha(0xFF000000, 140));
            FontRenderUtility.drawString(graphics, tag, tagX, tagY, hov ? accentColor : 0xFFAAAAAA, false);
        }

        // Disabled modules shown as ghost
        for (HudModule hm : hudMods) {
            if (hm.getEnabled()) continue;
            int x1 = hm.getX(), y1 = hm.getY();
            int x2 = x1 + hm.getWidth(), y2 = y1 + hm.getHeight();
            graphics.fill(x1, y1, x2, y2, 0x18FFFFFF);
            int textW = FontRenderUtility.getStringWidth(hm.getName());
            FontRenderUtility.drawString(graphics, hm.getName(), x1 + (hm.getWidth() - textW) / 2, y1 + hm.getHeight() / 2 - 4, 0x44FFFFFF, false);
        }

        // ── Hover tooltip (1.5 s delay) ─────────────────────────────────────────
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
                graphics.fill(tx, ty, tx + tw, ty + 16, bg);
                graphics.fill(tx, ty, tx + tw, ty + 1,  brd);
                FontRenderUtility.drawString(graphics, desc, tx + 5, ty + 4,
                        ColorUtility.withAlpha(0xE0E0E0, (int)(tooltipAlpha * 255)), false);
            }
        }

        // ── Header bar ──────────────────────────────────────────────────────────
        graphics.fill(0, 0, this.width, 44, 0xCC06060F);
        graphics.fill(0, 43, this.width, 44, accentColor);
        // Glow below header
        for (int s = 1; s <= 4; s++) {
            graphics.fill(0, 44 + s - 1, this.width, 44 + s, ColorUtility.withAlpha(accentColor, 24 / s));
        }

        FontRenderUtility.drawString(graphics, FontRenderUtility.FontType.VANILLA, "HUD Editor",
                18, 9, 0xFFFFFFFF, true);
        FontRenderUtility.drawString(graphics, "Drag elements  ·  G = grid snap  ·  RMB = toggle  ·  ESC = save & exit",
                18, 22, 0xFF666688, false);

        // Snap indicator
        String snapLabel = snapToGrid ? "§aGrid Snap: ON" : "§cGrid Snap: OFF";
        FontRenderUtility.drawString(graphics, snapLabel, this.width - FontRenderUtility.getStringWidth("Grid Snap: OFF") - 18, 16, 0xFFFFFFFF, false);

        // ── Bottom toolbar ───────────────────────────────────────────────────────
        int tbH = 40;
        int tbY = this.height - tbH;
        graphics.fill(0, tbY, this.width, this.height, 0xCC06060F);
        graphics.fill(0, tbY, this.width, tbY + 1, accentColor);

        // Save button
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

        // Flash on save
        String btnLabel = (savedFlashMs > 0 && now - savedFlashMs < 600) ? "§aSaved!" : "Save & Exit";
        int lblW = FontRenderUtility.getStringWidth(btnLabel);
        FontRenderUtility.drawString(graphics, btnLabel, btnX + (btnW - lblW) / 2, btnY + 7, 0xFFFFFFFF, false);

        super.render(graphics, mouseX, mouseY, partialTicks);

        // ── Right-click context menu ─────────────────────────────────────────────
        if (ctxModule != null) renderContextMenu(graphics, mouseX, mouseY, accentColor);
    }

    private void renderContextMenu(GuiGraphics g, int mx, int my, int accentColor) {
        int menuW = 120, itemH = 22;
        int menuH = itemH * 2 + 4;
        int menuX = Math.min(ctxX, this.width  - menuW - 4);
        int menuY = Math.min(ctxY, this.height - menuH - 4);

        g.fill(menuX, menuY, menuX + menuW, menuY + menuH, 0xF00C0C1C);
        g.fill(menuX, menuY, menuX + menuW, menuY + 1,     accentColor);
        g.fill(menuX, menuY, menuX + 1,     menuY + menuH, ColorUtility.withAlpha(accentColor, 60));
        g.fill(menuX + menuW - 1, menuY, menuX + menuW, menuY + menuH, ColorUtility.withAlpha(accentColor, 60));

        String[] items = {
            ctxModule.getEnabled() ? "§cDisable" : "§aEnable",
            "Reset Position"
        };
        for (int i = 0; i < items.length; i++) {
            int iy = menuY + 2 + i * itemH;
            boolean hov = mx >= menuX && mx <= menuX + menuW && my >= iy && my <= iy + itemH;
            if (hov) g.fill(menuX + 1, iy, menuX + menuW - 1, iy + itemH, ColorUtility.withAlpha(accentColor, 30));
            FontRenderUtility.drawString(g, items[i], menuX + 10, iy + 7, 0xFFD0D0E0, false);
        }
    }

    // ─── Mouse ───────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        int mx = (int) event.x(), my = (int) event.y();
        int accentColor = ColorUtility.getActiveColor();

        // Close context menu on any click outside
        if (ctxModule != null) {
            int menuW = 120, itemH = 22, menuH = itemH * 2 + 4;
            int menuX = Math.min(ctxX, this.width  - menuW - 4);
            int menuY = Math.min(ctxY, this.height - menuH - 4);

            if (mx >= menuX && mx <= menuX + menuW && my >= menuY && my <= menuY + menuH) {
                // Which item?
                int rel = (my - menuY - 2) / itemH;
                if (rel == 0) {
                    ctxModule.toggle();
                } else if (rel == 1) {
                    // Reset position to a safe default
                    ctxModule.setX(10);
                    ctxModule.setY(10);
                }
                ctxModule = null;
                return true;
            }
            ctxModule = null;
            return true;
        }

        // Right-click → context menu
        if (event.button() == 1) {
            for (HudModule hm : ModuleManager.INSTANCE.getHudModules()) {
                if (mx >= hm.getX() && mx <= hm.getX() + hm.getWidth() &&
                    my >= hm.getY() && my <= hm.getY() + hm.getHeight()) {
                    ctxModule = hm;
                    ctxX = mx;
                    ctxY = my;
                    return true;
                }
            }
        }

        // Left-click: check toolbar button
        if (event.button() == 0) {
            int tbH = 40, tbY = this.height - tbH;
            int btnW = 100, btnH = 22;
            int btnX = (this.width - btnW) / 2;
            int btnY = tbY + (tbH - btnH) / 2;

            if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                doSave();
                return true;
            }

            // Start drag
            for (HudModule hm : ModuleManager.INSTANCE.getHudModules()) {
                if (!hm.getEnabled()) continue;
                if (mx >= hm.getX() && mx <= hm.getX() + hm.getWidth() &&
                    my >= hm.getY() && my <= hm.getY() + hm.getHeight()) {
                    draggingHud  = hm;
                    dragOffsetX  = mx - hm.getX();
                    dragOffsetY  = my - hm.getY();
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

    // ─── Keyboard ────────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) { doSave(); return true; }
        if (key == GLFW.GLFW_KEY_G)      { snapToGrid = !snapToGrid; return true; }
        return super.keyPressed(event);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private void doSave() {
        savedFlashMs = System.currentTimeMillis();
        ravex.manager.ConfigManager.INSTANCE.save("default");
        this.minecraft.setScreen(null);
    }
}
