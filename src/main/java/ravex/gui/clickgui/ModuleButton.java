package ravex.gui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.modules.Module;
import ravex.modules.client.ClickGui;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;
import ravex.parameter.Parameter;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

public class ModuleButton {
    public static final Set<Module> expandedModules = new HashSet<>();
    private static long lastGearTick = System.currentTimeMillis();
    private static final int MAX_INLINE_HEIGHT = 800;

    private float inlineScrollTarget = 0f;
    private float inlineScrollAnim = 0f;
    private float searchReveal = 1.0f;
    private boolean matchesSearch = true;

    public static void tickAllGears() {
<<<<<<< HEAD
        float speed = ModuleManager.get(ClickGui.class).gearRotationSpeed.getValue().floatValue();
=======
        float speed = ClickGui.INSTANCE.gearRotationSpeed.getValue().floatValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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

    public float getSearchReveal() { return searchReveal; }

    public void updateSearchReveal(boolean matches, boolean hasQuery) {
        matchesSearch = matches;
        float target = (!hasQuery || matches) ? 1.0f : 0.0f;
        if (searchReveal < target)
            searchReveal = Math.min(target, searchReveal + 0.35f);
        else if (searchReveal > target)
            searchReveal = Math.max(target, searchReveal - 0.30f);
    }

    public int getExpandedHeight(int panelWidth) {
        int h = 0;
        for (ParameterElement pe : parameterElements) {
            if (pe.getParameter().isVisible() || pe.getExpandAnimProgress() > 0.001f) {
                h += pe.getHeight();
            }
        }
        return (int) ((Math.min(h, MAX_INLINE_HEIGHT) + 4) * expandAnim);
    }

    public boolean onInlineScroll(double mouseX, double mouseY, double amount, int x, int y, int width) {
        int paramW = width - 6;
        int scrollOffset = Math.round(inlineScrollAnim);
        int pY = y + 2 - scrollOffset;

        int fullH = 0;
        for (ParameterElement pe : parameterElements) {
            if (pe.getParameter().isVisible() || pe.getExpandAnimProgress() > 0.001f) {
                fullH += pe.getHeight();
            }
        }
        int maxScroll = Math.max(0, fullH - MAX_INLINE_HEIGHT);

        float prev = inlineScrollTarget;
        inlineScrollTarget = Math.max(0, Math.min(maxScroll, inlineScrollTarget - (float)(amount * 36)));
        boolean listScrolled = Math.abs(inlineScrollTarget - prev) > 0.01f;

        boolean paramConsumed = false;
        pY = y + 2 - scrollOffset;
        for (ParameterElement pe : parameterElements) {
            if (!pe.getParameter().isVisible() && pe.getExpandAnimProgress() < 0.001f) continue;
            int pHeight = pe.getHeight();
            if (pe.mouseScrolled(mouseX, mouseY, amount, x + 1, pY, paramW, pHeight)) {
                paramConsumed = true;
            }
            pY += pHeight;
        }

        return listScrolled || paramConsumed;
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, int[] currentYOut, int viewTop, int viewBot) {
        render(graphics, x, y, width, mouseX, mouseY, currentYOut, "", viewTop, viewBot);
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, int[] currentYOut, String searchQuery, int viewTop, int viewBot) {
        int currentY = currentYOut[0];
        if (searchReveal < 0.001f) return;
<<<<<<< HEAD
        int btnH = ModuleManager.get(ClickGui.class).buttonHeight.getValue().intValue();
=======
        int btnH = ravex.modules.client.ClickGui.INSTANCE.buttonHeight.getValue().intValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + btnH
            && currentY + btnH > viewTop && currentY < viewBot;

        if (hovered) {
            ClickGUI.hoveredDescription = module.getDescription();
            hoverProgress = Math.min(1.0f, hoverProgress + 0.10f);
        } else {
            hoverProgress = Math.max(0.0f, hoverProgress - 0.10f);
        }

        float targetAnim = module.getEnabled() ? 1.0f : 0.0f;
        if (enableAnim < targetAnim) {
            enableAnim = Math.min(targetAnim, enableAnim + 0.30f);
        } else if (enableAnim > targetAnim) {
            enableAnim = Math.max(targetAnim, enableAnim - 0.35f);
        }

        int activeColor = ColorUtility.getActiveColor();
<<<<<<< HEAD
        int btnAlpha = ModuleManager.get(ClickGui.class).buttonOpacity.getValue().intValue();
        int disabledBg = ColorUtility.withAlpha(0x252530, btnAlpha);
        int btnRadius = Math.min(ModuleManager.get(ClickGui.class).cornerRadius.getValue().intValue(), btnH / 2);
=======
        int btnAlpha = ravex.modules.client.ClickGui.INSTANCE.buttonOpacity.getValue().intValue();
        int disabledBg = ColorUtility.withAlpha(0x252530, btnAlpha);
        int btnRadius = Math.min(ClickGui.INSTANCE.cornerRadius.getValue().intValue(), btnH / 2);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

        int mergedBg = disabledBg;
        if (enableAnim > 0.01f) {
            int enableAlpha = (int) (enableAnim * Math.min(255, btnAlpha * 3));
            mergedBg = blendSrcOver(mergedBg, ColorUtility.withAlpha(activeColor, enableAlpha));
        }
        if (hoverProgress > 0.01f && enableAnim < 0.01f) {
            int hoverAlpha = (int) (hoverProgress * Math.min(30, btnAlpha / 2));
            mergedBg = blendSrcOver(mergedBg, ColorUtility.withAlpha(0xFFFFFFFF, hoverAlpha));
        }

        Render2DEngine.drawPixelPerfectRound(graphics, x + 2, currentY, width - 4, btnH, btnRadius, mergedBg);

        if (searchQuery != null && !searchQuery.isEmpty()
            && module.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
            int searchBg = blendSrcOver(mergedBg, ColorUtility.withAlpha(activeColor, 30));
            Render2DEngine.drawPixelPerfectRound(graphics, x + 2, currentY, width - 4, btnH, btnRadius, searchBg);
        }

        int baseColor = lerpColor(0xFFB0B0C0, activeColor, enableAnim);
        int textColor = hovered ? 0xFFFFFFFF : baseColor;

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
<<<<<<< HEAD
            renderHighlightedName(graphics, displayName, x + ModuleManager.get(ravex.modules.client.Settings.class).moduleTextX.getValue().intValue(), textY, textColor, searchQuery);
        } else {
            FontRenderUtility.drawString(graphics, displayName, x + ModuleManager.get(ravex.modules.client.Settings.class).moduleTextX.getValue().intValue(), textY, textColor, true);
        }

        boolean hasParams = !module.getParameters().isEmpty();
        if (hasParams && ModuleManager.get(ClickGui.class).showGear.getValue()) {
=======
            renderHighlightedName(graphics, displayName, x + ravex.modules.client.Settings.INSTANCE.moduleTextX.getValue().intValue(), textY, textColor, searchQuery);
        } else {
            FontRenderUtility.drawString(graphics, displayName, x + ravex.modules.client.Settings.INSTANCE.moduleTextX.getValue().intValue(), textY, textColor, true);
        }

        boolean hasParams = !module.getParameters().isEmpty();
        if (hasParams && ClickGui.INSTANCE.showGear.getValue()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            Identifier settingsTex = ravex.utility.render.TextureLoader.getSettingsWhiteTexture();
            if (settingsTex == null) settingsTex = ravex.utility.render.TextureLoader.getSettingsTexture();
            if (settingsTex != null) {
                int iconSize = 10;
                int iconX = x + width - iconSize - 8;
                int iconY = currentY + (btnH - iconSize) / 2;
                boolean rotating = expanded;
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
                String indicator = expanded ? "-" : "+";
                FontRenderUtility.drawString(graphics, indicator, x + width - 12, textY, 0xFF7A7A8A, true);
            }
        }

        Identifier modIcon = ravex.utility.render.TextureLoader.getModuleIcon(module.getName());
        if (modIcon != null) {
            int iconSize = 14;
            int iconX = x + width - iconSize - 4;
            int iconY = currentY + btnH - iconSize - 2;
            graphics.blit(modIcon, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f);
        }

        currentY += btnH + 2;

        if (hasParams) {
            float targetExpand = expanded ? 1.0f : 0.0f;
            if (expandAnim < targetExpand) {
                expandAnim = Math.min(targetExpand, expandAnim + 0.10f);
            } else if (expandAnim > targetExpand) {
                expandAnim = Math.max(targetExpand, expandAnim - 0.10f);
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
                int bgCol = ColorUtility.withAlpha(0x0A0A14, Math.max(btnAlpha / 2, 24));
                Render2DEngine.drawPixelPerfectRound(graphics, x + 3, currentY, width - 6, actualH, Math.max(4, btnRadius - 2), bgCol);

                inlineScrollAnim = inlineScrollTarget;
                int scrollOffset = Math.round(inlineScrollAnim);
                int pY = currentY + 2 - scrollOffset;
                int visTop = currentY + 2;
                int visBot = currentY + actualH - 2;
                graphics.enableScissor(x + 3, currentY, x + width - 3, currentY + actualH);
                for (ParameterElement pe : parameterElements) {
                    if (!pe.getParameter().isVisible() && pe.getExpandAnimProgress() < 0.001f) continue;
                    int pHeight = pe.getHeight();
                    int pBot = pY + pHeight;
                    if (pBot > visTop && pY < visBot) {
                        float oldExpand = expandAnim;
                        expandAnim = 1.0f;
                        pe.render(graphics, x + 5, pY, paramW, pHeight, mouseX, mouseY);
                        expandAnim = oldExpand;
                    }
                    pY += pHeight;
                }

                int maxScroll = Math.max(0, paramAreaH - MAX_INLINE_HEIGHT);
                if (maxScroll > 0) {
                    int sbX = x + width - 5;
                    int sbY = currentY + 2;
                    int sbH = actualH - 4;
                    float ratio = sbH / (float) paramAreaH;
                    int thumbH = Math.max(8, (int) (sbH * ratio));
                    int thumbY = sbY + (int) ((sbH - thumbH) * (inlineScrollAnim / maxScroll));
                    graphics.fill(sbX, sbY, sbX + 2, sbY + sbH, 0x1515152A);
                    graphics.fill(sbX, thumbY, sbX + 2, thumbY + thumbH, ColorUtility.withAlpha(0xFFFFFFFF, 40));
                }

                graphics.disableScissor();
                currentY += actualH;
            }
        } else {
            expandAnim = Math.max(0.0f, expandAnim - 0.10f);
        }

        boolean searching = searchQuery != null && !searchQuery.isEmpty();
        float revealH = (!searching || matchesSearch) ? 1.0f : searchReveal;
        if (revealH < 0.99f) {
            int fadeAlpha = (int)((1.0f - revealH) * 200);
<<<<<<< HEAD
<<<<<<< HEAD
            Render2DEngine.drawRound(graphics, x + 2, currentYOut[0], width - 4, btnH, Math.min(ModuleManager.get(ClickGui.class).cornerRadius.getValue().intValue(), btnH / 2), (fadeAlpha << 24) | 0x050510);
=======
            Render2DEngine.drawRound(graphics, x + 2, currentYOut[0], width - 4, btnH, Math.min(ClickGui.INSTANCE.cornerRadius.getValue().intValue(), btnH / 2), (fadeAlpha << 24) | 0x050510);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
=======
            Render2DEngine.drawPixelPerfectRound(graphics, x + 2, currentYOut[0], width - 4, btnH, Math.min(ModuleManager.get(ClickGui.class).cornerRadius.getValue().intValue(), btnH / 2), (fadeAlpha << 24) | 0x050510);
>>>>>>> 0ab37177398daa0e9880b2ec0d3ee76a2dbed416
        }
        currentYOut[0] = currentYOut[0] + (int)((currentY - currentYOut[0]) * revealH);
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

    private static int blendSrcOver(int dst, int src) {
        int sa = (src >> 24) & 0xFF;
        if (sa == 0) return dst;
        if (sa >= 254) return src;
        int da = (dst >> 24) & 0xFF;
        if (da == 0) return src;
        int sr = (src >> 16) & 0xFF;
        int sg = (src >> 8) & 0xFF;
        int sb = src & 0xFF;
        int dr = (dst >> 16) & 0xFF;
        int dg = (dst >> 8) & 0xFF;
        int db = dst & 0xFF;
        float a = sa / 255f;
        float invA = 1f - a;
        int r = (int) (sr * a + dr * invA);
        int g = (int) (sg * a + dg * invA);
        int b = (int) (sb * a + db * invA);
        int na = (int) (sa + da * invA);
        return (Math.min(255, na) << 24) | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
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
<<<<<<< HEAD
        int btnH = ModuleManager.get(ClickGui.class).buttonHeight.getValue().intValue();
<<<<<<< HEAD
=======
        int btnH = ravex.modules.client.ClickGui.INSTANCE.buttonHeight.getValue().intValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        int totalH = btnH + (expanded ? getExpandedHeight(width) : 0);
=======
        int gap = 2;
        int paramH = expanded ? getExpandedHeight(width) : 0;
        int totalH = btnH + paramH + (expanded ? gap : 0);
        int advanceY = btnH + paramH + gap;
>>>>>>> 0ab37177398daa0e9880b2ec0d3ee76a2dbed416

        if (mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + totalH) {
            if (mouseY <= currentY + btnH) {
                if (button == 0) {
                    module.toggle();
                } else if (button == 1 && !module.getParameters().isEmpty()) {
                    expanded = !expanded;
                    if (expanded) {
                        expandedModules.add(module);
                        inlineScrollTarget = 0;
                    } else {
                        expandedModules.remove(module);
                        module.setGearAngle(0f, System.currentTimeMillis());
                    }
                } else if (button == 2) {
                    ClickGUI.bindingModuleButton = this;
                }
            } else if (expanded) {
                int scrollOffset = Math.round(inlineScrollAnim);
                int pY = currentY + btnH + gap - scrollOffset;
                int paramW = width - 6;
                for (ParameterElement pe : parameterElements) {
                    if (!pe.getParameter().isVisible() && pe.getExpandAnimProgress() < 0.001f) continue;
                    int pH = pe.getHeight();
                    if (mouseX >= x + 4 && mouseX <= x + 4 + paramW && mouseY >= pY && mouseY <= pY + pH) {
                        if (pe.mouseClicked(mouseX, mouseY, button, x + 4, pY, paramW, pH)) {
                            currentYOut[0] = currentY + advanceY;
                            return true;
                        }
                    }
                    pY += pH;
                }
            }
            currentYOut[0] = currentY + advanceY;
            return true;
        }

        currentYOut[0] = currentY + advanceY;
        return false;
    }

    public Module getModule() {
        return module;
    }

    public List<ParameterElement> getParameterElements() {
        return parameterElements;
    }
}
