package ravex.gui.clickgui;
import ravex.utility.render.ColorUtility;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.modules.Module;
import ravex.modules.client.ClickGui;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.animate.AnimationUtility;
import ravex.parameter.Parameter;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ravex.manager.ModuleManager;
import ravex.event.EventBusHolder;
import ravex.event.client.SoundEvent;
import ravex.modules.Modules;

public class ModuleButton {
    public static final Set<Module> expandedModules = new HashSet<>();
    private static long lastGearTick = System.currentTimeMillis();
    private static float frameDt = 1f / 60f;
    private static final int MAX_INLINE_HEIGHT = 800;
    // polled once per frame, not per button
    private static boolean lmbDown = false;

    public static float getFrameDt() {
        return frameDt;
    }

    private float inlineScrollTarget = 0f;
    private float inlineScrollAnim = 0f;
    private float searchReveal = 1.0f;
    private float lastRevealH = 1.0f;
    private boolean matchesSearch = true;

    public static void tickAllGears() {
        long now = System.currentTimeMillis();
        float dt = Math.min(100f, now - lastGearTick) / 1000f;
        lastGearTick = now;
        frameDt = dt;
        try {
            lmbDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                MinecraftWrapper.getWrapper().getWindow().handle(),
                org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        } catch (Throwable ignored) {
            lmbDown = false;
        }
        float speed = (float) ModuleManager.get(ClickGui.class).gearRotationSpeed;
        if (speed > 0) {
            for (Module m : expandedModules) {
                float cur = m.getGearAngle();
                m.setGearAngle(cur + speed * dt, now);
            }
        }
        float decay = (float) Math.exp(-dt * 12f);
        for (Module m : ravex.manager.ModuleManager.INSTANCE.getModules()) {
            if (expandedModules.contains(m)) continue;
            float cur = m.getGearAngle();
            if (Math.abs(cur) > 0.01f) {
                float newAngle = cur * decay;
                if (Math.abs(newAngle) < 0.01f) newAngle = 0f;
                m.setGearAngle(newAngle, now);
            }
        }
    }

    private final Module module;
    private final String searchKey;
    private final List<ParameterElement> parameterElements = new ArrayList<>();
    private float hoverProgress = 0.0f;
    private float enableAnim = 0.0f;
    private boolean expanded = false;
    private float expandAnim = 0.0f;
    private float closingGearAngle = 0f;
    private float hoverGlow = 0.0f;
    private float bindAnim = 0.0f;
    private float bindingShowAnim = 0.0f;
    private long flashStartMs = -1L;
    private float flashPeak = 1f;
    private static final long FLASH_DURATION_MS = 600L;

    private float updateFlash() {
        if (flashStartMs < 0L) return 0f;
        long elapsed = System.currentTimeMillis() - flashStartMs;
        if (elapsed >= FLASH_DURATION_MS) {
            flashStartMs = -1L;
            return 0f;
        }
        float t = elapsed / (float) FLASH_DURATION_MS;
        return flashPeak * (1f - AnimationUtility.Easing.CUBIC_OUT.apply(t));
    }

    void triggerFlash(float peak) {
        flashPeak = peak;
        flashStartMs = System.currentTimeMillis();
    }

    // wobble when a right click finds no settings
    private long shakeStartNs = -1L;
    private static final long SHAKE_MS = 240L;

    private float updateShake() {
        if (shakeStartNs < 0L) return 0f;
        float t = (System.nanoTime() - shakeStartNs) / (SHAKE_MS * 1_000_000f);
        if (t >= 1f) {
            shakeStartNs = -1L;
            return 0f;
        }
        return (float) Math.sin(t * Math.PI * 6.0) * 3.5f * (1f - t);
    }

