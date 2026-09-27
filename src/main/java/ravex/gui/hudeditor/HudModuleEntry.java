package ravex.gui.hudeditor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.utility.render.ColorUtility;
import ravex.modules.Module;
import ravex.parameter.Parameter;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.TextureLoaderUtility;
import ravex.utility.render.animate.AnimationUtility;
import ravex.modules.client.ClickGui;
import ravex.manager.ModuleManager;
import ravex.event.EventBusHolder;
import ravex.event.client.SoundEvent;
import java.util.ArrayList;
import java.util.List;

public class HudModuleEntry {
    private final Module module;
    private final List<HudParameterEntry> paramEntries = new ArrayList<>();
    private float hoverProgress;
    private float enableAnim;
    private boolean lastHovered;

    public HudModuleEntry(Module module) {
        this.module = module;
        this.enableAnim = module.getEnabled() ? 1f : 0f;
        for (Parameter<?> p : module.getParameters()) {
            paramEntries.add(new HudParameterEntry(p));
        }
    }

    public Module getModule() { return module; }
    public List<HudParameterEntry> getParamEntries() { return paramEntries; }

    public void render(GuiGraphics g, int x, int y, int width, boolean hovered, boolean enabled, int alpha, int accentColor, int mouseX, int mouseY, boolean expanded, float expandAnim) {
        if (hovered) {
            if (!lastHovered) {
                EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.HOVER, 0.5f));
            }
            hoverProgress = Math.min(1.0f, hoverProgress + 0.12f);
        } else {
            hoverProgress = Math.max(0.0f, hoverProgress - 0.10f);
        }
        lastHovered = hovered;

        float targetAnim = enabled ? 1.0f : 0.0f;
        if (enableAnim < targetAnim) {
            enableAnim = Math.min(targetAnim, enableAnim + 0.25f);
        } else if (enableAnim > targetAnim) {
            enableAnim = Math.max(targetAnim, enableAnim - 0.30f);
        }

        int btnH = (int) ModuleManager.get(ClickGui.class).buttonHeight;
        int btnAlpha = (int) ModuleManager.get(ClickGui.class).buttonOpacity;
        int btnRadius = Math.min((int) ModuleManager.get(ClickGui.class).cornerRadius, btnH / 2);

        int disabledBg = ColorUtility.withAlpha(0x252530, (int) (btnAlpha * (alpha / 255f)));
        int enabledBg = ColorUtility.withAlpha(accentColor, (int) (Math.min(255, btnAlpha * 3) * (alpha / 255f)));
        int mergedBg = ColorUtility.interpolate(disabledBg, enabledBg, enableAnim);

        if (hoverProgress > 0.01f && enableAnim < 0.01f) {
            int hoverAlpha = (int) (hoverProgress * Math.min(30, btnAlpha / 2) * (alpha / 255f));
            mergedBg = ColorUtility.withAlpha(ColorUtility.interpolate(mergedBg, 0xFFFFFFFF, hoverProgress * 0.2f), (mergedBg >> 24) & 0xFF);
        }

        Render2DUtility.drawPixelPerfectRound(g, x, y, width, btnH, btnRadius, mergedBg);

        if (hoverProgress > 0.01f) {
            int glowAlpha = (int) (hoverProgress * 100 * (alpha / 255f));
            int whiteGlow = ColorUtility.withAlpha(0xFFFFFFFF, glowAlpha);
            Render2DUtility.pushScissor(g, x, y, width, btnH);
            Render2DUtility.drawGaussianShadow(g, (float) mouseX - 8, (float) mouseY - 8, 16, 16, 10, whiteGlow);
            Render2DUtility.popScissor(g);
        }

        int baseColor = ColorUtility.interpolate(0xFFB0B0C0, accentColor, enableAnim);
        int textColor = hovered ? 0xFFFFFFFF : baseColor;
        textColor = ColorUtility.withAlpha(textColor, alpha);

        String name = ravex.utility.misc.LanguageUtility.moduleName(module.getName());
        String displayName = name.endsWith("Hud") ? name.substring(0, name.length() - 3) : name;

        int textY = y + (btnH - FontRenderUtility.getFontHeight()) / 2 + 1;
        FontRenderUtility.drawString(g, displayName, x + 6, textY, textColor, true);

        boolean hasParams = !module.getParameters().isEmpty();
        if (hasParams && ModuleManager.get(ClickGui.class).showGear) {
            Identifier settingsTex = TextureLoaderUtility.getSettingsWhiteTexture();
            if (settingsTex == null) settingsTex = TextureLoaderUtility.getSettingsTexture();
            if (settingsTex != null) {
                int iconSize = 10;
                int iconX = x + width - iconSize - 6;
                int iconY = y + (btnH - iconSize) / 2;
                float openProgress = AnimationUtility.Easing.CUBIC_OUT.apply(expandAnim);
                float angle = openProgress * 90f + module.getGearAngle();
                var pose = g.pose();
                pose.pushMatrix();
                pose.translate(iconX + iconSize / 2f, iconY + iconSize / 2f);
                if (Math.abs(angle) > 0.01f) {
                    pose.rotate(angle * (float) Math.PI / 180f);
                }
                pose.translate(-(iconX + iconSize / 2f), -(iconY + iconSize / 2f));
                g.blit(settingsTex, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f);
                pose.popMatrix();
            } else {
                String indicator = expanded ? "-" : "+";
                FontRenderUtility.drawString(g, indicator, x + width - 12, textY, 0xFF7A7A8A, true);
            }
        }
    }

    public int getExpandedParamCount() {
        int count = 0;
        for (HudParameterEntry pe : paramEntries) {
            if (pe.getExpandProgress() > 0.005f) count++;
        }
        return count;
    }

    public boolean mouseClickedParam(int mouseX, int mouseY, int x, int y, int width) {
        int btnH = (int) ModuleManager.get(ClickGui.class).buttonHeight;
        int cy = y + btnH;
        for (HudParameterEntry pe : paramEntries) {
            if (!pe.getParam().isVisible()) continue;
            int ph = pe.getHeight();
            if (ph <= 0) { cy += ph; continue; }
            if (pe.mouseClicked(mouseX, mouseY, x + 2, cy, width - 4, 0)) return true;
            cy += ph;
        }
        return false;
    }

    public HudParameterEntry getParamAt(int mouseX, int mouseY, int x, int y, int width) {
        int btnH = (int) ModuleManager.get(ClickGui.class).buttonHeight;
        int cy = y + btnH;
        for (HudParameterEntry pe : paramEntries) {
            if (!pe.getParam().isVisible()) continue;
            int ph = pe.getHeight();
            if (ph <= 0) { cy += ph; continue; }
            if (mouseX >= x + 2 && mouseX <= x + width - 2 && mouseY >= cy && mouseY <= cy + ph) return pe;
            cy += ph;
        }
        return null;
    }
}
