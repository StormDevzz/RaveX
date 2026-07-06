package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.parameter.Parameter;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.KeybindParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

public class ParameterElement {
    private final Parameter<?> parameter;
    private boolean isDragging = false;
    private float toggleAnimProgress = 0f;
    private long toggleLastUpdate = 0;
    private long modeExpandedAt = 0;
    
    private float expandAnimProgress = 0f;
    private float dropdownAnimProgress = 0f;
    private float sliderKnobAnim = 0f;
    private boolean isEditingNumber = false;
    private String numberInputText = "";
    private long lastAnimTime = 0;

    public ParameterElement(Parameter<?> parameter) {
        this.parameter = parameter;
        this.expandAnimProgress = parameter.isVisible() ? 1.0f : 0.0f;
        this.dropdownAnimProgress = parameter.isExpanded() ? 1.0f : 0.0f;
    }

    public float getExpandAnimProgress() {
        return expandAnimProgress;
    }

    public Parameter<?> getParameter() {
        return parameter;
    }

    public void updateAnimations() {
        long now = System.currentTimeMillis();
        if (now == lastAnimTime) return;
        long delta = now - lastAnimTime;
        if (lastAnimTime == 0) {
            lastAnimTime = now;
            return;
        }
        lastAnimTime = now;
        if (delta > 100) delta = 16;

        boolean smoothOption = ravex.modules.render.ClickGui.INSTANCE.smoothOption.getValue();
        float optionSmoothness = ravex.modules.render.ClickGui.INSTANCE.optionSmoothness.getValue().floatValue();

        
        float targetExpand = parameter.isVisible() ? 1.0f : 0.0f;
        if (smoothOption) {
            float speed = (optionSmoothness / 100f) * (delta / 16f);
            if (expandAnimProgress < targetExpand) {
                expandAnimProgress = Math.min(targetExpand, expandAnimProgress + speed);
            } else if (expandAnimProgress > targetExpand) {
                expandAnimProgress = Math.max(targetExpand, expandAnimProgress - speed);
            }
        } else {
            expandAnimProgress = targetExpand;
        }

        
        float targetDropdown = parameter.isExpanded() ? 1.0f : 0.0f;
        if (smoothOption) {
            float speed = (optionSmoothness / 100f) * (delta / 16f);
            if (dropdownAnimProgress < targetDropdown) {
                dropdownAnimProgress = Math.min(targetDropdown, dropdownAnimProgress + speed);
            } else if (dropdownAnimProgress > targetDropdown) {
                dropdownAnimProgress = Math.max(targetDropdown, dropdownAnimProgress - speed);
            }
        } else {
            dropdownAnimProgress = targetDropdown;
        }
    }