    public void triggerShake() {
        shakeStartNs = System.nanoTime();
    }
    private double circleX = -1;
    private double circleY = -1;
    private final Render2DUtility.HoverGlowRenderer glowRenderer;
    private boolean lastHovered = false;
    private boolean wasEnabled = false;
    private long enableStartNs = -1L;
    private float enableFrom = 0f;
    private static final long ENABLE_MS = 240L;
    private static final long DISABLE_MS = 180L;
    private float pressAnim = 0.0f;
    private static final float PRESS_SCALE_MIN = 0.93f;
    private static final float PRESS_DOWN_SPEED = 18f;
    private static final float PRESS_UP_SPEED = 10f;

    public ModuleButton(Module module) {
        this.module = module;
        this.searchKey = module.getName() + " " + ravex.utility.misc.LanguageUtility.moduleName(module.getName());
        this.glowRenderer = new Render2DUtility.HoverGlowRenderer("btn_glow_" + module.getName());
        wasEnabled = module.getEnabled();
        if (module.getKeyBind() != org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) {
            bindAnim = 1.0f;
        }
        for (Parameter<?> p : module.getParameters()) {
            parameterElements.add(new ParameterElement(p, module.getName()));
        }
    }

    // built once, search pass runs each frame
    public String getSearchKey() { return searchKey; }

    public boolean isExpanded() { return expanded || expandAnim > 0.001f; }

    public void collapse() {
        expanded = false;
        closingGearAngle = 0f;
        inlineScrollTarget = 0f;
        inlineScrollAnim = 0f;
        expandedModules.remove(module);
    }

    public void onGuiOpen() {
        collapse();
        expandAnim = 0f;
        hoverProgress = 0f;
        hoverGlow = 0f;
        circleX = -1;
        circleY = -1;
        lastHovered = false;
        pressAnim = 0f;
        flashStartMs = -1L;
        shakeStartNs = -1L;
        wasEnabled = module.getEnabled();
        enableAnim = wasEnabled && !"Settings".equals(module.getName()) ? 1.0f : 0.0f;
        enableStartNs = -1L;
        bindAnim = module.getKeyBind() != org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN ? 1.0f : 0.0f;
        bindingShowAnim = 0.0f;
        searchReveal = 1.0f;
        lastRevealH = 1.0f;
        matchesSearch = true;
    }

    public float getSearchReveal() { return searchReveal; }

    public void updateSearchReveal(boolean matches, boolean hasQuery) {
        matchesSearch = matches;
        float target = (!hasQuery || matches) ? 1.0f : 0.0f;
        if (searchReveal < target)
            searchReveal = Math.min(target, searchReveal + 0.35f);
        else if (searchReveal > target)
            searchReveal = Math.max(target, searchReveal - 0.30f);
        // same factor as the panel height math
        lastRevealH = (!hasQuery || matches) ? 1.0f : searchReveal;
    }

