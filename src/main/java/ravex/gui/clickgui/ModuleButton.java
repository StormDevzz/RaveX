package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import ravex.modules.Module;
import ravex.utility.render.FontRenderUtility;
import ravex.parameter.Parameter;
import ravex.utility.sound.SoundUtility;
import java.util.ArrayList;
import java.util.List;

public class ModuleButton {
    private final Module module;
    private final List<ParameterElement> parameterElements = new ArrayList<>();
    private boolean expanded = false;

    private float expansionProgress = 0.0f;
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

        int bg = ColorUtility.getModuleColor(module.getEnabled(), hovered);
        int textColor = ColorUtility.getTextColor(module.getEnabled(), hovered);

        if (searchQuery != null && !searchQuery.isEmpty()
            && module.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
            bg = blendColors(bg, ColorUtility.getActiveColor(), 0.15f);
        }

        graphics.fill(x, currentY, x + width, currentY + 18, bg);

        int activeColor = ColorUtility.getActiveColor();
        int accentColor = module.getEnabled() ? activeColor : 0xFF3A3A45;
        graphics.fill(x, currentY, x + 3, currentY + 18, accentColor);
        if (module.getEnabled()) {
            graphics.fill(x + 3, currentY, x + width, currentY + 1, ColorUtility.withAlpha(activeColor, 25));
        }

        if (ravex.modules.render.ClickGui.INSTANCE.moduleOutlines.getValue()) {
            int borderCol = ravex.modules.render.ClickGui.INSTANCE.moduleOutlineColor.getValue();
            graphics.fill(x, currentY, x + width, currentY + 1, borderCol);
            graphics.fill(x, currentY + 17, x + width, currentY + 18, borderCol);
            graphics.fill(x, currentY, x + 1, currentY + 18, borderCol);
            graphics.fill(x + width - 1, currentY, x + width, currentY + 18, borderCol);
        } else {
            graphics.fill(x + 3, currentY + 17, x + width, currentY + 18, ColorUtility.withAlpha(accentColor, 40));
            if (module.getEnabled()) {
                graphics.fill(x + 3, currentY + 1, x + width, currentY + 2, ColorUtility.withAlpha(activeColor, 10));
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
            FontRenderUtility.drawString(graphics, displayName, x + 9, currentY + 5, textColor, true);
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
            FontRenderUtility.drawString(graphics, expanded ? "−" : "+", x + width - 12, currentY + 5, 0xFF606080, true);
        }

        currentY += 18;

        long now = System.currentTimeMillis();
        long delta = now - lastUpdateTime;
        lastUpdateTime = now;
        if (delta > 100) delta = 16;

        float animSpeed = 0.008f;
        if (expanded) {
            expansionProgress = Math.min(1.0f, expansionProgress + delta * animSpeed);
        } else {
            expansionProgress = Math.max(0.0f, expansionProgress - delta * animSpeed);
        }

        if (expansionProgress > 0.001f && visibleCount > 0) {
            int animatedHeight = (int) (expandedHeight * expansionProgress);
            int totalDrawHeight = animatedHeight + 4;

            int boxBg = 0xFF14141E;
            int boxBorder = ColorUtility.getActiveColor();
            graphics.fill(x + 4, currentY, x + width - 4, currentY + totalDrawHeight, boxBg);
            graphics.fill(x + 4, currentY, x + 6, currentY + totalDrawHeight, boxBorder);
            graphics.fill(x + 4, currentY + totalDrawHeight - 1, x + width - 4, currentY + totalDrawHeight, ColorUtility.withAlpha(boxBorder, 40));

            graphics.enableScissor(x + 4, currentY, x + width - 4, currentY + totalDrawHeight);

            int py = currentY + 2;
            for (ParameterElement pe : parameterElements) {
                if (pe.getParameter().isVisible()) {
                    int pHeight = pe.getHeight();
                    pe.render(graphics, x + 6, py, width - 10, pHeight, mouseX, mouseY);
                    py += pHeight;
                }
            }

            graphics.disableScissor();
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
            int highlightColor = 0xFFFFFF80;
            graphics.fill(currentX - 1, y - 1, currentX + FontRenderUtility.getStringWidth(matched) + 1, y + 10, 0x44FFAA00);
            FontRenderUtility.drawString(graphics, matched, currentX, y, highlightColor, true);
            currentX += FontRenderUtility.getStringWidth(matched);

            i = matchIdx + queryLen;
        }
    }

    private static int blendColors(int bg, int fg, float alpha) {
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
}
