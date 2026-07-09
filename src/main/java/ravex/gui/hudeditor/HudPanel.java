package ravex.gui.hudeditor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ColorUtility;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;
import ravex.utility.render.TextureLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
public class HudPanel {
    public static final int PANEL_W = 130;
    public static final int PANEL_RADIUS = 8;
    public static final int HEADER_H = 18;
    public static final int ROW_H = 18;
    public static final int MAX_VISIBLE_PARAMS = 10;
    private int panelX;
    private int panelY;
    private boolean panelDragging;
    private int panelDragOffX;
    private int panelDragOffY;
    private int modulePanelScroll;
    private float renderX;
    private float renderY;
    private float velX;
    private float velY;
    private float rotation;
    private int lastPanelX;
    private final Set<Module> expandedModules = new HashSet<>();
    private final Map<Module, Float> expandAnimMap = new HashMap<>();
    private final List<HudModuleEntry> entries = new ArrayList<>();
    public HudPanel() {
        panelX = -1;
        panelY = 10;
        renderX = panelX;
        renderY = panelY;
        rebuildEntries();
    }
    public void rebuildEntries() {
        entries.clear();
        for (Module m : ModuleManager.INSTANCE.getHudModules()) {
            entries.add(new HudModuleEntry(m));
        }
        if (modulePanelScroll >= entries.size()) {
            modulePanelScroll = Math.max(0, entries.size() - 1);
        }
    }
    public void init(int screenWidth) {
        if (panelX < 0) {
            panelX = (screenWidth - PANEL_W) / 2;
            renderX = panelX;
        }
    }
    public int getX() { return Math.round(renderX); }
    public int getY() { return Math.round(renderY); }
    public void setPanelDragging(boolean d, int offX, int offY) {
        panelDragging = d;
        panelDragOffX = offX;
        panelDragOffY = offY;
    }
    public boolean isPanelDragging() { return panelDragging; }
    public int getDragOffX() { return panelDragOffX; }
    public int getDragOffY() { return panelDragOffY; }
    public void dragTo(int mx, int my, int screenW, int screenH) {
        panelX = mx - panelDragOffX;
        panelY = my - panelDragOffY;
    }
    public void clamp(int screenW, int screenH) {
        if (panelX + PANEL_W > screenW) panelX = screenW - PANEL_W - 4;
        if (panelX < 0) panelX = 4;
        if (panelY < 0) panelY = 4;
    }
    public void scroll(int amount, int total) {
        if (amount < 0) modulePanelScroll = Math.min(total - 1, modulePanelScroll + 1);
        else if (amount > 0) modulePanelScroll = Math.max(0, modulePanelScroll - 1);
    }
    public int getScroll() { return modulePanelScroll; }
    public int getEntryCount() { return entries.size(); }
    public boolean isExpanded(Module m) { return expandedModules.contains(m); }
    public void toggleExpanded(Module m) {
        if (expandedModules.contains(m)) expandedModules.remove(m);
        else expandedModules.add(m);
    }
    public void render(GuiGraphics g, int mx, int my, int screenW, int screenH, int alpha, int accentColor) {
        if (alpha <= 5) return;
        clamp(screenW, screenH);
        int targetX = panelX;
        int targetY = panelY;
        int pw = PANEL_W;
        int headerH = HEADER_H;
        int totalRows = getContentHeight();
        int ph = headerH + totalRows + 4;
        if (targetY + ph > screenH) targetY = screenH - ph - 4;
        if (targetY < 0) targetY = 4;
        boolean moving = panelDragging || Math.abs(velX) > 0.5f || Math.abs(velY) > 0.5f
            || Math.abs(renderX - targetX) > 0.5f || Math.abs(renderY - targetY) > 0.5f;
        if (moving) {
            float ax = (targetX - renderX) * 0.12f - velX * 0.55f;
            float ay = (targetY - renderY) * 0.12f - velY * 0.55f;
            velX += ax;
            velY += ay;
            renderX += velX;
            renderY += velY;
        } else {
            renderX = targetX;
            renderY = targetY;
            velX = 0;
            velY = 0;
        }
        int deltaX = panelX - lastPanelX;
        float rotTargetDeg;
        if (deltaX > 0) {
            rotTargetDeg = Math.min(18f, 5f + deltaX * 3.3f);
        } else if (deltaX < 0) {
            rotTargetDeg = Math.max(-18f, -5f - (-deltaX) * 3.3f);
        } else {
            rotTargetDeg = 0;
        }
        float rotSpeed = panelDragging ? 0.92f : 0.85f;
        float rotTargetRad = rotTargetDeg * 0.017453292f;
        rotation = rotation * (1f - rotSpeed) + rotTargetRad * rotSpeed;
        if (!panelDragging && Math.abs(rotation) < 0.0002f) {
            rotation = 0;
        }
        lastPanelX = panelX;
        int px = Math.round(renderX);
        int py = Math.round(renderY);
        int centerX = px + pw / 2;
        int centerY = py + ph / 2;
        float animAlpha = alpha / 255f;
        int baseAlpha = ModuleManager.get(Hud.class).editorOpacity.getValue().intValue();
        int pAlpha = (int)(baseAlpha * animAlpha);
        float stretch = Math.abs(rotation) * 0.3f;
        boolean hasTransform = Math.abs(rotation) > 0.0001f || stretch > 0.0001f;
        if (hasTransform) {
            float scaleX = 1f + stretch;
            g.pose().pushMatrix();
            g.pose().translate(centerX, centerY);
            g.pose().scale(scaleX, 1f);
            g.pose().rotate(rotation);
            g.pose().translate(-centerX, -centerY);
        }
        int panelColor = ModuleManager.get(Hud.class).panelColor.getValue();
        int pcA = (panelColor >> 24) & 0xFF;
        int bodyColor = pcA > 0
            ? ColorUtility.withAlpha(panelColor, (int)(pcA * animAlpha))
            : ColorUtility.withAlpha(ColorUtility.PANEL_BODY_END, pAlpha);
        Render2DEngine.drawRound(g, px, py, pw, ph, PANEL_RADIUS, bodyColor);
        Render2DEngine.drawRoundBorder(g, px, py, pw, ph, PANEL_RADIUS, 1,
            ColorUtility.withAlpha(accentColor, (int)(40 * animAlpha)));
        boolean headerHov = mx >= px && mx <= px + pw && my >= py && my <= py + headerH;
        if (headerHov) {
            Render2DEngine.drawRound(g, px, py, pw, headerH, PANEL_RADIUS,
                ColorUtility.withAlpha(accentColor, (int)(15 * animAlpha)));
        }
        g.fill(px, py + headerH - 1, px + pw, py + headerH,
            ColorUtility.withAlpha(accentColor, (int)(40 * animAlpha)));
        Identifier palTex = TextureLoader.getPaletteTexture();
        if (palTex != null) {
            int iconSize = 14;
            g.blit(palTex, px + 4, py + 2, px + 4 + iconSize, py + 2 + iconSize,
                0.0f, 1.0f, 0.0f, 1.0f);
        }
        FontRenderUtility.drawString(g, "HUD", px + 22, py + 3,
            ColorUtility.withAlpha(0xFFD0D0E0, alpha), true);
        List<Module> huds = ModuleManager.INSTANCE.getHudModules();
        int enabledCount = 0;
        for (Module m : huds) { if (m.getEnabled()) enabledCount++; }
        String countText = enabledCount + "/" + huds.size();
        int cw = FontRenderUtility.getStringWidth(countText);
        int badgeX = px + pw - cw - 12;
        Render2DEngine.drawRound(g, badgeX, py + 3, cw + 8, 14, 4,
            ColorUtility.withAlpha(accentColor, (int)(50 * animAlpha)));
        FontRenderUtility.drawString(g, countText, badgeX + 4, py + 5,
            enabledCount == huds.size() ? 0xFFA0E0A0 : 0xFFE0E0E0, true);
        int cy = py + headerH + 2;
        int drawn = 0;
        for (int i = modulePanelScroll; i < entries.size() && drawn < 30; i++) {
            HudModuleEntry entry = entries.get(i);
            Module hud = entry.getModule();
            boolean hovered = mx >= px + 2 && mx <= px + pw - 2 && my >= cy && my <= cy + ROW_H;
            boolean enabled = hud.getEnabled();
            boolean expanded = expandedModules.contains(hud);
            float expandAnim = expandAnimMap.getOrDefault(hud, 0f);
            float targetExpand = expanded ? 1f : 0f;
            if (expandAnim < targetExpand) {
                expandAnim = Math.min(targetExpand, expandAnim + 0.10f);
            } else if (expandAnim > targetExpand) {
                expandAnim = Math.max(targetExpand, expandAnim - 0.10f);
            }
            expandAnimMap.put(hud, expandAnim);
            entry.render(g, px + 2, cy + 1, pw - 4, hovered, enabled, alpha, accentColor, mx, my, expanded, expandAnim);
            cy += ROW_H;
            drawn++;
            if (expandAnim > 0.005f) {
                int totalParamH = 0;
                List<HudParameterEntry> params = entry.getParamEntries();
                for (HudParameterEntry pe : params) {
                    pe.update(expanded);
                    int ph2 = pe.getHeight();
                    if (ph2 <= 0) continue;
                    totalParamH += ph2;
                }
                int actualH = (int)(totalParamH * expandAnim);
                if (actualH > 0) {
                    int paramX = px + 4;
                    int paramY = cy;
                    g.enableScissor(paramX, paramY, paramX + pw - 8, paramY + actualH);
                    int pcy = cy;
                    for (HudParameterEntry pe : params) {
                        int ph2 = pe.getHeight();
                        if (ph2 <= 0) continue;
                        pe.render(g, paramX, pcy, pw - 8, alpha, accentColor, mx, my);
                        pcy += ph2;
                    }
                    g.disableScissor();
                    cy = paramY + actualH;
                }
            }
        }
        if (modulePanelScroll > 0) {
            FontRenderUtility.drawString(g, "\u2191",
                px + pw / 2 - 4, py + headerH + 2,
                ColorUtility.withAlpha(0xFF606070, alpha), false);
        }
        if (modulePanelScroll + 30 < entries.size()) {
            FontRenderUtility.drawString(g, "\u2193",
                px + pw / 2 - 4, py + ph - ROW_H + 1,
                ColorUtility.withAlpha(0xFF606070, alpha), false);
        }
        if (hasTransform) {
            g.pose().popMatrix();
        }
    }
    private int getContentHeight() {
        int h = 0;
        int drawn = 0;
        for (int i = modulePanelScroll; i < entries.size() && drawn < 30; i++) {
            h += ROW_H;
            drawn++;
            HudModuleEntry entry = entries.get(i);
            Module m = entry.getModule();
            float anim = expandAnimMap.getOrDefault(m, 0f);
            if (anim > 0.01f) {
                int totalParamH = 0;
                for (HudParameterEntry pe : entry.getParamEntries()) {
                    if (!pe.getParam().isVisible()) continue;
                    totalParamH += pe.getHeight();
                }
                h += (int)(totalParamH * anim);
            }
        }
        return h;
    }
    public boolean mouseClicked(int mx, int my, int btn, int screenW, int screenH) {
        int px = Math.round(renderX), py = Math.round(renderY);
        int pw = PANEL_W;
        clamp(screenW, screenH);
        if (mx >= px && mx <= px + pw && my >= py) {
            int headerH = HEADER_H;
            if (btn == 0 && my >= py && my <= py + headerH) {
                panelDragging = true;
                panelDragOffX = mx - px;
                panelDragOffY = my - py;
                return true;
            }
            int cy = py + headerH + 2;
            for (int i = modulePanelScroll; i < entries.size(); i++) {
                HudModuleEntry entry = entries.get(i);
                Module hud = entry.getModule();
                if (my >= cy && my <= cy + ROW_H) {
                    if (btn == 0) {
                        hud.toggle();
                        rebuildEntries();
                        return true;
                    }
                    if (btn == 1) {
                        toggleExpanded(hud);
                        return true;
                    }
                    return true;
                }
                cy += ROW_H;
                boolean expanded = expandedModules.contains(hud);
                if (expanded) {
                    for (HudParameterEntry pe : entry.getParamEntries()) {
                        if (!pe.getParam().isVisible()) continue;
                        int ph = pe.getHeight();
                        if (ph <= 0) continue;
                        if (my >= cy && my <= cy + ph) {
                            if (btn == 0) {
                                pe.toggle();
                                return true;
                            }
                            return true;
                        }
                        cy += ph;
                    }
                }
            }
            return true;
        }
        return false;
    }
    public void mouseReleased() {
        panelDragging = false;
    }
}
