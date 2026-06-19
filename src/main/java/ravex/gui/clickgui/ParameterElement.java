package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.parameter.Parameter;
import ravex.utility.render.FontRenderUtility;
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

        // 1. Parameter visibility animation (for child options)
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

        // 2. Dropdown expansion animation (for ModeParameter)
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

        int leftAccent = ColorUtility.withAlpha(activeColor, 30);
        graphics.fill(x, y, x + 2, y + height, leftAccent);

        if (parameter instanceof BooleanParameter bp) {
            int textCol = bp.getValue() ? 0xFFD0D0E0 : 0xFF9090A0;
            FontRenderUtility.drawString(graphics, bp.getName(), x + 8, y + 7, textCol, true);

            int swW = 28;
            int swH = 12;
            int swX = x + width - swW - 7;
            int swY = y + (height - swH) / 2;

            if (bp.getValue()) {
                toggleAnimProgress = Math.min(1.0f, toggleAnimProgress + delta * 0.015f);
            } else {
                toggleAnimProgress = Math.max(0.0f, toggleAnimProgress - delta * 0.015f);
            }

            int knobSize = 10;
            int knobRange = swW - knobSize - 3;
            int knobX = swX + 2 + (int)(toggleAnimProgress * knobRange);
            int knobY2 = swY + 1;

            Identifier switcherTex = ravex.utility.render.TextureLoader.getSwitcherTexture();
            if (switcherTex != null) {
                graphics.blit(switcherTex, swX, swY, swX + swW, swY + swH, 0.0f, 1.0f, 0.0f, 1.0f);
            } else {
                int trackR = ((activeColor >> 16) & 0xFF);
                int trackG = ((activeColor >> 8) & 0xFF);
                int trackB = (activeColor & 0xFF);
                int trackOn = (0xCC << 24) | (trackR << 16) | (trackG << 8) | trackB;
                int trackOff = 0xFF2A2A38;
                int trackColor = lerpColor(trackOff, trackOn, toggleAnimProgress);
                int trackA = (trackColor >> 24) & 0xFF;

                float halfH = swH / 2f;
                for (int i = 0; i < swH; i++) {
                    float t = (i + 0.5f) / swH;
                    float edge = Math.min(t, 1f - t) * 2f;
                    float insetF = halfH * (1f - edge * edge);
                    int inset = (int) insetF;
                    float frac = insetF - inset;

                    int lx = swX + inset;
                    int rx = swX + swW - inset;
                    graphics.fill(lx + 1, swY + i, rx - 1, swY + i + 1, trackColor);

                    int a = Math.round(frac * trackA);
                    if (a > 0) {
                        int edgeCol = (a << 24) | (trackColor & 0x00FFFFFF);
                        graphics.fill(lx, swY + i, lx + 1, swY + i + 1, edgeCol);
                        graphics.fill(rx - 1, swY + i, rx, swY + i + 1, edgeCol);
                    }
                }
            }

            Identifier circleTex = ravex.utility.render.TextureLoader.getCircleWhiteTexture();
            if (circleTex != null) {
                int cSize = knobSize + 2;
                int cx = knobX - 1;
                int cy = knobY2 - 1;
                graphics.blit(circleTex, cx, cy, cx + cSize, cy + cSize, 0.0f, 1.0f, 0.0f, 1.0f);
            } else {
                for (int i = 0; i < knobSize; i++) {
                    float t = (i + 0.5f) / knobSize;
                    float edge = Math.min(t, 1f - t) * 2f;
                    int inset = (int)(knobSize/2 * (1f - edge * edge));
                    if (inset < 0) inset = 0;
                    int kcol = (int)(0xFF * (0.85f + 0.15f * toggleAnimProgress));
                    int knobCol = (0xFF << 24) | (kcol << 16) | (kcol << 8) | kcol;
                    if (i == 0 || i == knobSize - 1) {
                        int borderInset = knobSize/2 - 1;
                        graphics.fill(knobX + borderInset, knobY2 + i, knobX + knobSize - borderInset, knobY2 + i + 1, 0xFFE0E0E0);
                    } else {
                        graphics.fill(knobX + inset, knobY2 + i, knobX + knobSize - inset, knobY2 + i + 1, knobCol);
                    }
                }
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

            String valStr = String.format("%.1f", val);
            int valW = FontRenderUtility.getStringWidth(valStr);
            FontRenderUtility.drawString(graphics, valStr, x + width - valW - 8, y + 5, activeColor, true);

            int slX = x + 8;
            int slY = y + 16;
            int slW = width - 16;
            int slH = 12;

            if (isDragging) {
                if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(net.minecraft.client.Minecraft.getInstance().getWindow().handle(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_RELEASE) {
                    isDragging = false;
                } else {
                    double relative = (double) (mouseX - slX) / slW;
                    relative = Math.max(0.0, Math.min(1.0, relative));
                    double newValue = min + relative * (max - min);
                    double step = np.getStep();
                    newValue = Math.round(newValue / step) * step;
                    np.setValue(newValue);
                }
            }

            boolean slHovered = mouseX >= slX && mouseX <= slX + slW && mouseY >= slY - 2 && mouseY <= slY + slH + 2;

            Identifier switcherTex = ravex.utility.render.TextureLoader.getSwitcherTexture();
            if (switcherTex != null) {
                graphics.blit(switcherTex, slX, slY, slX + slW, slY + slH, 0.0f, 1.0f, 0.0f, 1.0f);
            } else {
                for (int i = 0; i < slH; i++) {
                    float gap = Math.abs(i - slH/2f) / (slH/2f);
                    int inset = (int)(slH/2 * gap * gap);
                    graphics.fill(slX + inset, slY + i, slX + slW - inset, slY + i + 1, 0xFF2A2A38);
                }
            }

            int fillW = slW - 12;
            int knobX = slX + 6 + (int)(fillW * progress);
            int knobY2 = slY + slH / 2;
            Identifier circleTex = ravex.utility.render.TextureLoader.getCircleWhiteTexture();
            if (circleTex != null) {
                int cSize = 12;
                graphics.blit(circleTex, knobX - cSize / 2, knobY2 - cSize / 2, knobX + cSize / 2 + 1, knobY2 + cSize / 2 + 1, 0.0f, 1.0f, 0.0f, 1.0f);
            } else {
                int knobR = 4;
                int knobColor = slHovered || isDragging ? 0xFFFFFFFF : 0xFFC8C8D0;
                for (int dy = -knobR; dy <= knobR; dy++) {
                    float gap = Math.abs(dy) / (float)knobR;
                    int dx = (int) Math.sqrt(knobR * knobR - dy * dy);
                    int innerInset = (int)(knobR/2 * gap * gap * 0.5f);
                    graphics.fill(knobX - dx + innerInset, knobY2 + dy, knobX + dx - innerInset + 1, knobY2 + dy + 1, knobColor);
                }
            }

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

    public void resetToggleAnim() {
        toggleAnimProgress = 0f;
        toggleLastUpdate = 0;
    }

    private void playSound() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.25f, 1.4f);
        }
    }
}
