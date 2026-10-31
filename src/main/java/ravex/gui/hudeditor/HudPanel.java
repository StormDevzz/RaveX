package ravex.gui.hudeditor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.utility.render.ColorUtility;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.modules.client.ClickGui;
import ravex.gui.clickgui.ParameterElement;
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
    private final Map<Module, Long> shakeStartNs = new HashMap<>();
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
    // drag follow and tilt run on springs
    private final AnimationUtility.SpringAnimation posSpringX = new AnimationUtility.SpringAnimation(0f);
    private final AnimationUtility.SpringAnimation posSpringY = new AnimationUtility.SpringAnimation(0f);
    private final AnimationUtility.SpringAnimation rotSpring = new AnimationUtility.SpringAnimation(0f);
    private long lastAnimNanos = System.nanoTime();
    private long lastGearTick = System.currentTimeMillis();
    private long lastExpandNanos = System.nanoTime();
    private final Set<Module> expandedModules = new HashSet<>();
    private final Map<Module, Float> expandAnimMap = new HashMap<>();
    private final List<HudModuleEntry> entries = new ArrayList<>();
    // the entry under the cursor, the description panel follows it
    private Module hoveredModule = null;
    private int lastPanelHeight = 0;

    public HudPanel() {
        panelX = -1;
        panelY = 10;
        renderX = panelX;
        renderY = panelY;
        posSpringX.setValue(renderX);
        posSpringY.setValue(renderY);
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
            posSpringX.setValue(renderX);
            posSpringY.setValue(renderY);
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
    public Module getHoveredModule() { return hoveredModule; }
    public int getPanelHeight() { return lastPanelHeight; }    public int getDragOffX() { return panelDragOffX; }
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

        long animNanos = System.nanoTime();
        float animDt = Math.min(50f, (animNanos - lastAnimNanos) / 1_000_000f);
        lastAnimNanos = animNanos;

        // critically damped follow, the panel settles without a snap
        float followStiffness = 220f;
        posSpringX.stiffness(followStiffness).damping(2f * (float) Math.sqrt(followStiffness)).mass(1f);
        posSpringY.stiffness(followStiffness).damping(2f * (float) Math.sqrt(followStiffness)).mass(1f);
        posSpringX.setTarget(targetX);
        posSpringY.setTarget(targetY);
        posSpringX.update(animDt);
        posSpringY.update(animDt);
        renderX = posSpringX.getValue();
        renderY = posSpringY.getValue();

        float deltaX = renderX - lastRenderX;
        float rotTargetDeg;
        if (deltaX > 0) {
            rotTargetDeg = Math.min(18f, 5f + deltaX * 6.0f);
        } else if (deltaX < 0) {
            rotTargetDeg = Math.max(-18f, -5f - (-deltaX) * 6.0f);
        } else {
            rotTargetDeg = 0;
        }
        // the tilt follows on a spring too
        float rotSpeed = panelDragging ? 150f : 110f;
        float rotTargetRad = rotTargetDeg * 0.017453292f;
        rotSpring.stiffness(rotSpeed).damping(2f * (float) Math.sqrt(rotSpeed)).mass(1f);
        rotSpring.setTarget(rotTargetRad);
        rotSpring.update(animDt);
        rotation = rotSpring.getValue();
        if (!panelDragging && Math.abs(rotation) < 0.0002f) {
            rotation = 0;
        }
        lastRenderX = renderX;
        int px = Math.round(renderX);
        int py = Math.round(renderY);
        lastPanelHeight = ph;
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
            int borderColor = ColorUtility.applyAlpha(ModuleManager.get(ClickGui.class).outlineColor, animAlpha);
            Render2DUtility.drawSmoothRoundOutline(g, px, py, pw, ph, cornerRadius, 1, borderColor);
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
        hoveredModule = null;
        int btnH = (int) ModuleManager.get(ClickGui.class).buttonHeight;
        long nowNanos = System.nanoTime();
        float expandDt = Math.min(0.05f, (nowNanos - lastExpandNanos) / 1_000_000_000f);
        lastExpandNanos = nowNanos;
        float expandStep = 7f * expandDt;
        for (int i = modulePanelScroll; i < entries.size() && drawn < 30; i++) {
            HudModuleEntry entry = entries.get(i);
            Module hud = entry.getModule();
            boolean hovered = mx >= px + 2 && mx <= px + pw - 2 && my >= cy && my <= cy + btnH;
            if (hovered) hoveredModule = hud;
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
            entry.render(g, px + 2 + Math.round(shakeOf(hud)), cy, pw - 4, hovered, enabled, alpha, accentColor, mx, my, expanded, expandAnim);
            cy += rowH;
            drawn++;
            if (expandAnim > 0.005f) {
                int totalParamH = 0;
                int rowCount = 0;
                List<ParameterElement> params = entry.getParamEntries();
                for (ParameterElement pe : params) {
                    pe.updateAnimations();
                    if (!pe.getParameter().isVisible()) continue;
                    int ph2 = pe.getHeight();
                    if (ph2 <= 0) continue;
                    totalParamH += ph2;
                    rowCount++;
                }
                float easedExpand = AnimationUtility.Easing.QUINT_OUT.apply(expandAnim);
                int actualH = (int) (totalParamH * easedExpand);
                if (actualH > 0) {
                    // the parameter block sits inside the row, same offsets as the click gui
                    int paramX = px + 5;
                    int paramY = cy;
                    int paramW = pw - 10;
                    int btnAlpha = (int) ModuleManager.get(ClickGui.class).buttonOpacity;
                    int btnRadius = Math.min((int) ModuleManager.get(ClickGui.class).cornerRadius, btnH / 2);
                    int bgAlpha = (int) (Math.max(btnAlpha / 2, 24) * animAlpha);
                    Render2DUtility.drawRound(g, paramX, paramY, paramW, actualH,
                        Math.max(4, btnRadius - 2), ColorUtility.withAlpha(0x0A0A14, bgAlpha));

                    int accentH = (int) (actualH * AnimationUtility.Easing.CUBIC_OUT.apply(expandAnim));
                    if (accentH > 1) {
                        int barColor = ColorUtility.withAlpha(accentColor, (int) (expandAnim * 150 * animAlpha));
                        Render2DUtility.drawRound(g, paramX + 2, paramY + 1, 2, accentH, 1, barColor);
                    }

                    float lastDelay = rowCount > 1 ? (rowCount - 1) * 0.06f : 0f;
                    float delayScale = lastDelay > 0.9f ? 0.9f / lastDelay : 1f;
                    float rowSpan = Math.max(0.1f, 1f - lastDelay * delayScale);
                    Render2DUtility.pushScissor(g, paramX, paramY, paramW, actualH);
                    int pcy = paramY;
                    int rowIdx = 0;
                    for (ParameterElement pe : params) {
                        if (!pe.getParameter().isVisible()) continue;
                        int ph2 = pe.getHeight();
                        if (ph2 <= 0) continue;
                        float delay = rowIdx * 0.06f * delayScale;
                        float rowT = AnimationUtility.clamp((expandAnim - delay) / rowSpan, 0f, 1f);
                        float rowEased = AnimationUtility.Easing.QUINT_OUT.apply(rowT);
                        int slide = (int) ((1f - rowEased) * 6f);
                        pe.render(g, paramX + 2, pcy + slide, paramW, ph2, mx, my);
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
                for (ParameterElement pe : entry.getParamEntries()) {
                    if (!pe.getParameter().isVisible()) continue;
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
                        if (hud.getParameters().isEmpty()) {
                            ravex.utility.sound.SoundUtility.playNoSettings();
                            shakeStartNs.put(hud, System.nanoTime());
                        } else {
                            toggleExpanded(hud);
                        }
                        return true;
                    }
                    return true;
                }
                cy += rowH;
                boolean expanded = expandedModules.contains(hud);
                if (expanded) {
                    for (ParameterElement pe : entry.getParamEntries()) {
                        if (!pe.getParameter().isVisible()) continue;
                        int ph = pe.getHeight();
                        if (ph <= 0) continue;
                        if (my >= cy && my <= cy + ph) {
                            if (pe.mouseClicked(mx, my, btn, px + 7, cy, pw - 10, ph)) {
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

    // wobble for a right click with no settings
    private float shakeOf(Module m) {
        Long start = shakeStartNs.get(m);
        if (start == null) return 0f;
        float t = (System.nanoTime() - start) / 240_000_000f;
        if (t >= 1f) {
            shakeStartNs.remove(m);
            return 0f;
        }
        return (float) Math.sin(t * Math.PI * 6.0) * 3.5f * (1f - t);
    }
}
