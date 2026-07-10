package ravex.gui.hudeditor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ColorPaletteModal;
import ravex.gui.clickgui.ColorUtility;
import ravex.parameter.*;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;

public class HudParameterEntry {
    private final Parameter<?> param;
    private boolean expanded = false;
    private boolean isDraggingSlider = false;
    private float sliderKnobAnim = -1f;

    public HudParameterEntry(Parameter<?> param) {
        this.param = param;
    }

    public Parameter<?> getParam() {
        return param;
    }

    public void update(boolean parentExpanded) {
        if (!parentExpanded) {
            expanded = false;
        }
    }

    public float getExpandProgress() {
        return 1f;
    }

    public int getHeight() {
        if (param instanceof NumberParameter) return 28;
        if (param instanceof ModeParameter mp && expanded) {
            return 16 + 14 * mp.getModes().size();
        }
        return 16;
    }

    public void render(GuiGraphics g, int x, int y, int width, int alpha, int accentColor, int mouseX, int mouseY) {
        int h = getHeight();
        if (h <= 0) return;
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 16;
        if (hovered && alpha > 10) {
            Render2DEngine.drawRound(g, x, y, width, 16, 3,
                ColorUtility.withAlpha(accentColor, (int)(15 * (alpha / 255f))));
        }
        String label = param.getName();
        FontRenderUtility.drawString(g, label, x + 6, y + 4,
            ColorUtility.withAlpha(0xFFB0B0C0, alpha), true);

        if (param instanceof ColorParameter cp) {
            int chipX = x + width - 26;
            int chipY = y + 3;
            int chipSize = 10;
            int argb = cp.getValue();
            Render2DEngine.drawRound(g, chipX - 1, chipY - 1, chipSize + 2, chipSize + 2, 2, ColorUtility.withAlpha(0xFF2A2A3A, alpha));
            g.fill(chipX, chipY, chipX + chipSize / 2, chipY + chipSize / 2, 0xFF444444);
            g.fill(chipX + chipSize / 2, chipY, chipX + chipSize, chipY + chipSize / 2, 0xFF888888);
            g.fill(chipX, chipY + chipSize / 2, chipX + chipSize / 2, chipY + chipSize, 0xFF888888);
            g.fill(chipX + chipSize / 2, chipY + chipSize / 2, chipX + chipSize, chipY + chipSize, 0xFF444444);
            g.fill(chipX, chipY, chipX + chipSize, chipY + chipSize, argb);
        } else if (param instanceof BooleanParameter bp) {
            int swW = 18;
            int swH = 9;
            int swX = x + width - swW - 6;
            int swY = y + (16 - swH) / 2;

            int trackColor = bp.getValue() ? ColorUtility.withAlpha(accentColor, alpha) : ColorUtility.withAlpha(0xFF2A2A3A, alpha);
            Render2DEngine.drawRound(g, swX, swY, swW, swH, swH / 2, trackColor);

            int knobSize = 7;
            int knobDrawX = bp.getValue() ? (swX + swW - knobSize - 1) : (swX + 1);
            int knobDrawY = swY + 1;
            Render2DEngine.drawRound(g, knobDrawX, knobDrawY, knobSize, knobSize, knobSize / 2, ColorUtility.withAlpha(0xFFFFFFFF, alpha));
        } else if (param instanceof NumberParameter np) {
            double min = np.getMin();
            double max = np.getMax();
            double val = np.getValue();
            double progress = (val - min) / (max - min);

            String valStr = String.format("%.1f", val);
            int valW = FontRenderUtility.getStringWidth(valStr);
            FontRenderUtility.drawString(g, valStr, x + width - valW - 6, y + 4, ColorUtility.withAlpha(accentColor, alpha), true);

            int slX = x + 6;
            int slY = y + 16;
            int slW = width - 12;
            int slH = 4;

            if (isDraggingSlider) {
                if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(net.minecraft.client.Minecraft.getInstance().getWindow().handle(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_RELEASE) {
                    isDraggingSlider = false;
                } else {
                    double relative = (double)(mouseX - slX) / slW;
                    relative = Math.max(0.0, Math.min(1.0, relative));
                    double newValue = min + relative * (max - min);
                    double step = np.getStep();
                    newValue = Math.round(newValue / step) * step;
                    np.setValue(newValue);
                }
            }

            float targetKnobX = slX + (float)(slW * progress);
            if (sliderKnobAnim < slX - 1 || sliderKnobAnim > slX + slW + 1) {
                sliderKnobAnim = targetKnobX;
            }
            sliderKnobAnim += (targetKnobX - sliderKnobAnim) * 0.2f;
            float animKnobX = Math.max(slX, Math.min(slX + slW, sliderKnobAnim));

            Render2DEngine.drawRound(g, slX, slY, slW, slH, slH / 2, ColorUtility.withAlpha(0xFF1A1A2A, alpha));
            float fillW = animKnobX - slX;
            if (fillW > 0) {
                Render2DEngine.drawRound(g, slX, slY, (int)Math.ceil(fillW), slH, slH / 2, ColorUtility.withAlpha(accentColor, alpha));
            }

            float knobSize = 8f;
            float knobDrawX = animKnobX - knobSize / 2f;
            float knobDrawY = slY + (slH - knobSize) / 2f;
            Identifier knobTex = Render2DEngine.getSmoothCircle();
            g.pose().pushMatrix();
            g.pose().translate(knobDrawX, knobDrawY);
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, knobTex, 0, 0, 0f, 0f, (int) knobSize, (int) knobSize, (int) knobSize, (int) knobSize, 0xFFFFFFFF);
            g.pose().popMatrix();
        } else if (param instanceof ModeParameter mp) {
            String valStr = mp.getValue();
            if (expanded) {

                FontRenderUtility.drawString(g, valStr, x + width - 6 - FontRenderUtility.getStringWidth(valStr),
                    y + 4, ColorUtility.withAlpha(0xFF808090, alpha), true);

                int modeY = y + 16;
                for (String m : mp.getModes()) {
                    boolean isCurrent = m.equals(mp.getValue());
                    boolean mHovered = mouseX >= x && mouseX <= x + width && mouseY >= modeY && mouseY <= modeY + 14;

                    int mBg = mHovered ? ColorUtility.withAlpha(accentColor, 30) : 0;
                    if (mBg != 0) {
                        g.fill(x + 2, modeY, x + width - 2, modeY + 14, mBg);
                    }
                    if (isCurrent) {
                        g.fill(x + 2, modeY, x + 4, modeY + 14, accentColor);
                    }

                    int mCol = isCurrent ? accentColor : 0xFF808090;
                    if (mHovered) mCol = 0xFFFFFFFF;

                    FontRenderUtility.drawString(g, m, x + 10, modeY + 3, ColorUtility.withAlpha(mCol, alpha), true);
                    modeY += 14;
                }
            } else {

                int mw = FontRenderUtility.getStringWidth(valStr);
                int valX = x + width - mw - 6;
                FontRenderUtility.drawString(g, valStr, valX, y + 4, ColorUtility.withAlpha(accentColor, alpha), true);
            }
        } else {
            String valStr = getDisplay();
            FontRenderUtility.drawString(g, valStr, x + width - 6 - FontRenderUtility.getStringWidth(valStr),
                y + 4, ColorUtility.withAlpha(accentColor, alpha), true);
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int x, int y, int width, int btn) {
        int h = getHeight();
        if (h <= 0 || mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + h) return false;

        if (btn == 1) {
            expanded = !expanded;
            return true;
        }

        if (btn == 0) {
            if (param instanceof BooleanParameter bp) {
                bp.setValue(!bp.getValue());
                return true;
            } else if (param instanceof ColorParameter cp) {
                ColorPaletteModal palette = new ColorPaletteModal(cp);
                palette.setOnClose(() -> {});
                HudEditorScreen screen = (HudEditorScreen) net.minecraft.client.Minecraft.getInstance().screen;
                if (screen != null) {
                    screen.activeColorParameter = cp;
                    screen.activeColorPalette = palette;
                }
                return true;
            } else if (param instanceof NumberParameter np) {
                int slX = x + 6;
                int slW = width - 12;
                if (mouseY >= y + 14) {
                    isDraggingSlider = true;
                    double relative = (double)(mouseX - slX) / slW;
                    relative = Math.max(0.0, Math.min(1.0, relative));
                    double newValue = np.getMin() + relative * (np.getMax() - np.getMin());
                    double step = np.getStep();
                    newValue = Math.round(newValue / step) * step;
                    np.setValue(newValue);
                } else {
                    double v = np.getValue() + np.getStep();
                    if (v > np.getMax()) v = np.getMin();
                    np.setValue(v);
                }
                return true;
            } else if (param instanceof ModeParameter mp) {
                if (expanded) {
                    int modeY = y + 16;
                    for (String m : mp.getModes()) {
                        if (mouseY >= modeY && mouseY <= modeY + 14) {
                            mp.setValue(m);
                            return true;
                        }
                        modeY += 14;
                    }
                } else {
                    var modes = mp.getModes();
                    int idx = modes.indexOf(mp.getValue());
                    idx = (idx + 1) % modes.size();
                    mp.setValue(modes.get(idx));
                    return true;
                }
            }
        }
        return false;
    }

    private String getDisplay() {
        if (param instanceof BooleanParameter bp) return bp.getValue() ? "ON" : "OFF";
        if (param instanceof ColorParameter) return "";
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
