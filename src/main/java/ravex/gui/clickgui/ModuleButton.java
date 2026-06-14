package ravex.gui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ravex.modules.Module;
import ravex.modules.render.ClickGui;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;
import ravex.parameter.Parameter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleButton {
    private final Module module;
    private final List<ParameterElement> parameterElements = new ArrayList<>();
    private float hoverProgress = 0.0f;
    private float enableAnim = 0.0f;

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
        int btnH = ravex.modules.render.ClickGui.INSTANCE.buttonHeight.getValue().intValue();
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + btnH;

        if (hovered) {
            ClickGUI.hoveredDescription = module.getDescription();
            hoverProgress = Math.min(1.0f, hoverProgress + 0.07f);
        } else {
            hoverProgress = Math.max(0.0f, hoverProgress - 0.07f);
        }

        float targetAnim = module.getEnabled() ? 1.0f : 0.0f;
        if (enableAnim < targetAnim) {
            enableAnim = Math.min(targetAnim, enableAnim + 0.06f);
        } else if (enableAnim > targetAnim) {
            enableAnim = Math.max(targetAnim, enableAnim - 0.10f);
        }

        int activeColor = ColorUtility.getActiveColor();
        int disabledBg = 0xFF0D0D14;

        if (enableAnim > 0.001f) {
            int bg = lerpColor(disabledBg, activeColor, enableAnim);
            graphics.fill(x, currentY, x + width, currentY + btnH, bg);
            if (hoverProgress > 0.01f) {
                int alpha = (int)(hoverProgress * 0x15);
                graphics.fill(x, currentY, x + width, currentY + btnH, ColorUtility.withAlpha(0xFFFFFFFF, alpha));
            }
        } else if (hoverProgress > 0.01f) {
            int alpha = (int)(hoverProgress * 0x22);
            graphics.fill(x, currentY, x + width, currentY + btnH, ColorUtility.withAlpha(0xFFFFFFFF, alpha));
        }

        float textLerp = enableAnim;
        if (textLerp < 0.001f) textLerp = hovered ? 0.5f : 0.0f;
        int textColor = lerpColor(0xFF8F8FA0, 0xFFFFFFFF, textLerp);

        if (searchQuery != null && !searchQuery.isEmpty()
            && module.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
            graphics.fill(x, currentY, x + width, currentY + btnH, ColorUtility.withAlpha(activeColor, 40));
            graphics.fill(x, currentY, x + 2, currentY + btnH, activeColor);
        }

        if (ClickGui.INSTANCE.moduleOutlines.getValue()) {
            int borderCol = ClickGui.INSTANCE.moduleOutlineColor.getValue();
            Render2DEngine.drawBorder(graphics, x, currentY, width, btnH, 1, borderCol);
        } else {
            graphics.fill(x, currentY + btnH - 1, x + width, currentY + btnH, 0x1AFFFFFF);
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

        int textY = currentY + (btnH - FontRenderUtility.getFontHeight()) / 2 + 1;

        if (searchQuery != null && !searchQuery.isEmpty() && !module.getName().isEmpty()) {
            renderHighlightedName(graphics, displayName, x + 9, textY, textColor, searchQuery);
        } else {
            FontRenderUtility.drawString(graphics, FontRenderUtility.FontType.VANILLA, displayName, x + 9, textY, textColor, true);
        }

        if (!module.getParameters().isEmpty()) {
            FontRenderUtility.drawString(graphics, FontRenderUtility.FontType.VANILLA, "+", x + width - 12, textY, 0xFF7A7A8A, true);
        }

        currentY += btnH;
        currentYOut[0] = currentY;
    }

    private void renderHighlightedName(GuiGraphics graphics, String text, int x, int y, int baseColor, String query) {
        String lower = text.toLowerCase();
        String qLower = query.toLowerCase();
        int queryLen = qLower.length();
        int fontH = FontRenderUtility.getFontHeight();

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
            graphics.fill(currentX - 1, y - 1, currentX + FontRenderUtility.getStringWidth(matched) + 1, y + fontH + 1, 0x44FFAA00);
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

    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int width, int[] currentYOut, net.minecraft.client.Minecraft mc) {
        int currentY = currentYOut[0];
        int btnH = ravex.modules.render.ClickGui.INSTANCE.buttonHeight.getValue().intValue();
        currentYOut[0] = currentY + btnH;

        if (mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + btnH) {
            if (button == 0) {
                module.toggle();
            } else if (button == 1 && !module.getParameters().isEmpty()) {
                Minecraft.getInstance().setScreen(new ModuleSettingsScreen(Minecraft.getInstance().screen, module));
            } else if (button == 2) {
                ClickGUI.bindingModuleButton = this;
                if (mc.player != null) {
                    mc.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.5f);
                }
            }
            return true;
        }

        return false;
    }

    public Module getModule() {
        return module;
    }

    public List<ParameterElement> getParameterElements() {
        return parameterElements;
    }
}
