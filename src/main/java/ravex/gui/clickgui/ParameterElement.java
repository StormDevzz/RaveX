package ravex.gui.clickgui;
import ravex.utility.render.ColorUtility;

import net.minecraft.client.gui.GuiGraphics;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import ravex.parameter.Parameter;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.animate.AnimationUtility;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.GroupParameter;
import ravex.parameter.KeybindParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.MultiSelectParameter;
import ravex.manager.ModuleManager;
import ravex.event.EventBusHolder;
import ravex.event.client.SoundEvent;

public class ParameterElement {
    private final Parameter<?> parameter;
    private boolean isDragging = false;
    private float toggleAnimProgress = 0f;
    private long toggleLastUpdate = 0;

    private float expandAnimProgress = 0f;
    private float dropdownAnimProgress = 0f;
    private float sliderKnobAnim = 0f;
    private float bindListenAnim = 0f;
    private boolean isEditingNumber = false;
    private String numberInputText = "";
    private long lastAnimTime = 0;
    private double lastSliderValue = Double.NaN;

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

        boolean smoothOption = ModuleManager.get(ravex.modules.client.ClickGui.class).smoothOption;
        float optionSmoothness = (float) ModuleManager.get(ravex.modules.client.ClickGui.class).optionSmoothness;

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

        if (parameter instanceof BooleanParameter bp) {
            float targetToggle = bp.getValue() ? 1.0f : 0.0f;
            if (smoothOption) {
                float speed = (optionSmoothness / 100f) * (delta / 16f);
                if (toggleAnimProgress < targetToggle) {
                    toggleAnimProgress = Math.min(targetToggle, toggleAnimProgress + speed);
                } else if (toggleAnimProgress > targetToggle) {
                    toggleAnimProgress = Math.max(targetToggle, toggleAnimProgress - speed);
                }
            } else {
                toggleAnimProgress = targetToggle;
            }
        }

        boolean bindListening = ClickGUI.activeKeybindElement == this && parameter instanceof KeybindParameter;
        float targetBind = bindListening ? 1.0f : 0.0f;
        if (smoothOption) {
            float speed = (optionSmoothness / 100f) * (delta / 16f);
            if (bindListenAnim < targetBind) {
                bindListenAnim = Math.min(targetBind, bindListenAnim + speed);
            } else if (bindListenAnim > targetBind) {
                bindListenAnim = Math.max(targetBind, bindListenAnim - speed);
            }
        } else {
            bindListenAnim = targetBind;
        }

