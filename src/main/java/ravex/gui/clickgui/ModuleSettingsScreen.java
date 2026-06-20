package ravex.gui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import ravex.modules.Module;
import ravex.parameter.*;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;
import com.mojang.math.Axis;

import java.util.ArrayList;
import java.util.List;

public class ModuleSettingsScreen extends Screen {


    private final Screen parent;
    private final Module module;
    private final List<ParameterElement> paramElements = new ArrayList<>();
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;

    private long openTime = -1;
    private boolean closing = false;
    private long closingStartTime = -1;

    private int lastPanelW = 360;
    private int lastPanelX = 0;
    private int lastPanelY = 0;
    private int lastPanelH = 0;
    private int lastHeaderH = 32;
    private int lastContentH = 0;
    private int lastTotalContentH = 0;
    private float lastScale = 1.0f;

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Component.literal(module.getName() + " Settings"));
        this.parent = parent;
        this.module = module;
        for (Parameter<?> p : module.getParameters()) {
            paramElements.add(new ParameterElement(p));
        }
        ravex.utility.sound.SoundUtility.playSettingsOpen();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void updatePanelLayout() {
        lastPanelW = Math.min(360, width - 40);
        lastPanelX = (width - lastPanelW) / 2;
        lastTotalContentH = 0;
        for (ParameterElement pe : paramElements) {
            if (pe.getParameter().isVisible() || pe.getExpandAnimProgress() > 0.001f) lastTotalContentH += pe.getHeight();
        }
        int maxPanelH = height - 80;
        lastPanelH = Math.min(lastTotalContentH + lastHeaderH + 8, maxPanelH);
        lastPanelY = (height - lastPanelH) / 2;
        lastContentH = lastPanelH - lastHeaderH - 8;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        if (openTime == -1) openTime = now;

        float progress = 1.0f;
        if (closing) {
            long elapsed = now - closingStartTime;
            progress = Math.max(0.0f, 1.0f - (elapsed / 300.0f));
            if (progress <= 0.001f) {
                Minecraft.getInstance().setScreen(parent);
                return;
            }
        } else {
            long elapsed = now - openTime;
            progress = Math.min(1.0f, elapsed / 250.0f);
        }

        float ease = progress * progress * (3.0f - 2.0f * progress);
        float scale = closing ? ease * 0.9f + 0.1f : 0.7f + 0.3f * ease;
        lastScale = scale;
        int activeColor = ColorUtility.getActiveColor();
        int w = this.width;
        int h = this.height;

        updatePanelLayout();

        graphics.fill(0, 0, w, h, 0xFF05050E);

        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(w / 2.0f, h / 2.0f);
        pose.scale(scale, scale);
        pose.translate(-w / 2.0f, -h / 2.0f);

        float cx = w / 2.0f;
        float cy = h / 2.0f;
        int mx = scale > 0.01f ? (int) ((mouseX - cx) / scale + cx) : mouseX;
        int my = scale > 0.01f ? (int) ((mouseY - cy) / scale + cy) : mouseY;

        int px = lastPanelX;
        int py = lastPanelY;
        int pw = lastPanelW;
        int ph = lastPanelH;
        int hh = lastHeaderH;

        graphics.fill(px, py, px + pw, py + ph, 0xEE141414);
        Render2DEngine.drawBorder(graphics, px, py, pw, ph, 1, 0xFF2C2C2C);

        graphics.fill(px, py, px + pw, py + hh, 0xFF1C1C1C);
        graphics.fill(px, py + hh - 2, px + pw, py + hh, activeColor);

        FontRenderUtility.drawString(graphics, module.getName() + " Settings", px + 10, py + 8, 0xFFFFFFFF, true);
        FontRenderUtility.drawString(graphics, "ESC to close", px + pw - 80, py + 10, 0xFF606080, false);

        int contentY = py + hh + 4;
        int contentH = lastContentH;

        float maxScroll = Math.max(0, lastTotalContentH - contentH);
        if (targetScrollOffset < scrollOffset) {
            scrollOffset = Math.max(targetScrollOffset, scrollOffset - 8.0f);
        } else if (targetScrollOffset > scrollOffset) {
            scrollOffset = Math.min(targetScrollOffset, scrollOffset + 8.0f);
        }
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
        targetScrollOffset = Math.max(0, Math.min(maxScroll, targetScrollOffset));

        graphics.enableScissor(px + 4, contentY, px + pw - 4, contentY + contentH);

        int paramY = contentY - (int) scrollOffset;
        for (ParameterElement pe : paramElements) {
            if (!pe.getParameter().isVisible() && pe.getExpandAnimProgress() < 0.001f) continue;
            int pHeight = pe.getHeight();
            if (paramY + pHeight >= contentY - 20 && paramY <= contentY + contentH + 20) {
                pe.render(graphics, px + 10, paramY, pw - 20, pHeight, mx, my);
            }
            paramY += pHeight;
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            int sbX = px + pw - 6;
            int sbY = contentY;
            int sbH = contentH;
            float ratio = sbH / (float) lastTotalContentH;
            int thumbH = Math.max(12, (int) (sbH * ratio));
            int thumbY = sbY + (int) ((sbH - thumbH) * (scrollOffset / maxScroll));
            graphics.fill(sbX, sbY, sbX + 2, sbY + sbH, 0x2215152A);
            graphics.fill(sbX, thumbY, sbX + 2, thumbY + thumbH, ColorUtility.withAlpha(activeColor, 80));
        }

        pose.popMatrix();

        if (ClickGUI.activeColorPalette != null) {
            ClickGUI.activeColorPalette.render(graphics, mouseX, mouseY, w, h);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void closeScreen() {
        if (!closing) {
            closing = true;
            closingStartTime = System.currentTimeMillis();
            ravex.utility.sound.SoundUtility.playSettingsClose();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (ClickGUI.activeColorPalette != null) return true;
        if (closing) return true;

        float s = lastScale;
        if (s <= 0.05f) return true;

        float cxx = width / 2.0f;
        float cyy = height / 2.0f;
        double mx = (mouseX - cxx) / s + cxx;
        double my = (mouseY - cyy) / s + cyy;

        if (mx >= lastPanelX && mx <= lastPanelX + lastPanelW && my >= lastPanelY && my <= lastPanelY + lastPanelH) {
            float maxScroll = Math.max(0, lastTotalContentH - lastContentH);
            targetScrollOffset = Math.max(0, Math.min(maxScroll, targetScrollOffset - (float)verticalAmount * 18));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (ClickGUI.activeColorPalette != null) {
            return ClickGUI.activeColorPalette.mouseClicked(event.x(), event.y(), event.button());
        }
        if (closing) return true;

        float s = lastScale;
        if (s <= 0.05f) return true;

        float cxx = width / 2.0f;
        float cyy = height / 2.0f;
        double mx = (event.x() - cxx) / s + cxx;
        double my = (event.y() - cyy) / s + cyy;

        int contentY = lastPanelY + lastHeaderH + 4;
        int py = contentY - (int) scrollOffset;
        for (ParameterElement pe : paramElements) {
            if (!pe.getParameter().isVisible() && pe.getExpandAnimProgress() < 0.001f) continue;
            int pHeight = pe.getHeight();
            if (pe.mouseClicked(mx, my, event.button(), lastPanelX + 10, py, lastPanelW - 20, pHeight)) {
                return true;
            }
            py += pHeight;
        }

        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ClickGUI.activeColorPalette != null) {
            return ClickGUI.activeColorPalette.keyPressed(event.key());
        }

        int key = event.key();

        if (ClickGUI.activeKeybindElement != null) {
            ravex.parameter.KeybindParameter kp = (ravex.parameter.KeybindParameter) ClickGUI.activeKeybindElement.getParameter();
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                kp.setValue(org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN);
                ClickGUI.activeKeybindElement = null;
            } else {
                kp.setValue(key);
                ClickGUI.activeKeybindElement = null;
            }
            if (this.minecraft.player != null) {
                this.minecraft.player.playSound(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                    0.5f, 1.5f
                );
            }
            return true;
        }

        if (ClickGUI.activeStringParameterElement != null) {
            ravex.parameter.StringParameter sp = (ravex.parameter.StringParameter) ClickGUI.activeStringParameterElement.getParameter();
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                ClickGUI.activeStringParameterElement = null;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String val = sp.getValue();
                if (!val.isEmpty()) {
                    sp.setValue(val.substring(0, val.length() - 1));
                }
                return true;
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            closeScreen();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (ClickGUI.activeColorPalette != null) {
            return true;
        }
        if (ClickGUI.activeKeybindElement != null) {
            return true;
        }
        if (ClickGUI.activeStringParameterElement != null) {
            ravex.parameter.StringParameter sp = (ravex.parameter.StringParameter) ClickGUI.activeStringParameterElement.getParameter();
            String text = event.codepointAsString();
            if (!text.isEmpty() && text.charAt(0) >= 32 && text.charAt(0) < 127) {
                sp.setValue(sp.getValue() + text);
            }
            return true;
        }
        return super.charTyped(event);
    }
}
