package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import ravex.parameter.Parameter;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.DependencyParameter;

public class ParameterElement {
    private final Parameter<?> parameter;
    private boolean isDragging = false;

    public ParameterElement(Parameter<?> parameter) {
        this.parameter = parameter;
    }

    public Parameter<?> getParameter() {
        return parameter;
    }

    private Parameter<?> getActualParameter() {
        Parameter<?> p = this.parameter;
        while (p instanceof DependencyParameter<?,?> dp) {
            p = dp.getDelegate();
        }
        return p;
    }

    public int getHeight() {
        if (!parameter.isVisible()) return 0;
        Parameter<?> actual = getActualParameter();
        if (actual instanceof NumberParameter) {
            return 16;
        }
        if (actual instanceof ColorParameter) {
            return 12;
        }
        return 12;
    }

    public void render(GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
        int paramBg = 0xFF0D0D13;
        graphics.fill(x, y, x + width, y + height, paramBg);

        int activeColor = ColorUtility.getActiveColor();
        Parameter<?> actual = getActualParameter();

        if (actual instanceof BooleanParameter bp) {
            // Draw Parameter Label on the left
            graphics.drawString(font, bp.getName(), x + 6, y + 2, 0xFF9E9EB0, false);

            // Draw Toggle Switch Pill on the right
            int swW = 16;
            int swH = 8;
            int swX = x + width - swW - 8;
            int swY = y + (height - swH) / 2;

            int trackCol = bp.getValue() ? activeColor : 0xFF2A2A35;
            graphics.fill(swX, swY, swX + swW, swY + swH, trackCol);

            int knobX = bp.getValue() ? (swX + swW - 6) : (swX + 1);
            graphics.fill(knobX, swY + 1, knobX + 5, swY + swH - 1, 0xFFFFFFFF);

        } else if (actual instanceof ModeParameter mp) {
            // Draw Parameter Label on the left
            graphics.drawString(font, mp.getName(), x + 6, y + 2, 0xFF9E9EB0, false);

            // Draw '< Mode >' selection on the right
            String modeVal = "< " + mp.getValue() + " >";
            int mw = font.width(modeVal);
            graphics.drawString(font, modeVal, x + width - mw - 6, y + 2, activeColor, false);

        } else if (actual instanceof NumberParameter np) {
            // Slider toolbar layout
            double min = np.getMin();
            double max = np.getMax();
            double val = np.getValue();
            double progress = (val - min) / (max - min);

            // Slider Label & current value
            String label = String.format("%s: %.1f", np.getName(), val);
            graphics.drawString(font, label, x + 6, y + 1, 0xFF9E9EB0, false);

            // Slider Track bounds
            int slX = x + 6;
            int slY = y + 10;
            int slW = width - 12;
            int slH = 2;

            // Handle active dragging inside rendering loop (real-time responsiveness)
            if (isDragging) {
                if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(net.minecraft.client.Minecraft.getInstance().getWindow().handle(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_RELEASE) {
                    isDragging = false;
                } else {
                    double relative = (double) (mouseX - slX) / slW;
                    relative = Math.max(0.0, Math.min(1.0, relative));
                    double newValue = min + relative * (max - min);
                    // Snap to step value
                    double step = np.getStep();
                    newValue = Math.round(newValue / step) * step;
                    np.setValue(newValue);
                }
            }

            // Draw background track line
            graphics.fill(slX, slY, slX + slW, slY + slH, 0xFF2A2A35);

            // Draw active progress line
            int fillW = (int) (slW * progress);
            graphics.fill(slX, slY, slX + fillW, slY + slH, activeColor);

            // Draw sliding knob handle
            int knobX = slX + fillW - 2;
            graphics.fill(knobX, slY - 2, knobX + 4, slY + slH + 2, 0xFFFFFFFF);
        } else if (actual instanceof ColorParameter cp) {
            // Label
            graphics.drawString(font, cp.getName(), x + 6, y + 2, 0xFF9E9EB0, false);
            // Colour chip on the right
            int chipX = x + width - 18;
            int chipY = y + 2;
            int argb  = cp.getValue();
            // Checkerboard behind chip (shows alpha)
            graphics.fill(chipX,     chipY,     chipX + 8,  chipY + 8,  0xFF888888);
            graphics.fill(chipX + 4, chipY,     chipX + 8,  chipY + 4,  0xFF444444);
            graphics.fill(chipX,     chipY + 4, chipX + 4,  chipY + 8,  0xFF444444);
            graphics.fill(chipX, chipY, chipX + 8, chipY + 8, argb);
            // Border
            int border = 0xFF606060;
            graphics.fill(chipX - 1, chipY - 1, chipX + 9, chipY,     border);
            graphics.fill(chipX - 1, chipY + 8, chipX + 9, chipY + 9, border);
            graphics.fill(chipX - 1, chipY - 1, chipX,     chipY + 9, border);
            graphics.fill(chipX + 8, chipY - 1, chipX + 9, chipY + 9, border);
        } else {
            // Fallback for general parameters
            String text = actual.getName() + ": " + actual.getValue();
            if (text.length() > 18) {
                text = text.substring(0, 16) + "..";
            }
            graphics.drawString(font, text, x + 6, y + 2, 0xFF9E9EB0, false);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width, int height) {
        if (!parameter.isVisible()) return false;
        
        Parameter<?> actual = getActualParameter();
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            if (actual instanceof BooleanParameter bp) {
                bp.setValue(!bp.getValue());
                playSound();
                return true;
            } else if (actual instanceof ModeParameter mp) {
                var modes = mp.getModes();
                int idx = modes.indexOf(mp.getValue());
                int next = (idx + 1) % modes.size();
                mp.setValue(modes.get(next));
                playSound();
                return true;
            } else if (actual instanceof NumberParameter np) {
                isDragging = true;
                return true;
            } else if (actual instanceof ColorParameter cp) {
                ClickGUI.activeColorParameter = cp;
                ClickGUI.activeColorPalette = new ColorPaletteModal(cp);
                playSound();
                return true;
            }
        }
        return false;
    }

    private void playSound() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.25f, 1.4f);
        }
    }
}