        if (parameter instanceof GroupParameter gp) {
            gp.updateChevron(smoothOption ? (optionSmoothness / 100f) * (delta / 16f) * 4f : 1f);
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
        } else if (parameter instanceof MultiSelectParameter msp) {
            extraH = 18 * msp.getOptions().size();
        }
        int totalH = baseH + (int) (extraH * dropdownAnimProgress);
        return (int) (totalH * expandAnimProgress);
    }

    private static int drawNameFit(GuiGraphics graphics, String name, int x, int y, int maxW, int color) {
        if (maxW <= 4) return 0;
        name = ravex.utility.misc.LanguageUtility.paramName(name);
        if (FontRenderUtility.getStringWidth(name) > maxW) {
            FontRenderUtility.FontType ft = FontRenderUtility.getCurrentFontType();
            FontRenderUtility.FitText fit = FontRenderUtility.fitText(ft, name, maxW);
            FontRenderUtility.drawScaled(graphics, ft, fit.text, x, y, fit.scale, color, true);
            return fit.width;
        }
        FontRenderUtility.drawString(graphics, name, x, y, color, true);
        return FontRenderUtility.getStringWidth(name);
    }

    private static int drawValueFit(GuiGraphics graphics, String value, int rightEdge, int y, int minX, int color) {
        FontRenderUtility.FontType ft = FontRenderUtility.getCurrentFontType();
        int w = FontRenderUtility.getStringWidth(value);
        int vx = rightEdge - w;
        if (vx < minX) {
            FontRenderUtility.FitText fit = FontRenderUtility.fitText(ft, value, rightEdge - minX);
            vx = rightEdge - fit.width;
            FontRenderUtility.drawScaled(graphics, ft, fit.text, vx, y, fit.scale, color, true);
        } else {
            FontRenderUtility.drawString(graphics, value, vx, y, color, true);
        }
        return vx;
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        updateAnimations();
        if (height <= 0) return;

        Render2DUtility.pushScissor(graphics, x, y, width, height);

        int activeColor = ColorUtility.getActiveColor();
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        int bg = hovered ? 0x11000000 : 0x08000000;
        graphics.fill(x, y, x + width, y + height, bg);

        boolean switchless = ModuleManager.get(ravex.modules.client.ClickGui.class).switchless;

        if (parameter instanceof GroupParameter gp) {
            int baseCol = ColorUtility.interpolate(0xFF808090, 0xFFE0E0F0, toggleAnimProgress);
            int textCol = hovered ? 0xFFFFFFFF : baseCol;
            drawNameFit(graphics, gp.getName(), x + 8, y + 7, width - 28, textCol);

            float chevCx = x + width - 13f;
            float chevCy = y + height / 2f;
            float angle = gp.getChevronAngle();
            int chevCol = hovered ? ColorUtility.interpolate(0xFFA0A0B0, activeColor, toggleAnimProgress)
                                  : ColorUtility.interpolate(0xFF707080, activeColor, toggleAnimProgress);

            Render2DUtility.drawChevron(graphics, chevCx, chevCy, 11f, angle, chevCol);

        } else if (parameter instanceof BooleanParameter bp) {
            if (switchless) {
                if (toggleAnimProgress > 0.01f) {
                    int glowAlpha = (int) (toggleAnimProgress * 0x18);
                    graphics.fill(x, y, x + width, y + height, ColorUtility.withAlpha(activeColor, glowAlpha));
                }
                int baseCol = ColorUtility.interpolate(0xFFA0A0B0, activeColor, toggleAnimProgress);
                int textCol = hovered ? ColorUtility.interpolate(0xFFD0D0E0, 0xFFFFFFFF, toggleAnimProgress) : baseCol;
                drawNameFit(graphics, bp.getName(), x + 8, y + 7, width - 16, textCol);
            } else {
                int textCol = ColorUtility.interpolate(0xFF9090A0, 0xFFD0D0E0, toggleAnimProgress);
                drawNameFit(graphics, bp.getName(), x + 8, y + 7, width - 16 - 22 - 8, textCol);

                int swW = 22;
                int swH = 11;
                int swX = x + width - swW - 8;
                int swY = y + (height - swH) / 2;

                int trackColor = ColorUtility.interpolate(0xFF2A2A3A, activeColor, toggleAnimProgress);
                graphics.pose().pushMatrix();
                graphics.pose().translate((float) swX, (float) swY);
                Render2DUtility.drawRound(graphics, 0, 0, swW, swH, swH / 2, trackColor);
                graphics.pose().popMatrix();

                float knobSize = 9f;
                float knobRange = swW - knobSize - 2;
                float knobDrawX = swX + 1f + toggleAnimProgress * knobRange;
                float knobDrawY = swY + 1f;

                Identifier knobTex = Render2DUtility.getSmoothCircle();
                graphics.pose().pushMatrix();
                graphics.pose().translate(knobDrawX, knobDrawY);
                graphics.blit(RenderPipelines.GUI_TEXTURED, knobTex, 0, 0, 0f, 0f, (int) knobSize, (int) knobSize, (int) knobSize, (int) knobSize, 0xFFFFFFFF);
                graphics.pose().popMatrix();
            }

        } else if (parameter instanceof ModeParameter mp) {
            boolean expanded = dropdownAnimProgress > 0.5f;
            int nameMax = expanded ? width - 16 : width - 16 - 44;
            int nameW = drawNameFit(graphics, mp.getName(), x + 8, y + 7, nameMax, 0xFFC0C0D0);

            if (dropdownAnimProgress > 0.01f) {
                int modeY = y + 22;
                for (String m : mp.getModes()) {
                    boolean isCurrent = m.equals(mp.getValue());
                    boolean mHovered = mouseX >= x && mouseX <= x + width && mouseY >= modeY && mouseY <= modeY + 18;

                    int mBg = mHovered ? 0x18FFFFFF : 0;
                    if (mBg != 0) {
                        graphics.fill(x + 2, modeY, x + width - 2, modeY + 18, mBg);
                    }
                    if (isCurrent) {
                        graphics.fill(x + 2, modeY, x + 4, modeY + 18, activeColor);
                    }

                    int mCol = isCurrent ? activeColor : 0xFF808090;
                    if (mHovered) {
                        mCol = 0xFFFFFFFF;
                        String modeDesc = ravex.utility.misc.LanguageUtility.getModeDescription(mp.getName(), m);
                        if (modeDesc != null) {
                            ClickGUI.hoveredDescription = modeDesc;
                        }
                    }

                    drawNameFit(graphics, m, x + 14, modeY + 5, width - 14 - 8, mCol);
                    modeY += 18;
                }
            } else {
                String modeVal = mp.getValue();
                boolean mHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 22;
                if (mHovered) {
                    String modeDesc = ravex.utility.misc.LanguageUtility.getModeDescription(mp.getName(), modeVal);
                    if (modeDesc != null) {
                        ClickGUI.hoveredDescription = modeDesc;
                    }
                }

                int ltW = FontRenderUtility.getStringWidth("<");
                int gtW = FontRenderUtility.getStringWidth(">");
                int minValX = x + 8 + nameW + 6 + ltW + 3;
                int valRight = x + width - 8 - gtW - 2;
                int mw = FontRenderUtility.getStringWidth(modeVal);
                int valX = x + width - mw - 8;
                if (valX < minValX) {
                    valX = drawValueFit(graphics, modeVal, valRight, y + 7, minValX, activeColor);
                } else {
                    FontRenderUtility.drawString(graphics, modeVal, valX, y + 7, activeColor, true);
                }

                FontRenderUtility.drawString(graphics, "<", valX - ltW - 3, y + 7, ColorUtility.withAlpha(activeColor, 100), true);
                FontRenderUtility.drawString(graphics, ">", x + width - 8, y + 7, ColorUtility.withAlpha(activeColor, 100), true);
            }

        } else if (parameter instanceof MultiSelectParameter msp) {
            drawNameFit(graphics, msp.getName(), x + 8, y + 7, width - 16, 0xFFC0C0D0);

            if (dropdownAnimProgress > 0.01f) {
                int modeY = y + 22;
                for (String opt : msp.getOptions()) {
                    boolean isSel = msp.isSelected(opt);
                    boolean mHovered = mouseX >= x && mouseX <= x + width && mouseY >= modeY && mouseY <= modeY + 18;

                    int mBg = mHovered ? 0x18FFFFFF : 0;
                    if (mBg != 0) {
                        graphics.fill(x + 2, modeY, x + width - 2, modeY + 18, mBg);
                    }
                    if (isSel) {
                        graphics.fill(x + 2, modeY, x + 4, modeY + 18, activeColor);
                    }

                    int mCol = isSel ? activeColor : 0xFF808090;
                    if (mHovered) mCol = 0xFFFFFFFF;

                    drawNameFit(graphics, opt, x + 14, modeY + 5, width - 14 - 8, mCol);
                    modeY += 18;
                }
            }

        } else if (parameter instanceof NumberParameter np) {
            double min = np.getMin();
            double max = np.getMax();
            double val = np.getValue();
            double progress = (val - min) / (max - min);

            int npNameW = drawNameFit(graphics, np.getName(), x + 8, y + 5, width - 16 - 44, 0xFFC0C0D0);

            String valStr;
            int extraCursor = 0;
            if (isEditingNumber) {
                valStr = numberInputText;
                float cursorBlink = (float)Math.sin(System.currentTimeMillis() * 0.003f);
                if (cursorBlink > 0) {
                    extraCursor = 1;
                }
            } else {
                valStr = String.format("%.1f", val);
            }
            drawValueFit(graphics, valStr, x + width - 8, y + 5, x + 8 + npNameW + 6, activeColor);
            if (extraCursor > 0) {
                int cursorX = x + width - 8;
                float cursorBlink = (float)(Math.sin(System.currentTimeMillis() * 0.003f) * 0.5f + 0.5f);
                int cursorAlpha = (int)(80 + 175 * cursorBlink * cursorBlink * cursorBlink);
                int fontH = FontRenderUtility.getFontHeight();
                graphics.fill(cursorX, y + 5, cursorX + 2, y + 5 + fontH, ColorUtility.withAlpha(activeColor, cursorAlpha));
            }

            int slX = x + 8;
            int slY = y + 16;
            int slW = width - 16;
            int slH = 4;

            if (isDragging) {
                if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(MinecraftWrapper.getWrapper().getWindow().handle(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_RELEASE) {
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
                    if (newValue != np.getValue()) {
                        if (Double.isNaN(lastSliderValue) || Math.abs(newValue - lastSliderValue) > step * 0.5) {
                            EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.SLIDE, 0.35f));
                            lastSliderValue = newValue;
                        }
                    }
                    np.setValue(newValue);
                }
            } else {
                lastSliderValue = Double.NaN;
            }

            float targetKnobX = slX + (float)(slW * progress);
            if (sliderKnobAnim < slX - 1 || sliderKnobAnim > slX + slW + 1) {
                sliderKnobAnim = targetKnobX;
            }
            sliderKnobAnim += (targetKnobX - sliderKnobAnim) * Math.min(1, 0.2f);
            float animKnobX = sliderKnobAnim;
            if (animKnobX < slX) animKnobX = slX;
            if (animKnobX > slX + slW) animKnobX = slX + slW;

            graphics.pose().pushMatrix();
            graphics.pose().translate((float) slX, (float) slY);
            Render2DUtility.drawRound(graphics, 0, 0, slW, slH, slH / 2, 0xFF1A1A2A);
            float fillW = animKnobX - slX;
            if (fillW > 0) {
                Render2DUtility.drawRound(graphics, 0, 0, (int) Math.ceil(fillW), slH, slH / 2, activeColor);
            }
            graphics.pose().popMatrix();

            float knobSize = 8f;
            float knobDrawX = animKnobX - knobSize / 2f;
            float knobDrawY = slY + (slH - knobSize) / 2f;

            Identifier sliderTex = Render2DUtility.getSmoothCircle();
            graphics.pose().pushMatrix();
            graphics.pose().translate(knobDrawX, knobDrawY);
            graphics.blit(RenderPipelines.GUI_TEXTURED, sliderTex, 0, 0, 0f, 0f, (int) knobSize, (int) knobSize, (int) knobSize, (int) knobSize, 0xFFFFFFFF);
            graphics.pose().popMatrix();

        } else if (parameter instanceof ColorParameter cp) {
            drawNameFit(graphics, cp.getName(), x + 8, y + 7, width - 16 - 10 - 8, 0xFFC0C0D0);

            int chipX = x + width - 24;
            int chipY = y + 6;
            int chipSize = 10;
            int argb = cp.getValue();
            float alpha01 = ((argb >>> 24) & 0xFF) / 255f;

            int glowColor = ColorUtility.withAlpha(argb, (int) (40 + 120 * alpha01));
            Render2DUtility.drawGaussianShadow(graphics, chipX - 2, chipY - 2, chipSize + 4, chipSize + 4, 8, glowColor);

            if (alpha01 < 0.98f) {
                int cell = 2;
                for (int cy = 0; cy < chipSize; cy += cell) {
                    for (int cx = 0; cx < chipSize; cx += cell) {
                        boolean light = ((cx / cell) + (cy / cell)) % 2 == 0;
                        int chk = light ? 0xFFB0B0BC : 0xFF70707C;
                        int px0 = chipX + cx;
                        int py0 = chipY + cy;
                        int px1 = Math.min(px0 + cell, chipX + chipSize);
                        int py1 = Math.min(py0 + cell, chipY + chipSize);
                        graphics.fill(px0, py0, px1, py1, chk);
                    }
                }
            }
            graphics.fill(chipX, chipY, chipX + chipSize, chipY + chipSize, argb);
            int borderCol = hovered ? ColorUtility.withAlpha(activeColor, 160) : ColorUtility.withAlpha(activeColor, 45);
            Render2DUtility.drawBorder(graphics, chipX - 1, chipY - 1, chipSize + 2, chipSize + 2, 1, borderCol);

        } else if (parameter instanceof ravex.parameter.ActionParameter ap) {
            int apNameW = drawNameFit(graphics, ap.getName(), x + 8, y + 7, width - 16 - 70, 0xFFC0C0D0);
            String text = "Configure >";
            drawValueFit(graphics, text, x + width - 8, y + 7, x + 8 + apNameW + 6, activeColor);

        } else if (parameter instanceof ravex.parameter.StringParameter sp) {
            int spNameW = drawNameFit(graphics, sp.getName(), x + 8, y + 7, width - 16 - 44, 0xFFC0C0D0);
            boolean isFocused = ClickGUI.activeStringParameterElement != null && ClickGUI.activeStringParameterElement.getParameter() == sp;
            String text = sp.getValue();
            drawValueFit(graphics, text, x + width - 8, y + 7, x + 8 + spNameW + 6, activeColor);
            if (isFocused) {
                float cursorBlink = (float)(Math.sin(System.currentTimeMillis() * 0.003f) * 0.5f + 0.5f);
                int cursorAlpha = (int)(80 + 175 * cursorBlink * cursorBlink * cursorBlink);
                int fontH = FontRenderUtility.getFontHeight();
                graphics.fill(x + width - 8, y + 7, x + width - 6, y + 7 + fontH, ColorUtility.withAlpha(activeColor, cursorAlpha));
            }

        } else if (parameter instanceof KeybindParameter kp) {
            int kpNameW = drawNameFit(graphics, kp.getName(), x + 8, y + 7, width - 16 - 60, 0xFFC0C0D0);
            boolean isListening = ClickGUI.activeKeybindElement != null && ClickGUI.activeKeybindElement.getParameter() == kp;
            String keyText = isListening ? "..." : KeybindParameter.getKeyName(kp.getValue());
            float bAnim = bindListenAnim;
            int rightEdge = x + width - 8;
            int keyCol = ColorUtility.interpolate(activeColor, 0xFF00FF00, bAnim);
            if (bAnim > 0.01f) {
                int textW = FontRenderUtility.getStringWidth(keyText);
                int pillW = textW + 10;
                int pillH = FontRenderUtility.getFontHeight() + 6;
                int pillX = rightEdge - pillW;
                int pillY = y + (22 - pillH) / 2;
                float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.004);
                int pillA = (int) ((30 + 50 * pulse) * bAnim);
                int pillBorderA = (int) ((60 + 70 * pulse) * bAnim);
                Render2DUtility.drawPixelPerfectRound(graphics, pillX, pillY, pillW, pillH, pillH / 2, ColorUtility.withAlpha(0xFF00381A, pillA));
                Render2DUtility.drawPixelPerfectRoundBorder(graphics, pillX, pillY, pillW, pillH, pillH / 2, 1, ColorUtility.withAlpha(0xFF00FF66, pillBorderA));
            }
            if (isListening) {
                int dotW = FontRenderUtility.getStringWidth(".");
                int gap = 2;
                int totalW = dotW * 3 + gap * 2;
                int dotsX = rightEdge - totalW;
                long now = System.currentTimeMillis();
                for (int i = 0; i < 3; i++) {
                    double phase = now * 0.008 - i * 0.85;
                    float wave = (float) (0.5 + 0.5 * Math.sin(phase));
                    float eased = AnimationUtility.Easing.CUBIC_OUT.apply(wave);
                    int dotA = (int) ((0.2f + 0.8f * eased) * 255 * bAnim);
                    int dx = dotsX + i * (dotW + gap);
                    int dy = y + 7 + (eased > 0.5f ? 0 : 1);
                    FontRenderUtility.drawString(graphics, ".", dx, dy, ColorUtility.withAlpha(0xFF00FF00, dotA), true);
                }
            } else {
                drawValueFit(graphics, keyText, rightEdge, y + 7, x + 8 + kpNameW + 6, keyCol);
            }

        } else {
            String text = ravex.utility.misc.LanguageUtility.paramName(parameter.getName()) + ": " + parameter.getValue();
            drawNameFit(graphics, text, x + 8, y + 7, width - 16, 0xFFC0C0D0);
        }
        Render2DUtility.popScissor(graphics);
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

            if (parameter instanceof MultiSelectParameter msp && msp.isExpanded()) {
                int modeY = y + 22;
                for (String opt : msp.getOptions()) {
                    if (mouseX >= x && mouseX <= x + width && mouseY >= modeY && mouseY <= modeY + 18) {
                        msp.toggle(opt);
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

            if (parameter instanceof ColorParameter cp) {
                ClickGUI.activeColorParameter = cp;
                ClickGUI.activeColorPalette = new ColorPaletteModal(cp);
                playSound();
                return true;
            }

            if (button == 1) {
        if (parameter instanceof GroupParameter gp) {
                    gp.setValue(!gp.getValue());
                    playSound();
                    return true;
                }
                parameter.setExpanded(!parameter.isExpanded());
                if (!(parameter instanceof ravex.parameter.StringParameter)) {
                    playSound();
                }
                return true;
            }
            if (parameter instanceof GroupParameter gp) {
                gp.setValue(!gp.getValue());
                playSound();
                return true;
            } else if (parameter instanceof BooleanParameter bp) {
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
            } else if (parameter instanceof MultiSelectParameter msp) {
                msp.setExpanded(!msp.isExpanded());
                playSound();
                return true;
            } else if (parameter instanceof NumberParameter np) {
                isDragging = true;
                if (np.getName().equalsIgnoreCase("Gui Scale")) {
                    ClickGUI.isDraggingSlider = true;
                }
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
        if (parameter instanceof NumberParameter np && ModuleManager.get(ravex.modules.client.ClickGui.class).wheelControl) {
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
        EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.TOGGLE, 0.5f));
    }

    private void drawTintedTexture(GuiGraphics graphics, Identifier texture, int x, int y, int width, int height, int color) {
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0f, 0.0f, width, height, width, height, color);
    }
}
