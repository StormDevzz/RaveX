package ravex.gui.hudeditor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.utility.render.ColorUtility;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.modules.client.ClickGui;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.TextureLoaderUtility;
import ravex.utility.render.animate.AnimationUtility;
import ravex.event.EventBusHolder;
import ravex.event.client.SoundEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HudPanel {
    public static final int PANEL_W = 130;
    public static final int HEADER_H = 18;
    private int panelX;
    private int panelY;
    private boolean panelDragging;
    private int panelDragOffX;
    private int panelDragOffY;
    private int modulePanelScroll;
    private float renderX;
    private float renderY;
    private float rotation;
    private float lastRenderX;
    private long lastGearTick = System.currentTimeMillis();
    private long lastExpandNanos = System.nanoTime();
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
        int prev = modulePanelScroll;
        if (amount < 0) modulePanelScroll = Math.min(total - 1, modulePanelScroll + 1);
        else if (amount > 0) modulePanelScroll = Math.max(0, modulePanelScroll - 1);
        if (modulePanelScroll != prev) {
            EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.SCROLL, 0.4f));
        }
    }

    public int getScroll() { return modulePanelScroll; }
    public int getEntryCount() { return entries.size(); }
    public boolean isExpanded(Module m) { return expandedModules.contains(m); }

    public void toggleExpanded(Module m) {
        if (expandedModules.contains(m)) {
            expandedModules.remove(m);
            EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.SETTINGS_CLOSE, 0.6f));
        } else {
            expandedModules.add(m);
            EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.SETTINGS_OPEN, 0.6f));
        }
    }

    private int getRowH() {
        return (int) ModuleManager.get(ClickGui.class).buttonHeight + 2;
    }

    public void render(GuiGraphics g, int mx, int my, int screenW, int screenH, int alpha, int accentColor) {
        if (alpha <= 5) return;
        clamp(screenW, screenH);
        int targetX = panelX;
        int targetY = panelY;
        int pw = PANEL_W;
        int headerH = HEADER_H;
        int rowH = getRowH();
        int totalRows = getContentHeight();
        int ph = headerH + totalRows + 4;
        if (targetY + ph > screenH) targetY = screenH - ph - 4;
        if (targetY < 0) targetY = 4;

        double dragLerp = 0.12;
        float dragFactor = (float) Math.min(1.0, dragLerp * 0.85);

        if (Math.abs(targetX - renderX) > 0.05f) {
            renderX += (targetX - renderX) * dragFactor;
        } else {
            renderX = targetX;
        }
        if (Math.abs(targetY - renderY) > 0.05f) {
            renderY += (targetY - renderY) * dragFactor;
        } else {
            renderY = targetY;
        }

        float deltaX = renderX - lastRenderX;
        float rotTargetDeg;
        if (deltaX > 0) {
            rotTargetDeg = Math.min(18f, 5f + deltaX * 6.0f);
        } else if (deltaX < 0) {
            rotTargetDeg = Math.max(-18f, -5f - (-deltaX) * 6.0f);
        } else {
            rotTargetDeg = 0;
        }
        float rotSpeed = panelDragging ? 0.92f : 0.85f;
        float rotTargetRad = rotTargetDeg * 0.017453292f;
        rotation = rotation * (1f - rotSpeed) + rotTargetRad * rotSpeed;
        if (!panelDragging && Math.abs(rotation) < 0.0002f) {
            rotation = 0;
        }
        lastRenderX = renderX;
        int px = Math.round(renderX);
        int py = Math.round(renderY);
        int centerX = px + pw / 2;
        int centerY = py + ph / 2;
        float animAlpha = alpha / 255f;

        long now = System.currentTimeMillis();
        float dt = Math.min(100f, now - lastGearTick) / 1000f;
        lastGearTick = now;
        float speed = (float) ModuleManager.get(ClickGui.class).gearRotationSpeed;
        if (speed > 0) {
            for (Module m : expandedModules) {
                float cur = m.getGearAngle();
                m.setGearAngle(cur + speed * dt, now);
            }
        }
        float decay = (float) Math.exp(-dt * 12f);
        for (Module m : ModuleManager.INSTANCE.getHudModules()) {
            if (expandedModules.contains(m)) continue;
            float cur = m.getGearAngle();
            if (Math.abs(cur) > 0.01f) {
                float newAngle = cur * decay;
                if (Math.abs(newAngle) < 0.01f) newAngle = 0f;
                m.setGearAngle(newAngle, now);
            }
        }

        int pAlpha = (int) ((int) ModuleManager.get(ClickGui.class).panelOpacity * animAlpha);
        int cornerRadius = Math.min((int) ModuleManager.get(ClickGui.class).cornerRadius, ph / 2);

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

        int bodyColor = ColorUtility.withAlpha(ColorUtility.PANEL_BODY_END, pAlpha);
        Render2DUtility.drawRound(g, px, py, pw, ph, cornerRadius, bodyColor);

        if (ModuleManager.get(ClickGui.class).outlines) {
            int borderColor = ColorUtility.withAlpha(ModuleManager.get(ClickGui.class).outlineColor, (int) (255 * animAlpha));
            Render2DUtility.drawRoundBorder(g, px, py, pw, ph, cornerRadius, 1, borderColor);
        }

        boolean headerHov = mx >= px && mx <= px + pw && my >= py && my <= py + headerH;
        if (headerHov) {
            Render2DUtility.drawRound(g, px, py, pw, headerH, cornerRadius,
                ColorUtility.withAlpha(accentColor, (int) (15 * animAlpha)));
        }
        g.fill(px, py + headerH - 1, px + pw, py + headerH,
            ColorUtility.withAlpha(accentColor, (int) (40 * animAlpha)));

        Identifier palTex = TextureLoaderUtility.getPaletteTexture();
        if (palTex != null) {
            int iconSize = 14;
            g.blit(palTex, px + 4, py + 2, px + 4 + iconSize, py + 2 + iconSize,
                0.0f, 1.0f, 0.0f, 1.0f);
        }
        FontRenderUtility.drawString(g, "HUD", px + 22, py + 3,
            ColorUtility.withAlpha(0xFFD0D0E0, alpha), true);

        if (ModuleManager.get(Hud.class).showCounter) {
            List<Module> huds = ModuleManager.INSTANCE.getHudModules();
            int enabledCount = 0;
            for (Module m : huds) { if (m.getEnabled()) enabledCount++; }
            String countText = enabledCount + "/" + huds.size();
            int cw = FontRenderUtility.getStringWidth(countText);
            int badgeX = px + pw - cw - 12;
            Render2DUtility.drawRound(g, badgeX, py + 3, cw + 8, 14, 4,
                ColorUtility.withAlpha(accentColor, (int) (50 * animAlpha)));
            FontRenderUtility.drawString(g, countText, badgeX + 4, py + 5,
                enabledCount == huds.size() ? 0xFFA0E0A0 : 0xFFE0E0E0, true);
        }

        int cy = py + headerH + 2;
        int drawn = 0;
        int btnH = (int) ModuleManager.get(ClickGui.class).buttonHeight;
        long nowNanos = System.nanoTime();
        float expandDt = Math.min(0.05f, (nowNanos - lastExpandNanos) / 1_000_000_000f);
        lastExpandNanos = nowNanos;
        float expandStep = 7f * expandDt;
        for (int i = modulePanelScroll; i < entries.size() && drawn < 30; i++) {
            HudModuleEntry entry = entries.get(i);
            Module hud = entry.getModule();
            boolean hovered = mx >= px + 2 && mx <= px + pw - 2 && my >= cy && my <= cy + btnH;
            boolean enabled = hud.getEnabled();
            boolean expanded = expandedModules.contains(hud);
            float expandAnim = expandAnimMap.getOrDefault(hud, 0f);
            float targetExpand = expanded ? 1f : 0f;
            if (expandAnim < targetExpand) {
                expandAnim = Math.min(targetExpand, expandAnim + expandStep);
            } else if (expandAnim > targetExpand) {
                expandAnim = Math.max(targetExpand, expandAnim - expandStep);
            }
            if (Math.abs(targetExpand - expandAnim) < 0.004f) expandAnim = targetExpand;
            expandAnimMap.put(hud, expandAnim);
            entry.render(g, px + 2, cy, pw - 4, hovered, enabled, alpha, accentColor, mx, my, expanded, expandAnim);
            cy += rowH;
            drawn++;
            if (expandAnim > 0.005f) {
                int totalParamH = 0;
                int rowCount = 0;
                List<HudParameterEntry> params = entry.getParamEntries();
                for (HudParameterEntry pe : params) {
                    pe.update(expanded);
                    if (!pe.getParam().isVisible()) continue;
                    int ph2 = pe.getHeight();
                    if (ph2 <= 0) continue;
                    totalParamH += ph2;
                    rowCount++;
                }
                float easedExpand = AnimationUtility.Easing.QUINT_OUT.apply(expandAnim);
                int actualH = (int) (totalParamH * easedExpand);
                if (actualH > 0) {
                    int paramX = px + 4;
                    int paramY = cy;
                    int panelW2 = pw - 8;
                    int bgAlpha = (int) (Math.max(Math.min(pAlpha, 255) / 2, 40) * easedExpand * animAlpha);
                    if (bgAlpha > 0) {
                        Render2DUtility.drawRound(g, paramX, paramY, panelW2, actualH, 6,
                            ColorUtility.withAlpha(0x0A0A14, bgAlpha));
                    }
                    int wipeW = (int) (panelW2 * easedExpand);
                    if (wipeW > 2) {
                        int lineAlpha = (int) (220f * easedExpand * animAlpha);
                        Render2DUtility.drawRound(g, paramX, paramY, wipeW, 2, 1,
                            ColorUtility.withAlpha(accentColor, lineAlpha));
                    }
                    float lastDelay = rowCount > 1 ? (rowCount - 1) * 0.06f : 0f;
                    float delayScale = lastDelay > 0.9f ? 0.9f / lastDelay : 1f;
                    float rowSpan = Math.max(0.1f, 1f - lastDelay * delayScale);
                    Render2DUtility.pushScissor(g, paramX, paramY, panelW2, actualH);
                    int pcy = paramY;
                    int rowIdx = 0;
                    for (HudParameterEntry pe : params) {
                        if (!pe.getParam().isVisible()) continue;
                        int ph2 = pe.getHeight();
                        if (ph2 <= 0) continue;
                        float delay = rowIdx * 0.06f * delayScale;
                        float rowT = AnimationUtility.clamp((expandAnim - delay) / rowSpan, 0f, 1f);
                        float rowEased = AnimationUtility.Easing.QUINT_OUT.apply(rowT);
                        int rowAlpha = (int) (alpha * rowEased);
                        if (rowAlpha > 0) {
                            int slide = (int) ((1f - rowEased) * 6f);
                            pe.render(g, paramX, pcy + slide, panelW2, rowAlpha, accentColor, mx, my);
                        }
                        pcy += ph2;
                        rowIdx++;
                    }
                    Render2DUtility.popScissor(g);
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
                px + pw / 2 - 4, py + ph - rowH + 1,
                ColorUtility.withAlpha(0xFF606070, alpha), false);
        }
        if (hasTransform) {
            g.pose().popMatrix();
        }
    }

    private int getContentHeight() {
        int h = 0;
        int drawn = 0;
        int rowH = getRowH();
        for (int i = modulePanelScroll; i < entries.size() && drawn < 30; i++) {
            h += rowH;
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
                h += (int) (totalParamH * AnimationUtility.Easing.QUINT_OUT.apply(anim));
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
            int rowH = getRowH();
            int btnH = (int) ModuleManager.get(ClickGui.class).buttonHeight;
            if (btn == 0 && my >= py && my <= py + headerH) {
                panelDragging = true;
                panelDragOffX = mx - px;
                panelDragOffY = my - py;
                ravex.utility.misc.CursorUtility.setHand();
                return true;
            }
            int cy = py + headerH + 2;
            for (int i = modulePanelScroll; i < entries.size(); i++) {
                HudModuleEntry entry = entries.get(i);
                Module hud = entry.getModule();
                if (my >= cy && my <= cy + btnH) {
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
                cy += rowH;
                boolean expanded = expandedModules.contains(hud);
                if (expanded) {
                    for (HudParameterEntry pe : entry.getParamEntries()) {
                        if (!pe.getParam().isVisible()) continue;
                        int ph = pe.getHeight();
                        if (ph <= 0) continue;
                        if (my >= cy && my <= cy + ph) {
                            if (pe.mouseClicked(mx, my, px + 4, cy, pw - 8, btn)) {
                                return true;
                            }
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
