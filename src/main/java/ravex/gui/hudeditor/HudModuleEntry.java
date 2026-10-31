package ravex.gui.hudeditor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.utility.render.ColorUtility;
import ravex.modules.Module;
import ravex.parameter.Parameter;
import ravex.gui.clickgui.ParameterElement;
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
    private final List<ParameterElement> paramEntries = new ArrayList<>();
    private final Render2DUtility.HoverGlowRenderer glowRenderer;
    private float hoverProgress;
    private float enableAnim;
    private boolean lastHovered;
    private boolean wasEnabled;
    private long enableStartNs = -1L;
    private float enableFrom = 0f;
    private boolean enableForward = true;
    private static final long ENABLE_MS = 240L;
    private static final long DISABLE_MS = 180L;

    public HudModuleEntry(Module module) {
        this.module = module;
        this.glowRenderer = new Render2DUtility.HoverGlowRenderer("hud_glow_" + module.getName());
        this.enableAnim = module.getEnabled() ? 1f : 0f;
        this.wasEnabled = module.getEnabled();
        for (Parameter<?> p : module.getParameters()) {
            paramEntries.add(new ParameterElement(p, module.getName()));
        }
    }

    public Module getModule() { return module; }
    public List<ParameterElement> getParamEntries() { return paramEntries; }

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
        if (enabled != wasEnabled) {
            enableForward = enabled;
            enableFrom = enableAnim;
            enableStartNs = System.nanoTime();
        }
        wasEnabled = enabled;

        // same eased timing as the click gui
        if (enableStartNs >= 0L) {
            long durNs = (enableForward ? ENABLE_MS : DISABLE_MS) * 1_000_000L;
            float t = Math.min(1f, (System.nanoTime() - enableStartNs) / (float) durNs);
            enableAnim = enableFrom + (targetAnim - enableFrom) * AnimationUtility.Easing.QUART_BOTH.apply(t);
            if (t >= 1f) enableStartNs = -1L;
        } else {
            enableAnim = targetAnim;
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
        if (ModuleManager.get(ClickGui.class).outlines) {
            // outline alpha scaled, not replaced, like the click gui
            Render2DUtility.drawPixelPerfectRoundBorder(g, x, y, width, btnH, btnRadius, 1,
                ColorUtility.applyAlpha(ModuleManager.get(ClickGui.class).outlineColor, alpha / 255f));
        }

        if (hoverProgress > 0.01f) {
            int glowAlpha = (int) (hoverProgress * 120 * (alpha / 255f));
            int whiteGlow = ColorUtility.withAlpha(0xFFFFFFFF, glowAlpha);
            glowRenderer.render(g, x, y, width, btnH, btnRadius,
                (float) mouseX, (float) (y + btnH / 2.0f), 26.0f, 13.0f, whiteGlow);
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
        for (ParameterElement pe : paramEntries) {
            if (pe.getExpandAnimProgress() > 0.005f) count++;
        }
        return count;
    }
}