    // row height this frame, also used to skip offscreen rows
    public int getLayoutHeight(int panelWidth) {
        int btnH = (int) ModuleManager.get(ClickGui.class).buttonHeight;
        int row = Math.max(1, (int) (btnH * lastRevealH)) + 2;
        if (!parameterElements.isEmpty() && expandAnim > 0.0001f) {
            row += (int) (getExpandedHeight(panelWidth) * lastRevealH);
        }
        return row;
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
        if (lastRevealH < 0.001f) {
            // search hidden rows still take layout space
            currentYOut[0] = currentY + getLayoutHeight(width);
            return;
        }
        int btnH = (int) ModuleManager.get(ClickGui.class).buttonHeight;
        int btnRadius = Math.min((int) ModuleManager.get(ClickGui.class).cornerRadius, btnH / 2);
        int btnX = x + 2;
        int btnW = width - 4;

        boolean inRect = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= currentY && mouseY <= currentY + btnH
            && currentY + btnH > viewTop && currentY < viewBot;
        boolean hovered = inRect;
        if (inRect && btnRadius > 0) {
            if (mouseX < btnX + btnRadius) {
                float dx = (btnX + btnRadius) - mouseX;
                if (mouseY < currentY + btnRadius) {
                    float dy = (currentY + btnRadius) - mouseY;
                    if (dx * dx + dy * dy > btnRadius * btnRadius) hovered = false;
                } else if (mouseY > currentY + btnH - btnRadius) {
                    float dy = mouseY - (currentY + btnH - btnRadius);
                    if (dx * dx + dy * dy > btnRadius * btnRadius) hovered = false;
                }
            } else if (mouseX > btnX + btnW - btnRadius) {
                float dx = mouseX - (btnX + btnW - btnRadius);
                if (mouseY < currentY + btnRadius) {
                    float dy = (currentY + btnRadius) - mouseY;
                    if (dx * dx + dy * dy > btnRadius * btnRadius) hovered = false;
                } else if (mouseY > currentY + btnH - btnRadius) {
                    float dy = mouseY - (currentY + btnH - btnRadius);
                    if (dx * dx + dy * dy > btnRadius * btnRadius) hovered = false;
                }
            }
        }

        if (hovered) {
            ClickGUI.hoveredDescription = module.getDescription();
            if (!lastHovered) {
                EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.HOVER, 0.5f));
            }
            hoverProgress = Math.min(1.0f, hoverProgress + 0.10f);
        } else {
            hoverProgress = Math.max(0.0f, hoverProgress - 0.10f);
        }
        lastHovered = hovered;

        float targetAnim = module.getEnabled() ? 1.0f : 0.0f;
        if (module.getEnabled() != wasEnabled) {
            enableFrom = enableAnim;
            enableStartNs = System.nanoTime();
        }
        wasEnabled = module.getEnabled();
        boolean enabledNow = module.getEnabled();

        // eased over real time, so it feels the same at any fps
        // settings stays functional, always drawn as off
        if ("Settings".equals(module.getName())) {
            enableAnim = 0f;
            enableStartNs = -1L;
        } else if (enableStartNs >= 0L) {
            long durNs = (enabledNow ? ENABLE_MS : DISABLE_MS) * 1_000_000L;
            float t = Math.min(1f, (System.nanoTime() - enableStartNs) / (float) durNs);
            enableAnim = enableFrom + (targetAnim - enableFrom) * AnimationUtility.Easing.QUART_BOTH.apply(t);
            if (t >= 1f) enableStartNs = -1L;
        } else {
            enableAnim = targetAnim;
        }

        // shrink while LMB held, spring back on release
        float pressTarget = (hovered && lmbDown) ? 1.0f : 0.0f;
        float pressSpeed = pressTarget > pressAnim ? PRESS_DOWN_SPEED : PRESS_UP_SPEED;
        float pressStep = 1f - (float) Math.exp(-getFrameDt() * pressSpeed);
        pressAnim += (pressTarget - pressAnim) * pressStep;
        if (Math.abs(pressTarget - pressAnim) < 0.003f) pressAnim = pressTarget;
        float pressEased = AnimationUtility.Easing.CUBIC_OUT.apply(Math.max(0f, Math.min(1f, pressAnim)));
        float pressScale = 1f - (1f - PRESS_SCALE_MIN) * pressEased;
        boolean scaled = pressScale < 0.999f;

        int activeColor = ColorUtility.getActiveColor();
        int btnAlpha = (int) ModuleManager.get(ClickGui.class).buttonOpacity;
        int disabledBg = ColorUtility.withAlpha(0x252530, btnAlpha);

        int mergedBg = disabledBg;
        if (enableAnim > 0.01f) {
            int enableAlpha = (int) (enableAnim * Math.min(255, btnAlpha * 3));
            mergedBg = ColorUtility.overlay(mergedBg, ColorUtility.withAlpha(activeColor, enableAlpha));
        }
        if (hoverProgress > 0.01f && enableAnim < 0.01f) {
            int hoverAlpha = (int) (hoverProgress * Math.min(30, btnAlpha / 2));
            mergedBg = ColorUtility.overlay(mergedBg, ColorUtility.withAlpha(0xFFFFFFFF, hoverAlpha));
        }
        float flash = updateFlash();
        if (flash > 0.003f) {
            int flashAlpha = (int) (flash * Math.min(40, btnAlpha / 3));
            mergedBg = ColorUtility.overlay(mergedBg, ColorUtility.withAlpha(activeColor, flashAlpha));
        }
        if (pressEased > 0.01f) {
            int pressAlpha = (int) (pressEased * 45);
            mergedBg = ColorUtility.overlay(mergedBg, ColorUtility.withAlpha(0x000000, pressAlpha));
        }

        // centered scale keeps the plate inside the rounded bounds
        // layout keeps full size, list never jumps while pressed
        float shakeX = updateShake();
        boolean shaking = shakeX != 0f;
        float pressCx = btnX + btnW / 2f;
        float pressCy = currentY + btnH / 2f;
        if (scaled || shaking) {
            graphics.pose().pushMatrix();
            if (scaled) {
                graphics.pose().translate(pressCx, pressCy);
                graphics.pose().scale(pressScale, pressScale);
                graphics.pose().translate(-pressCx, -pressCy);
            }
            if (shaking) {
                graphics.pose().translate(shakeX, 0f);
            }
        }

        // plain rounded path while animating, pixel perfect otherwise
        if (scaled || shaking) {
            Render2DUtility.drawRound(graphics, btnX, currentY, btnW, btnH, btnRadius, mergedBg);
        } else {
            Render2DUtility.drawPixelPerfectRound(graphics, btnX, currentY, btnW, btnH, btnRadius, mergedBg);
        }
        if (ModuleManager.get(ClickGui.class).buttonOutlines) {
            int outlineCol = ModuleManager.get(ClickGui.class).outlineColor;
            if (scaled || shaking) {
                Render2DUtility.drawSmoothRoundOutline(graphics, btnX, currentY, btnW, btnH, btnRadius, 1, outlineCol);
            } else {
                // same snap as the body, border sits exactly on the edge
                Render2DUtility.drawPixelPerfectRoundBorder(graphics, btnX, currentY, btnW, btnH, btnRadius, 1, outlineCol);
            }
        }

        if (hovered) {
            hoverGlow = Math.min(1.0f, hoverGlow + 0.12f);
            float targetGX = (float) mouseX;
            float targetGY = currentY + btnH / 2.0f;
            if (circleX < 0) {
                circleX = targetGX;
                circleY = targetGY;
            } else {
                circleX += (targetGX - circleX) * 0.28;
                circleY += (targetGY - circleY) * 0.28;
            }
        } else {
            hoverGlow = Math.max(0.0f, hoverGlow - 0.10f);
            if (hoverGlow <= 0.01f) {
                circleX = -1;
            }
        }
        if (hoverGlow > 0.01f) {
            int alpha = (int) (hoverGlow * 125);
            int whiteGlow = ColorUtility.withAlpha(0xFFFFFFFF, alpha);
            glowRenderer.render(graphics, btnX, currentY, btnW, btnH, btnRadius,
                (float) circleX, (float) circleY, 26.0f, 13.0f, whiteGlow);
        }

        if (searchQuery != null && !searchQuery.isEmpty() && matchesSearch) {
            int searchBg = ColorUtility.overlay(mergedBg, ColorUtility.withAlpha(activeColor, 30));
            Render2DUtility.drawRound(graphics, x + 2, currentY, width - 4, btnH, btnRadius, searchBg);
        }

        int baseColor = ColorUtility.interpolate(0xFFB0B0C0, activeColor, enableAnim);
        // label brightens with hoverProgress, not the hover flag
        int textColor = ColorUtility.interpolate(baseColor, 0xFFFFFFFF, AnimationUtility.Easing.CUBIC_OUT.apply(hoverProgress));

        String displayName = ravex.utility.misc.LanguageUtility.moduleName(module.getName());
        boolean binding = ClickGUI.bindingModuleButton == this;
        String bindSuffix = "";
        if (!binding && module.getKeyBind() != org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) {
            String keyName = org.lwjgl.glfw.GLFW.glfwGetKeyName(module.getKeyBind(), 0);
            if (keyName == null) {
                if (module.getKeyBind() == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) keyName = "RShift";
                else if (module.getKeyBind() == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) keyName = "LShift";
                else if (module.getKeyBind() == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) keyName = "Space";
                else keyName = ravex.utility.misc.LanguageUtility.t("mb_key") + module.getKeyBind();
            } else {
                keyName = keyName.toUpperCase();
            }
            bindSuffix = " [" + keyName + "]";
        }

        float bindTarget = bindSuffix.isEmpty() ? 0.0f : 1.0f;
        if (bindAnim < bindTarget) {
            bindAnim = Math.min(bindTarget, bindAnim + 0.14f);
        } else if (bindAnim > bindTarget) {
            bindAnim = Math.max(bindTarget, bindAnim - 0.14f);
        }

        float bindingTarget = binding ? 1.0f : 0.0f;
        if (bindingShowAnim < bindingTarget) {
            bindingShowAnim = Math.min(bindingTarget, bindingShowAnim + 0.16f);
        } else if (bindingShowAnim > bindingTarget) {
            bindingShowAnim = Math.max(bindingTarget, bindingShowAnim - 0.12f);
        }

        int textY = currentY + (btnH - FontRenderUtility.getFontHeight()) / 2 + 1;
        int textX = x + (int) ModuleManager.get(ravex.modules.client.Settings.class).moduleTextX;

        float bindingEase = AnimationUtility.Easing.CUBIC_OUT.apply(bindingShowAnim);
        float nameAlpha = 1f - bindingEase;
        if (nameAlpha > 0.01f) {
            int nameColor = ColorUtility.withAlpha(textColor, (int) (nameAlpha * 255));
            if (searchQuery != null && !searchQuery.isEmpty() && !module.getName().isEmpty()) {
                renderHighlightedName(graphics, displayName, textX, textY, nameColor, searchQuery);
            } else {
                FontRenderUtility.drawString(graphics, displayName, textX, textY, nameColor, true);
            }

            float bindEase = AnimationUtility.Easing.CUBIC_OUT.apply(bindAnim);
            float bindAlpha = bindEase * ClickGUI.bindReveal * nameAlpha;
            if (!bindSuffix.isEmpty() && bindAlpha > 0.01f) {
                int suffixX = textX + FontRenderUtility.getStringWidth(displayName)
                    + (int) ((1f - bindEase) * 4f + (1f - ClickGUI.bindReveal) * 4f);
                int bindColor = ColorUtility.withAlpha(textColor, (int) (bindAlpha * 255));
                FontRenderUtility.drawString(graphics, bindSuffix, suffixX, textY, bindColor, true);
            }
        }

        if (bindingEase > 0.01f) {
            float labelAlpha = bindingEase * ClickGUI.bindReveal;
            if (labelAlpha > 0.01f) {
                renderBindingLabel(graphics, x, width, textY, textColor, labelAlpha);
            }
        }

        boolean hasParams = !module.getParameters().isEmpty();
        if (hasParams && ModuleManager.get(ClickGui.class).showGear) {
            Identifier settingsTex = ravex.utility.render.TextureLoaderUtility.getSettingsWhiteTexture();
            if (settingsTex == null) settingsTex = ravex.utility.render.TextureLoaderUtility.getSettingsTexture();
            if (settingsTex != null) {
                int iconSize = 10;
                int iconX = x + width - iconSize - 8;
                int iconY = currentY + (btnH - iconSize) / 2;
                float openProgress = AnimationUtility.Easing.CUBIC_OUT.apply(expandAnim);
                float angle = openProgress * 90f + module.getGearAngle();
                var pose = graphics.pose();
                if (Math.abs(angle) > 0.01f) {
                    pose.pushMatrix();
                    pose.translate(iconX + iconSize / 2f, iconY + iconSize / 2f);
                    pose.rotate(angle * (float)Math.PI / 180f);
                    pose.translate(-(iconX + iconSize / 2f), -(iconY + iconSize / 2f));
                    graphics.blit(settingsTex, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f);
                    pose.popMatrix();
                } else {
                    graphics.blit(settingsTex, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f);
                }
            } else {
                String indicator = expanded ? "-" : "+";
                FontRenderUtility.drawString(graphics, indicator, x + width - 12, textY, 0xFF7A7A8A, true);
            }
        }

        Identifier modIcon = ravex.utility.render.TextureLoaderUtility.getModuleIcon(module.getName());
        if (modIcon != null) {
            int iconSize = 14;
            int iconX = x + width - iconSize - 4;
            int iconY = currentY + btnH - iconSize - 2;
            graphics.blit(modIcon, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f);
        }

        if (scaled || shaking) {
            graphics.pose().popMatrix();
        }

        currentY += btnH + 2;

        if (hasParams) {
            float targetExpand = expanded ? 1.0f : 0.0f;
            float speed = expanded ? 9f : 5f;
            float expandStep = 1f - (float) Math.exp(-getFrameDt() * speed);
            expandAnim += (targetExpand - expandAnim) * expandStep;
            if (Math.abs(targetExpand - expandAnim) < 0.00001f) {
                expandAnim = targetExpand;
            }
            if (expandAnim > 0.0f) {
                int paramAreaH = 0;
                int paramW = width - 6;
                for (ParameterElement pe : parameterElements) {
                    if (pe.getParameter().isVisible() || pe.getExpandAnimProgress() > 0.001f) {
                        paramAreaH += pe.getHeight();
                    }
                }
                int actualH = getExpandedHeight(width);
                int bgCol = ColorUtility.withAlpha(0x0A0A14, Math.max(btnAlpha / 2, 24));
                Render2DUtility.drawRound(graphics, x + 3, currentY, width - 6, actualH, Math.max(4, btnRadius - 2), bgCol);

                int accentH = (int) (actualH * expandAnim);
                if (accentH > 1) {
                    int barColor = ColorUtility.withAlpha(activeColor, (int) (expandAnim * 150));
                    Render2DUtility.drawRound(graphics, x + 5, currentY + 1, 2, accentH, 1, barColor);
                }

                inlineScrollAnim = inlineScrollTarget;
                int scrollOffset = Math.round(inlineScrollAnim);
                int pY = currentY + 2 - scrollOffset;
                int visTop = currentY + 2;
                int visBot = currentY + actualH - 2;
                Render2DUtility.pushScissor(graphics, x + 3, currentY, width - 6, actualH);
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

                Render2DUtility.popScissor(graphics);
                currentY += actualH;
            }
        } else {
            expandAnim += (0.0f - expandAnim) * (1f - (float) Math.exp(-getFrameDt() * 5f));
            if (expandAnim < 0.0001f) expandAnim = 0.0f;
        }

        boolean searching = searchQuery != null && !searchQuery.isEmpty();
        float revealH = lastRevealH;
        if (revealH < 0.99f) {
            int fadeAlpha = (int)((1.0f - revealH) * 200);
            Render2DUtility.drawRound(graphics, x + 2, currentYOut[0], width - 4, btnH, Math.min((int) ModuleManager.get(ClickGui.class).cornerRadius, btnH / 2), (fadeAlpha << 24) | 0x050510);
        }
        // same value the panel height math uses
        currentYOut[0] = currentYOut[0] + getLayoutHeight(width);
    }

    private void renderBindingLabel(GuiGraphics graphics, int x, int width, int y, int textColor, float alpha) {
        String base = ravex.utility.misc.LanguageUtility.t("mb_binding");
        String dot = ".";
        int baseW = FontRenderUtility.getStringWidth(base);
        int dotW = FontRenderUtility.getStringWidth(dot);
        int gap = 2;
        int spaceBeforeDots = 2;
        int totalW = baseW + spaceBeforeDots + dotW * 3 + gap * 2;
        int startX = x + (width - totalW) / 2;

        int baseColor = ColorUtility.withAlpha(textColor, (int) (alpha * 255));
        FontRenderUtility.drawString(graphics, base, startX, y, baseColor, true);

        long now = System.currentTimeMillis();
        int dotsX = startX + baseW + spaceBeforeDots;
        for (int i = 0; i < 3; i++) {
            double phase = now * 0.008 - i * 0.85;
            float wave = (float) (0.5 + 0.5 * Math.sin(phase));
            float eased = AnimationUtility.Easing.CUBIC_OUT.apply(wave);
            float dotAlpha = alpha * (0.2f + 0.8f * eased);
            int dotColor = ColorUtility.withAlpha(textColor, (int) (dotAlpha * 255));
            int dx = dotsX + i * (dotW + gap);
            int dy = y + (eased > 0.5f ? 0 : 1);
            FontRenderUtility.drawString(graphics, dot, dx, dy, dotColor, true);
        }
    }

    private void renderHighlightedName(GuiGraphics graphics, String text, int x, int y, int baseColor, String query) {
        int fontH = FontRenderUtility.getFontHeight();
        int[] match = ravex.utility.misc.SearchUtility.findMatch(text, query);
        if (match == null) {
            FontRenderUtility.drawString(graphics, text, x, y, baseColor, true);
            return;
        }
        int matchIdx = match[0];
        int queryLen = match[1];
        if (matchIdx < 0) matchIdx = 0;
        if (matchIdx + queryLen > text.length()) queryLen = Math.max(0, text.length() - matchIdx);

        int currentX = x;
        if (matchIdx > 0) {
            String before = text.substring(0, matchIdx);
            FontRenderUtility.drawString(graphics, before, currentX, y, baseColor, true);
            currentX += FontRenderUtility.getStringWidth(before);
        }

        String matched = text.substring(matchIdx, matchIdx + queryLen);
        graphics.fill(currentX - 1, y - 1, currentX + FontRenderUtility.getStringWidth(matched) + 1, y + fontH + 1, 0x44FFAA00);
        FontRenderUtility.drawString(graphics, matched, currentX, y, 0xFFFFFF80, true);
        currentX += FontRenderUtility.getStringWidth(matched);

        if (matchIdx + queryLen < text.length()) {
            FontRenderUtility.drawString(graphics, text.substring(matchIdx + queryLen), currentX, y, baseColor, true);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int width, int[] currentYOut, net.minecraft.client.Minecraft mc) {
        int currentY = currentYOut[0];
        int btnH = (int) ModuleManager.get(ClickGui.class).buttonHeight;
        int gap = 2;
        int paramH = expanded ? getExpandedHeight(width) : 0;
        int totalH = btnH + paramH + (expanded ? gap : 0);
        int advanceY = btnH + paramH + gap;

        if (mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + totalH) {
            if (mouseY <= currentY + btnH) {
                if (button == 0) {
                    module.toggle();
                } else if (button == 1 && !module.getParameters().isEmpty()) {
                    expanded = !expanded;
                    if (expanded) {
                        expandedModules.add(module);
                        inlineScrollTarget = 0;
                        inlineScrollAnim = 0;
                        triggerFlash(1f);
                        EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.SETTINGS_OPEN, 0.6f));
                    } else {
                        expandedModules.remove(module);
                        closingGearAngle = 0f;
                        triggerFlash(0.55f);
                        EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.SETTINGS_CLOSE, 0.6f));
                    }
                } else if (button == 1) {
                    ravex.utility.sound.SoundUtility.playNoSettings();
                    triggerShake();
                } else if (button == 2) {
                    if (ClickGUI.bindingModuleButton != null && ClickGUI.bindingModuleButton != this) {
                        ClickGUI.bindingModuleButton = null;
                    }
                    ClickGUI.bindingModuleButton = this;
                    bindAnim = 0f;
                    bindingShowAnim = 0f;
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
