package ravex.gui.hudeditor;
import net.minecraft.client.gui.GuiGraphics;
import ravex.gui.clickgui.ColorUtility;
import ravex.parameter.*;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;
public class HudParameterEntry {
    private final Parameter<?> param;
    public HudParameterEntry(Parameter<?> param) {
        this.param = param;
    }
    public Parameter<?> getParam() { return param; }
    public void update(boolean expanded) {}
    public float getExpandProgress() { return 1f; }
    public int getHeight() { return 16; }
    public void render(GuiGraphics g, int x, int y, int width, int alpha, int accentColor, int mouseX, int mouseY) {
        int h = getHeight();
        if (h <= 0) return;
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + h;
        if (hovered && alpha > 10) {
            Render2DEngine.drawRound(g, x, y, width, h, 3,
                ColorUtility.withAlpha(accentColor, (int)(15 * (alpha / 255f))));
        }
        String label = param.getName();
        FontRenderUtility.drawString(g, label, x + 6, y + (h - FontRenderUtility.getFontHeight()) / 2 + 1,
            ColorUtility.withAlpha(0xFFB0B0C0, alpha), true);
        String valStr = getDisplay();
        FontRenderUtility.drawString(g, valStr, x + width - 6 - FontRenderUtility.getStringWidth(valStr),
            y + (h - FontRenderUtility.getFontHeight()) / 2 + 1,
            ColorUtility.withAlpha(accentColor, alpha), true);
    }
    public boolean mouseClicked(int mouseX, int mouseY, int x, int y, int width) {
        int h = getHeight();
        return h > 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + h;
    }
    public void toggle() {
        if (param instanceof BooleanParameter bp) {
            bp.setValue(!bp.getValue());
        } else if (param instanceof ColorParameter cp) {
            int c = cp.getValue();
            int r = ((c >> 16) & 0xFF) + 40;
            int g = ((c >> 8) & 0xFF) + 40;
            int b = (c & 0xFF) + 40;
            cp.setValue((0xFF << 24) | ((r % 256) << 16) | ((g % 256) << 8) | (b % 256));
        } else if (param instanceof NumberParameter np) {
            double v = np.getValue() + np.getStep();
            if (v > np.getMax()) v = np.getMin();
            np.setValue(v);
        } else if (param instanceof ModeParameter mp) {
            var modes = mp.getModes();
            int idx = modes.indexOf(mp.getValue());
            idx = (idx + 1) % modes.size();
            mp.setValue(modes.get(idx));
        }
    }
    private String getDisplay() {
        if (param instanceof BooleanParameter bp) return bp.getValue() ? "\u00A7aON" : "\u00A7cOFF";
        if (param instanceof ColorParameter cp) {
            int c = cp.getValue();
            return String.format("#%02X%02X%02X", (c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
        }
        if (param instanceof NumberParameter np) {
            double v = np.getValue();
            if (v == (long) v) return String.valueOf((long) v);
            return String.format("%.1f", v);
        }
        if (param instanceof ModeParameter mp) return mp.getValue();
        if (param instanceof StringParameter sp) return "\"" + sp.getValue() + "\"";
        return String.valueOf(param.getValue());
    }
}