    public int getHeight() {
        updateAnimations();
        if (!parameter.isVisible() && expandAnimProgress < 0.001f) return 0;
        int baseH = 22;
        if (parameter instanceof NumberParameter) {
            baseH = 28;
        }
        int extraH = 0;
        if (parameter instanceof ModeParameter mp) {
            extraH = 18 * mp.getModes().size();
        }
        int totalH = baseH + (int) (extraH * dropdownAnimProgress);
        return (int) (totalH * expandAnimProgress);
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        updateAnimations();
        if (height <= 0) return;

        graphics.enableScissor(x, y, x + width, y + height);

        long now = System.currentTimeMillis();
        if (toggleLastUpdate == 0) toggleLastUpdate = now;
        long delta = now - toggleLastUpdate;
        toggleLastUpdate = now;
        if (delta > 100) delta = 16;

        int activeColor = ColorUtility.getActiveColor();
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        int bg = hovered ? 0xFF111122 : 0xFF0D0D18;
        graphics.fill(x, y, x + width, y + height, bg);

        boolean switchless = ravex.modules.render.ClickGui.INSTANCE.switchless.getValue();

        if (parameter instanceof BooleanParameter bp) {
            if (bp.getValue()) {
                toggleAnimProgress = Math.min(1.0f, toggleAnimProgress + delta * 0.015f);
            } else {
                toggleAnimProgress = Math.max(0.0f, toggleAnimProgress - delta * 0.015f);
            }

            if (switchless) {
                if (toggleAnimProgress > 0.01f) {
                    int glowAlpha = (int) (toggleAnimProgress * 0x22);
                    graphics.fill(x, y, x + width, y + height, ColorUtility.withAlpha(activeColor, glowAlpha));
                }
                int baseCol = lerpColor(0xFF8F8FA0, activeColor, toggleAnimProgress);
                int textCol = hovered ? lerpColor(0xFFC0C0D0, 0xFFFFFFFF, toggleAnimProgress) : baseCol;
                FontRenderUtility.drawString(graphics, bp.getName(), x + 8, y + 7, textCol, true);
            } else {
                int textCol = bp.getValue() ? 0xFFD0D0E0 : 0xFF9090A0;
                FontRenderUtility.drawString(graphics, bp.getName(), x + 8, y + 7, textCol, true);

                int swW = 26;
                int swH = 14;
                int swX = x + width - swW - 8;
                int swY = y + (height - swH) / 2;

                int trackColor = lerpColor(0xFF232330, activeColor, toggleAnimProgress);
                int trackBorderColor = lerpColor(0xFF35354A, ColorUtility.withAlpha(activeColor, 120), toggleAnimProgress);
                
                Render2DEngine.drawSmoothRound(graphics, swX - 0.5f, swY - 0.5f, swW + 1.0f, swH + 1.0f, swH / 2.0f + 0.5f, trackBorderColor);
                Render2DEngine.drawSmoothRound(graphics, swX, swY, swW, swH, swH / 2.0f, trackColor);

                int knobSize = 12;
                int knobRange = swW - knobSize - 2;
                int knobX = swX + 1 + (int)(toggleAnimProgress * knobRange);
                int knobY = swY + 1;

                double stretch = Math.sin(toggleAnimProgress * Math.PI) * 2.5;
                float currentKnobWidth = (float) (knobSize + stretch);
                float drawKnobX = knobX - (float)(stretch / 2.0);
                float drawKnobY = knobY;

                Render2DEngine.drawSmoothRound(graphics, drawKnobX - 0.5f, drawKnobY + 0.5f, currentKnobWidth + 1.0f, knobSize + 1.0f, knobSize / 2.0f, 0x1F000000);
                Render2DEngine.drawSmoothRound(graphics, drawKnobX, drawKnobY, currentKnobWidth, knobSize, knobSize / 2.0f, 0xFFFFFFFF);
            }

        } else if (parameter instanceof ModeParameter mp) {
            FontRenderUtility.drawString(graphics, mp.getName(), x + 8, y + 7, 0xFFC0C0D0, true);

            if (dropdownAnimProgress > 0.01f) {
                int modeY = y + 22;
                for (String m : mp.getModes()) {
                    boolean isCurrent = m.equals(mp.getValue());
                    boolean mHovered = mouseX >= x && mouseX <= x + width && mouseY >= modeY && mouseY <= modeY + 18;

                    int mBg = mHovered ? 0x22FFFFFF : 0;
                    if (mBg != 0) {
                        graphics.fill(x + 2, modeY, x + width - 2, modeY + 18, mBg);
                    }
                    if (isCurrent) {
                        graphics.fill(x + 2, modeY, x + 4, modeY + 18, activeColor);
                        graphics.fill(x + 2, modeY + 17, x + width - 2, modeY + 18, ColorUtility.withAlpha(activeColor, 50));
                    }

                    int mCol = isCurrent ? activeColor : 0xFF808090;
                    if (mHovered) mCol = 0xFFFFFFFF;

                    FontRenderUtility.drawString(graphics, m, x + 14, modeY + 5, mCol, true);
                    modeY += 18;
                }
            } else {
                String modeVal = mp.getValue();
                int mw = FontRenderUtility.getStringWidth(modeVal);
                int valX = x + width - mw - 8;
                FontRenderUtility.drawString(graphics, modeVal, valX, y + 7, activeColor, true);

                FontRenderUtility.drawString(graphics, "<", valX - FontRenderUtility.getStringWidth("<") - 3, y + 7, ColorUtility.withAlpha(activeColor, 120), true);
                FontRenderUtility.drawString(graphics, ">", x + width - 8, y + 7, ColorUtility.withAlpha(activeColor, 120), true);
            }

        } else if (parameter instanceof NumberParameter np) {
            double min = np.getMin();
            double max = np.getMax();
            double val = np.getValue();
            double progress = (val - min) / (max - min);

            FontRenderUtility.drawString(graphics, np.getName(), x + 8, y + 5, 0xFFC0C0D0, true);

            String valStr;
            if (isEditingNumber) {
                valStr = numberInputText;
                if ((System.currentTimeMillis() / 500) % 2 == 0) {
                    valStr += "_";
                }
            } else {
                valStr = String.format("%.1f", val);
            }
            int valW = FontRenderUtility.getStringWidth(valStr);
            FontRenderUtility.drawString(graphics, valStr, x + width - valW - 8, y + 5, activeColor, true);

            int slX = x + 8;
            int slY = y + 16;
            int slW = width - 16;
            int slH = 4;

            if (isDragging) {
                if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(net.minecraft.client.Minecraft.getInstance().getWindow().handle(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_RELEASE) {
                    isDragging = false;
                    if (np.getName().equalsIgnoreCase("Gui Scale")) {
                        ClickGUI.isDraggingSlider = false;
                    }
                } else {
                    double relative = (double) (mouseX - slX) / slW;
                    relative = Math.max(0.0, Math.min(1.0, relative));
                    double newValue = min + relative * (max - min);
                    double step = np.getStep();
                    newValue = Math.round(newValue / step) * step;
                    np.setValue(newValue);
                }
            }

            boolean slHovered = mouseX >= slX && mouseX <= slX + slW && mouseY >= slY - 4 && mouseY <= slY + slH + 4;
            boolean active = slHovered || isDragging;
            float targetKnob = active ? 1.0f : 0.0f;
            if (sliderKnobAnim < targetKnob) {
                sliderKnobAnim = Math.min(targetKnob, sliderKnobAnim + delta * 0.008f);
            } else if (sliderKnobAnim > targetKnob) {
                sliderKnobAnim = Math.max(targetKnob, sliderKnobAnim - delta * 0.008f);
            }

            int baseKnobSize = 10;
            int knobRange = slW - baseKnobSize;
            int knobX = slX + baseKnobSize / 2 + (int)(knobRange * progress);

            Render2DEngine.drawSmoothRound(graphics, slX, slY, slW, slH, slH / 2.0f, 0xFF1D1D2A);
            
            if (knobX > slX) {
                Render2DEngine.drawSmoothRound(graphics, slX, slY, (float)(knobX - slX), slH, slH / 2.0f, activeColor);
            }

            float currentKnobSize = 8.0f + sliderKnobAnim * 3.5f;
            float knobDrawX = knobX - currentKnobSize / 2.0f;
            float knobDrawY = (slY + slH / 2.0f) - currentKnobSize / 2.0f;

            int outerColor = lerpColor(0xFF808090, activeColor, sliderKnobAnim);
            
            Render2DEngine.drawSmoothRound(graphics, knobDrawX - 0.5f, knobDrawY + 0.5f, currentKnobSize + 1.0f, currentKnobSize + 1.0f, currentKnobSize / 2.0f, 0x1B000000);
            Render2DEngine.drawSmoothRound(graphics, knobDrawX, knobDrawY, currentKnobSize, currentKnobSize, currentKnobSize / 2.0f, outerColor);
            
            float innerSize = currentKnobSize * 0.5f;
            float innerDrawX = knobX - innerSize / 2.0f;
            float innerDrawY = (slY + slH / 2.0f) - innerSize / 2.0f;
            Render2DEngine.drawSmoothRound(graphics, innerDrawX, innerDrawY, innerSize, innerSize, innerSize / 2.0f, 0xFFFFFFFF);

        } else if (parameter instanceof ColorParameter cp) {
            FontRenderUtility.drawString(graphics, cp.getName(), x + 8, y + 7, 0xFFC0C0D0, true);

            int chipX = x + width - 24;
            int chipY = y + 6;
            int chipSize = 10;
            int argb = cp.getValue();

            graphics.fill(chipX - 1, chipY - 1, chipX + chipSize + 1, chipY, ColorUtility.withAlpha(activeColor, 40));
            graphics.fill(chipX - 1, chipY + chipSize, chipX + chipSize + 1, chipY + chipSize + 1, ColorUtility.withAlpha(activeColor, 40));
            graphics.fill(chipX - 1, chipY - 1, chipX, chipY + chipSize + 1, ColorUtility.withAlpha(activeColor, 40));
            graphics.fill(chipX + chipSize, chipY - 1, chipX + chipSize + 1, chipY + chipSize + 1, ColorUtility.withAlpha(activeColor, 40));

            graphics.fill(chipX, chipY, chipX + chipSize, chipY + chipSize, 0xFF888888);
            graphics.fill(chipX + chipSize / 2, chipY, chipX + chipSize, chipY + chipSize / 2, 0xFF444444);
            graphics.fill(chipX, chipY + chipSize / 2, chipX + chipSize / 2, chipY + chipSize, 0xFF444444);
            graphics.fill(chipX, chipY, chipX + chipSize, chipY + chipSize, argb);

        } else if (parameter instanceof ravex.parameter.ActionParameter ap) {
            FontRenderUtility.drawString(graphics, ap.getName(), x + 8, y + 7, 0xFFC0C0D0, true);
            String text = "Configure >";
            int tw = FontRenderUtility.getStringWidth(text);
            FontRenderUtility.drawString(graphics, text, x + width - tw - 8, y + 7, activeColor, true);

        } else if (parameter instanceof ravex.parameter.StringParameter sp) {
            FontRenderUtility.drawString(graphics, sp.getName(), x + 8, y + 7, 0xFFC0C0D0, true);
            boolean isFocused = ClickGUI.activeStringParameterElement != null && ClickGUI.activeStringParameterElement.getParameter() == sp;
            String text = sp.getValue();
            if (isFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
                text += "_";
            }
            int tw = FontRenderUtility.getStringWidth(text);
            FontRenderUtility.drawString(graphics, text, x + width - tw - 8, y + 7, activeColor, true);

        } else if (parameter instanceof KeybindParameter kp) {
            FontRenderUtility.drawString(graphics, kp.getName(), x + 8, y + 7, 0xFFC0C0D0, true);
            boolean isListening = ClickGUI.activeKeybindElement != null && ClickGUI.activeKeybindElement.getParameter() == kp;
            String keyText = isListening ? "..." : KeybindParameter.getKeyName(kp.getValue());
            int tw = FontRenderUtility.getStringWidth(keyText);
            int keyCol = isListening ? 0xFF00FF00 : activeColor;
            FontRenderUtility.drawString(graphics, keyText, x + width - tw - 8, y + 7, keyCol, true);

        } else {
            String text = parameter.getName() + ": " + parameter.getValue();
            if (text.length() > 18) {
                text = text.substring(0, 16) + "..";
            }
            FontRenderUtility.drawString(graphics, text, x + 8, y + 7, 0xFFC0C0D0, true);
        }
        graphics.disableScissor();
    }

    private static int lerpColor(int from, int to, float t) {
        int a = (int)(((from >> 24) & 0xFF) * (1 - t) + ((to >> 24) & 0xFF) * t);
        int r = (int)(((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
        int g = (int)(((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
        int b = (int)((from & 0xFF) * (1 - t) + (to & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width, int height) {
        if (!parameter.isVisible()) return false;

        if (isEditingNumber) {
            if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
                applyInput();
                if (ClickGUI.activeNumberParameterElement == this) {
                    ClickGUI.activeNumberParameterElement = null;
                }
            }
        }
        
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            if (parameter instanceof ModeParameter mp && mp.isExpanded()) {
                int modeY = y + 22;
                for (String m : mp.getModes()) {
                    if (mouseX >= x && mouseX <= x + width && mouseY >= modeY && mouseY <= modeY + 18) {
                        mp.setValue(m);
                        playSound();
                        return true;
                    }
                    modeY += 18;
                }
            }

            if (parameter instanceof NumberParameter np) {
                if (button == 1 || button == 2) {
                    if (isEditingNumber) {
                        isEditingNumber = false;
                        if (ClickGUI.activeNumberParameterElement == this) {
                            ClickGUI.activeNumberParameterElement = null;
                        }
                    } else {
                        if (ClickGUI.activeNumberParameterElement != null && ClickGUI.activeNumberParameterElement != this) {
                            ClickGUI.activeNumberParameterElement.applyInput();
                        }
                        ClickGUI.activeNumberParameterElement = this;
                        isEditingNumber = true;
                        numberInputText = "";
                    }
                    playSound();
                    return true;
                }
            }

            if (button == 1) {
                parameter.setExpanded(!parameter.isExpanded());
                playSound();
                return true;
            }
            if (parameter instanceof BooleanParameter bp) {
                bp.setValue(!bp.getValue());
                playSound();
                return true;
            } else if (parameter instanceof ModeParameter mp) {
                var modes = mp.getModes();
                int idx = modes.indexOf(mp.getValue());
                int next = (idx + 1) % modes.size();
                mp.setValue(modes.get(next));
                playSound();
                return true;
            } else if (parameter instanceof NumberParameter np) {
                isDragging = true;
                if (np.getName().equalsIgnoreCase("Gui Scale")) {
                    ClickGUI.isDraggingSlider = true;
                }
                return true;
            } else if (parameter instanceof ColorParameter cp) {
                ClickGUI.activeColorParameter = cp;
                ClickGUI.activeColorPalette = new ColorPaletteModal(cp);
                playSound();
                return true;
            } else if (parameter instanceof ravex.parameter.ActionParameter ap) {
                ap.getValue().run();
                playSound();
                return true;
            } else if (parameter instanceof ravex.parameter.StringParameter sp) {
                if (ClickGUI.activeStringParameterElement != null && ClickGUI.activeStringParameterElement.getParameter() == sp) {
                    ClickGUI.activeStringParameterElement = null;
                } else {
                    ClickGUI.activeStringParameterElement = this;
                }
                playSound();
                return true;
            } else if (parameter instanceof KeybindParameter kp) {
                if (ClickGUI.activeKeybindElement != null && ClickGUI.activeKeybindElement.getParameter() == kp) {
                    ClickGUI.activeKeybindElement = null;
                } else {
                    ClickGUI.activeKeybindElement = this;
                }
                playSound();
                return true;
            }
        }
        return false;
    }

    public void appendChar(char ch) {
        if (numberInputText.length() < 12) {
            numberInputText += ch;
        }
    }

    public void removeLastChar() {
        if (!numberInputText.isEmpty()) {
            numberInputText = numberInputText.substring(0, numberInputText.length() - 1);
        }
    }

    public void applyInput() {
        if (isEditingNumber && parameter instanceof NumberParameter np) {
            try {
                double val = Double.parseDouble(numberInputText);
                double min = np.getMin();
                double max = np.getMax();
                val = Math.max(min, Math.min(max, val));
                double step = np.getStep();
                val = Math.round(val / step) * step;
                np.setValue(val);
            } catch (NumberFormatException ignored) {}
        }
        isEditingNumber = false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount, int x, int y, int width, int height) {
        if (!parameter.isVisible()) return false;
        if (parameter instanceof NumberParameter np) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                double val = np.getValue();
                double step = np.getStep();
                double min = np.getMin();
                double max = np.getMax();
                double newVal = val + amount * step;
                newVal = Math.max(min, Math.min(max, newVal));
                newVal = Math.round(newVal / step) * step;
                np.setValue(newVal);
                return true;
            }
        }
        return false;
    }

    public void resetToggleAnim() {
        toggleAnimProgress = 0f;
        toggleLastUpdate = 0;
    }

    private void playSound() {
    }

    private void drawTintedTexture(GuiGraphics graphics, Identifier texture, int x, int y, int width, int height, int color) {
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0f, 0.0f, width, height, width, height, color);
    }
}
