package ravex.gui.hudeditor;
import net.minecraft.client.gui.GuiGraphics;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.parameter.Parameter;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;
import java.util.ArrayList;
import java.util.List;
public class HudModuleEntry {
    private final Module module;
    private final List<HudParameterEntry> paramEntries = new ArrayList<>();
    private float hoverProgress;
    public HudModuleEntry(Module module) {
        this.module = module;
        for (Parameter<?> p : module.getParameters()) {
            paramEntries.add(new HudParameterEntry(p));
        }
    }
    public Module getModule() { return module; }
    public List<HudParameterEntry> getParamEntries() { return paramEntries; }
    public void render(GuiGraphics g, int x, int y, int width, boolean hovered, boolean enabled, int alpha, int accentColor, int mouseX, int mouseY, boolean expanded, float expandAnim) {
        float target = hovered ? 1f : 0f;
        hoverProgress += (target - hoverProgress) * 0.2f;
        if (Math.abs(hoverProgress - target) < 0.005f) hoverProgress = target;
        int bgColor;
        if (expanded) {
            bgColor = ColorUtility.withAlpha(accentColor, (int)(40 * (alpha / 255f)));
        } else if (enabled) {
            bgColor = ColorUtility.withAlpha(accentColor, (int)(50 * (alpha / 255f)));
        } else {
            bgColor = ColorUtility.withAlpha(0xFFFFFF, (int)(8 * (alpha / 255f)));
        }
        Render2DEngine.drawRound(g, x, y, width, 18, 4, bgColor);
        if (hovered && enabled) {
            Render2DEngine.drawRound(g, x, y, width, 18, 4,
                ColorUtility.withAlpha(accentColor, (int)(20 * (alpha / 255f))));
        }
        int textColor = enabled
            ? ColorUtility.withAlpha(0xFFFFFFFF, alpha)
            : ColorUtility.withAlpha(0xFF707080, alpha);
        FontRenderUtility.drawString(g, module.getName(), x + 6, y + 4, textColor, true);
        // No circle indicator
    }
    public int getExpandedParamCount() {
        int count = 0;
        for (HudParameterEntry pe : paramEntries) {
            if (pe.getExpandProgress() > 0.005f) count++;
        }
        return count;
    }
    public boolean mouseClickedParam(int mouseX, int mouseY, int x, int y, int width) {
        int cy = y + 18;
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
        int cy = y + 18;
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
