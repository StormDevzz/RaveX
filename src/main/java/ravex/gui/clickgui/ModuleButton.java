package ravex.gui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.modules.Module;
import ravex.modules.render.ClickGui;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;
import ravex.parameter.Parameter;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModuleButton {
    public static final Set<Module> expandedModules = new HashSet<>();
    private static long lastGearTick = System.currentTimeMillis();
    private static final int MAX_INLINE_HEIGHT = 800;

    private int inlineScrollTarget = 0;
    private float inlineScrollAnim = 0f;

    public static void tickAllGears() {
        float speed = ClickGui.INSTANCE.gearRotationSpeed.getValue().floatValue();
        if (speed <= 0 || expandedModules.isEmpty()) return;
        long now = System.currentTimeMillis();
        float dt = Math.min(100f, now - lastGearTick) / 1000f;
        lastGearTick = now;
        for (Module m : expandedModules) {
            float cur = m.getGearAngle();
            m.setGearAngle(cur + speed * dt, now);
        }
    }

    private final Module module;
    private final List<ParameterElement> parameterElements = new ArrayList<>();
    private float hoverProgress = 0.0f;
    private float enableAnim = 0.0f;
    private boolean expanded = false;
    private float expandAnim = 0.0f;

    public ModuleButton(Module module) {
        this.module = module;
        for (Parameter<?> p : module.getParameters()) {
            parameterElements.add(new ParameterElement(p));
        }
    }

    public boolean isExpanded() { return expanded || expandAnim > 0.001f; }

    public int getExpandedHeight(int panelWidth) {
        int h = 0;
        for (ParameterElement pe : parameterElements) {
            if (pe.getParameter().isVisible() || pe.getExpandAnimProgress() > 0.001f) {
                h += pe.getHeight();
            }
        }
        return (int) ((Math.min(h, MAX_INLINE_HEIGHT) + 4) * expandAnim);
    }

    public boolean onInlineScroll(double amount) {
        int fullH = 0;
        for (ParameterElement pe : parameterElements) {
            if (pe.getParameter().isVisible() || pe.getExpandAnimProgress() > 0.001f) {
                fullH += pe.getHeight();
            }
        }
        int maxScroll = Math.max(0, fullH - MAX_INLINE_HEIGHT);
        if (maxScroll <= 0) return false;

        int prevTarget = inlineScrollTarget;
        inlineScrollTarget = Math.max(0, Math.min(maxScroll, inlineScrollTarget - (int)amount * 12));
        return inlineScrollTarget != prevTarget;
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
            enableAnim = Math.min(targetAnim, enableAnim + 0.30f);
        } else if (enableAnim > targetAnim) {
            enableAnim = Math.max(targetAnim, enableAnim - 0.35f);
        }

        int activeColor = ColorUtility.getActiveColor();
        int disabledBg = 0xFF0D0D14;

        graphics.fill(x, currentY, x + width, currentY + btnH, disabledBg);

        if (enableAnim > 0.01f) {
            int enableAlpha = (int) (enableAnim * 0x22);
            graphics.fill(x, currentY, x + width, currentY + btnH, ColorUtility.withAlpha(activeColor, enableAlpha));
        }

        if (hoverProgress > 0.01f) {
            int hoverAlpha = (int) (hoverProgress * 0x18);
            graphics.fill(x, currentY, x + width, currentY + btnH, ColorUtility.withAlpha(0xFFFFFFFF, hoverAlpha));
        }

        int baseColor = lerpColor(0xFF8F8FA0, activeColor, enableAnim);
        int textColor = hovered ? lerpColor(0xFFC0C0D0, 0xFFFFFFFF, enableAnim) : baseColor;

        if (searchQuery != null && !searchQuery.isEmpty()
            && module.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
            graphics.fill(x, currentY, x + width, currentY + btnH, ColorUtility.withAlpha(activeColor, 50));
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
            renderHighlightedName(graphics, displayName, x + ravex.modules.client.Settings.INSTANCE.moduleTextX.getValue().intValue(), textY, textColor, searchQuery);
        } else {
            FontRenderUtility.drawString(graphics, FontRenderUtility.FontType.VANILLA, displayName, x + ravex.modules.client.Settings.INSTANCE.moduleTextX.getValue().intValue(), textY, textColor, true);
        }

        boolean hasParams = !module.getParameters().isEmpty();
        if (hasParams) {
            Identifier settingsTex = ravex.utility.render.TextureLoader.getSettingsWhiteTexture();
            if (settingsTex == null) settingsTex = ravex.utility.render.TextureLoader.getSettingsTexture();
            if (settingsTex != null) {
                int iconSize = 10;
                int iconX = x + width - iconSize - 6;
                int iconY = currentY + (btnH - iconSize) / 2;
                boolean rotating = !ClickGui.INSTANCE.separateSettings.getValue() && expanded;
                float angle = module.getGearAngle();
                var pose = graphics.pose();
                pose.pushMatrix();
                pose.translate(iconX + iconSize / 2f, iconY + iconSize / 2f);
                if (rotating) {
                    pose.rotate(angle * (float)Math.PI / 180f);
                }
                pose.translate(-(iconX + iconSize / 2f), -(iconY + iconSize / 2f));
                graphics.blit(settingsTex, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f);
                pose.popMatrix();
            } else {
                boolean sepSettings = ClickGui.INSTANCE.separateSettings.getValue();
                String indicator = sepSettings ? "+" : (expanded ? "-" : "+");
                FontRenderUtility.drawString(graphics, FontRenderUtility.FontType.VANILLA, indicator, x + width - 12, textY, 0xFF7A7A8A, true);
            }
        }

        currentY += btnH;

        
        if (hasParams) {
            float targetExpand = expanded ? 1.0f : 0.0f;
            if (expandAnim < targetExpand) {
                expandAnim = Math.min(targetExpand, expandAnim + 0.08f);
            } else if (expandAnim > targetExpand) {
                expandAnim = Math.max(targetExpand, expandAnim - 0.08f);
            }

            if (expandAnim > 0.01f) {
                int paramAreaH = 0;
                int paramW = width - 6;
                for (ParameterElement pe : parameterElements) {
                    if (pe.getParameter().isVisible() || pe.getExpandAnimProgress() > 0.001f) {
                        paramAreaH += pe.getHeight();
                    }
                }
                int actualH = getExpandedHeight(width);
                int bgCol = 0xFF0A0A14;
                graphics.fill(x + 1, currentY, x + width - 1, currentY + actualH, bgCol);

                if (ClickGui.INSTANCE.smoothScroll.getValue()) {
                    float lerp = ClickGui.INSTANCE.scrollSmoothness.getValue().floatValue() / 100f;
                    inlineScrollAnim += (inlineScrollTarget - inlineScrollAnim) * lerp;
                } else {
                    inlineScrollAnim = inlineScrollTarget;
                }
                int scrollOffset = Math.round(inlineScrollAnim);
                int pY = currentY + 2 - scrollOffset;
                int visTop = currentY + 2;
                int visBot = currentY + actualH - 2;
                graphics.enableScissor(x + 1, currentY, x + width - 1, currentY + actualH);
                for (ParameterElement pe : parameterElements) {
                    if (!pe.getParameter().isVisible() && pe.getExpandAnimProgress() < 0.001f) continue;
                    int pHeight = pe.getHeight();
                    int pBot = pY + pHeight;
                    if (pBot > visTop && pY < visBot) {
                        float oldExpand = expandAnim;
                        expandAnim = 1.0f;
                        pe.render(graphics, x + 4, pY, paramW, pHeight, mouseX, mouseY);
                        expandAnim = oldExpand;
                    }
                    pY += pHeight;
                }

                int maxScroll = Math.max(0, paramAreaH - MAX_INLINE_HEIGHT);
                
                if (inlineScrollAnim > 0.5f) {
                    graphics.fillGradient(x + 1, currentY, x + width - 1, currentY + 12, bgCol, 0x000A0A14);
                }
                if (inlineScrollAnim < maxScroll - 0.5f) {
                    graphics.fillGradient(x + 1, currentY + actualH - 12, x + width - 1, currentY + actualH, 0x000A0A14, bgCol);
                }
                
                if (maxScroll > 0) {
                    int sbX = x + width - 4;
                    int sbY = currentY + 2;
                    int sbH = actualH - 4;
                    float ratio = sbH / (float) paramAreaH;
                    int thumbH = Math.max(8, (int) (sbH * ratio));
                    int thumbY = sbY + (int) ((sbH - thumbH) * (inlineScrollAnim / maxScroll));
                    graphics.fill(sbX, sbY, sbX + 2, sbY + sbH, 0x1515152A);
                    graphics.fill(sbX, thumbY, sbX + 2, thumbY + thumbH, ColorUtility.withAlpha(0xFFFFFFFF, 60));
                }

                graphics.disableScissor();
                currentY += actualH;
            }
        } else {
            expandAnim = Math.max(0.0f, expandAnim - 0.10f);
        }

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
        int totalH = btnH + (expanded && !ClickGui.INSTANCE.separateSettings.getValue() ? getExpandedHeight(width) : 0);
        boolean sepMode = ClickGui.INSTANCE.separateSettings.getValue();

        if (mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + totalH) {
            if (mouseY <= currentY + btnH) {
                if (button == 0) {
                    module.toggle();
                } else if (button == 1 && !module.getParameters().isEmpty()) {
                    if (sepMode) {
                        Minecraft.getInstance().setScreen(new ModuleSettingsScreen(Minecraft.getInstance().screen, module));
                    } else {
                        expanded = !expanded;
                        if (expanded) {
                            expandedModules.add(module);
                            inlineScrollTarget = 0;
                            //ravex.utility.sound.SoundUtility.playSettingsOpen();
                        } else {
                            expandedModules.remove(module);
                            module.setGearAngle(0f, System.currentTimeMillis());
                            //ravex.utility.sound.SoundUtility.playSettingsClose();
                        }
                    }
                } else if (button == 2) {
                    ClickGUI.bindingModuleButton = this;
                    //if (mc.player != null) {
                    //    mc.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.5f);
                    //}
                }
            } else if (!sepMode && expanded) {
                int scrollOffset = Math.round(inlineScrollAnim);
                int pY = currentY + btnH + 2 - scrollOffset;
                int paramW = width - 6;
                for (ParameterElement pe : parameterElements) {
                    if (!pe.getParameter().isVisible() && pe.getExpandAnimProgress() < 0.001f) continue;
                    int pH = pe.getHeight();
                    if (mouseX >= x + 4 && mouseX <= x + 4 + paramW && mouseY >= pY && mouseY <= pY + pH) {
                        if (pe.mouseClicked(mouseX, mouseY, button, x + 4, pY, paramW, pH)) {
                            currentYOut[0] = currentY + totalH;
                            return true;
                        }
                    }
                    pY += pH;
                }
            }
            currentYOut[0] = currentY + totalH;
            return true;
        }

        currentYOut[0] = currentY + totalH;
        return false;
    }

    public Module getModule() {
        return module;
    }

    public List<ParameterElement> getParameterElements() {
        return parameterElements;
    }
}
