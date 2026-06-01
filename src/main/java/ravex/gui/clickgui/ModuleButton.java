package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import ravex.modules.Module;
import ravex.modules.render.ClickGui;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;
import ravex.parameter.Parameter;
import ravex.utility.sound.SoundUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleButton {
    private final Module module;
    private final List<ParameterElement> parameterElements = new ArrayList<>();
    private boolean expanded = false;

    private float expansionProgress = 0.0f;
    private float hoverProgress = 0.0f;
    private float enabledAnim = 0.0f;
    private long lastUpdateTime = System.currentTimeMillis();

    public ModuleButton(Module module) {
        this.module = module;
        for (Parameter<?> p : module.getParameters()) {
            parameterElements.add(new ParameterElement(p));
        }
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, int[] currentYOut) {
        render(graphics, x, y, width, mouseX, mouseY, currentYOut, "");
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, int[] currentYOut, String searchQuery) {
        int currentY = currentYOut[0];
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + 18;

        if (hovered) {
            ClickGUI.hoveredDescription = module.getDescription();
        }

        long now = System.currentTimeMillis();
        long delta = now - lastUpdateTime;
        lastUpdateTime = now;
        if (delta > 100) delta = 16;

        float expSpeed = 0.012f;
        if (expanded) {
            expansionProgress = Math.min(1.0f, expansionProgress + delta * expSpeed);
        } else {
            expansionProgress = Math.max(0.0f, expansionProgress - delta * expSpeed);
        }

        float hoverSpeed = 0.02f;
        if (hovered) {
            hoverProgress = Math.min(1.0f, hoverProgress + delta * hoverSpeed);
        } else {
            hoverProgress = Math.max(0.0f, hoverProgress - delta * hoverSpeed);
        }

        if (module.getEnabled()) {
            enabledAnim = Math.min(1.0f, enabledAnim + delta * 0.018f);
        } else {
            enabledAnim = Math.max(0.0f, enabledAnim - delta * 0.018f);
        }

        int activeColor = ColorUtility.getActiveColor();

        Color c270 = ColorUtility.getColor(270);
        Color c0 = ColorUtility.getColor(0);
        Color c180 = ColorUtility.getColor(180);
        Color c90 = ColorUtility.getColor(90);

        if (enabledAnim > 0.01f) {
            float anim = enabledAnim;
            String gradMode = ClickGui.INSTANCE.gradientMode.getValue();
            switch (gradMode) {
                case "LeftToRight" -> {
                    Render2DEngine.drawRect(graphics, x, currentY, width, 18,
                        Render2DEngine.injectAlpha(c270, (int)(anim * 255)).getRGB());
                }
                case "UpsideDown" -> {
                    graphics.fillGradient(x, currentY, x + width, currentY + 18,
                        Render2DEngine.injectAlpha(c270, (int)(anim * 255)).getRGB(),
                        Render2DEngine.injectAlpha(c0, (int)(anim * 255)).getRGB());
                }
                case "Both" -> {
                    graphics.fillGradient(x, currentY, x + width, currentY + 18,
                        Render2DEngine.injectAlpha(c270, (int)(anim * 255)).getRGB(),
                        Render2DEngine.injectAlpha(c180, (int)(anim * 255)).getRGB());
                }
            }
        } else {
            int normalBg = ColorUtility.getModuleColor(false, false);
            int hoverBg = ColorUtility.getModuleColor(false, true);
            int bg = lerpColor(normalBg, hoverBg, hoverProgress);
            graphics.fill(x, currentY, x + width, currentY + 18, bg);
        }

        int textColor;
        if (module.getEnabled()) {
            textColor = 0xFFE0E0F0;
        } else {
            int normalText = 0xFF8F8FA0;
            int hoverText = 0xFFD0D0E0;
            textColor = lerpColor(normalText, hoverText, hoverProgress);
        }

        if (searchQuery != null && !searchQuery.isEmpty()
            && module.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
            graphics.fill(x, currentY, x + width, currentY + 18,
                ColorUtility.withAlpha(activeColor, 30));
        }

        int accentWidth = (int)(3 + hoverProgress * 3);
        int animatedAccent = ColorUtility.withAlpha(activeColor,
            module.getEnabled() ? 255 : (int)(0x3A + hoverProgress * (0x60 - 0x3A)));
        graphics.fill(x, currentY, x + accentWidth, currentY + 18, animatedAccent);

        if (module.getEnabled() && hoverProgress > 0.01f) {
            graphics.fill(x + accentWidth, currentY, x + width, currentY + 1,
                ColorUtility.withAlpha(activeColor, (int)(25 * (1 + hoverProgress))));
        } else if (module.getEnabled()) {
            graphics.fill(x + 3, currentY, x + width, currentY + 1,
                ColorUtility.withAlpha(activeColor, 25));
        }

        if (ClickGui.INSTANCE.moduleOutlines.getValue()) {
            int borderCol = ClickGui.INSTANCE.moduleOutlineColor.getValue();
            graphics.fill(x, currentY, x + width, currentY + 1, borderCol);
            graphics.fill(x, currentY + 17, x + width, currentY + 18, borderCol);
            graphics.fill(x, currentY, x + 1, currentY + 18, borderCol);
            graphics.fill(x + width - 1, currentY, x + width, currentY + 18, borderCol);
        } else {
            graphics.fill(x + accentWidth, currentY + 17, x + width, currentY + 18,
                ColorUtility.withAlpha(module.getEnabled() ? activeColor : 0xFF3A3A45,
                    (int)(40 + hoverProgress * 30)));
            if (module.getEnabled()) {
                graphics.fill(x + 3, currentY + 1, x + width, currentY + 2,
                    ColorUtility.withAlpha(activeColor, 10));
            }
        }

        String displayName = module.getName();
        if (ClickGUI.bindingModuleButton == this) {
            displayName = "[Binding...]";
        } else if (module.getKeyBind() != org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) {
            String keyName = org.lwjgl.glfw.GLFW.glfwGetKeyName(module.getKeyBind(), 0);
            if (keyName == null) {
                if (module.getKeyBind() == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) keyName = "RShift";
                else if (module.getKeyBind() == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) keyName = "LShift";
                else if (module.getKeyBind() == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) keyName = "Space";
                else keyName = "Key " + module.getKeyBind();
            } else {
                keyName = keyName.toUpperCase();
            }
            displayName += " [" + keyName + "]";
        }

        if (searchQuery != null && !searchQuery.isEmpty() && !module.getName().isEmpty()) {
            renderHighlightedName(graphics, displayName, x + 9, currentY + 5, textColor, searchQuery);
        } else {
            FontRenderUtility.drawString(graphics, FontRenderUtility.FontType.SF_BOLD, displayName, x + 9, currentY + 5, textColor, true);
        }

        int visibleCount = 0;
        int expandedHeight = 0;
        for (ParameterElement pe : parameterElements) {
            if (pe.getParameter().isVisible()) {
                visibleCount++;
                expandedHeight += pe.getHeight();
            }
        }

        if (visibleCount > 0) {
            FontRenderUtility.drawString(graphics, expanded ? "\u2212" : "+", x + width - 12, currentY + 5, 0xFF606080, true);
        }

        currentY += 18;

        if (expansionProgress > 0.001f && visibleCount > 0) {
            int animatedHeight = (int) (expandedHeight * expansionProgress);
            int totalDrawHeight = animatedHeight + 4;

            int boxBg = 0xFF14141E;
            graphics.fill(x + 4, currentY, x + width - 4, currentY + totalDrawHeight, boxBg);

            int boxBorder = ColorUtility.withAlpha(activeColor, 60);
            graphics.fill(x + 4, currentY, x + 6, currentY + totalDrawHeight, boxBorder);
            graphics.fill(x + 4, currentY + totalDrawHeight - 1, x + width - 4, currentY + totalDrawHeight,
                ColorUtility.withAlpha(activeColor, 25));

            int py = currentY + 2;
            for (ParameterElement pe : parameterElements) {
                if (pe.getParameter().isVisible()) {
                    int pHeight = pe.getHeight();
                    pe.render(graphics, x + 6, py, width - 10, pHeight, mouseX, mouseY);
                    py += pHeight;
                }
            }

            currentY += totalDrawHeight;
        }

        currentYOut[0] = currentY;
    }

    private void renderHighlightedName(GuiGraphics graphics, String text, int x, int y, int baseColor, String query) {
        String lower = text.toLowerCase();
        String qLower = query.toLowerCase();
        int queryLen = qLower.length();

        int currentX = x;
        int i = 0;
        while (i < text.length()) {
            int matchIdx = lower.indexOf(qLower, i);
            if (matchIdx == -1) {
                FontRenderUtility.drawString(graphics, text.substring(i), currentX, y, baseColor, true);
                break;
            }

            if (matchIdx > i) {
                String before = text.substring(i, matchIdx);
                FontRenderUtility.drawString(graphics, before, currentX, y, baseColor, true);
                currentX += FontRenderUtility.getStringWidth(before);
            }

            String matched = text.substring(matchIdx, Math.min(matchIdx + queryLen, text.length()));
            graphics.fill(currentX - 1, y - 1, currentX + FontRenderUtility.getStringWidth(matched) + 1, y + 10, 0x44FFAA00);
            FontRenderUtility.drawString(graphics, matched, currentX, y, 0xFFFFFF80, true);
            currentX += FontRenderUtility.getStringWidth(matched);

            i = matchIdx + queryLen;
        }
    }

    private static int lerpColor(int bg, int fg, float alpha) {
        int aBg = (bg >> 24) & 0xFF;
        int rBg = (bg >> 16) & 0xFF;
        int gBg = (bg >> 8) & 0xFF;
        int bBg = bg & 0xFF;

        int aFg = (fg >> 24) & 0xFF;
        int rFg = (fg >> 16) & 0xFF;
        int gFg = (fg >> 8) & 0xFF;
        int bFg = fg & 0xFF;

        int r = (int)(rBg * (1 - alpha) + rFg * alpha);
        int g = (int)(gBg * (1 - alpha) + gFg * alpha);
        int b = (int)(bBg * (1 - alpha) + bFg * alpha);
        int a = (int)(aBg * (1 - alpha) + aFg * alpha);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int[] currentYOut, net.minecraft.client.Minecraft mc) {
        int currentY = currentYOut[0];
        int height = 18;

        int visibleCount = 0;
        int expandedHeight = 0;
        for (ParameterElement pe : parameterElements) {
            if (pe.getParameter().isVisible()) {
                visibleCount++;
                expandedHeight += pe.getHeight();
            }
        }

        if (mouseX >= x && mouseX <= x + 120 && mouseY >= currentY && mouseY <= currentY + height) {
            if (button == 0) {
                module.toggle();
            } else if (button == 1 && visibleCount > 0) {
                expanded = !expanded;
                if (expanded) {
                    SoundUtility.playSettingsOpen();
                } else {
                    SoundUtility.playSettingsClose();
                }
            } else if (button == 2) {
                ClickGUI.bindingModuleButton = this;
                if (mc.player != null) {
                    mc.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.5f);
                }
            }
            currentYOut[0] += height;
            if (expanded && visibleCount > 0) {
                currentYOut[0] += (int) ((expandedHeight + 4) * expansionProgress);
            }
            return true;
        }

        currentY += height;

        if (expanded && expansionProgress > 0.9f && visibleCount > 0) {
            currentY += 2;
            for (ParameterElement pe : parameterElements) {
                if (pe.getParameter().isVisible()) {
                    int pHeight = pe.getHeight();
                    if (pe.mouseClicked(mouseX, mouseY, button, x + 6, currentY, 120 - 10, pHeight)) {
                        currentYOut[0] = currentY + pHeight;
                        return true;
                    }
                    currentY += pHeight;
                }
            }
            currentY += 2;
        }

        currentYOut[0] = currentY;
        return false;
    }

    public Module getModule() {
        return module;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public float getExpansionProgress() {
        return expansionProgress;
    }

    public List<ParameterElement> getParameterElements() {
        return parameterElements;
    }
}
