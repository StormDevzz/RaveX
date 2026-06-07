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

    private long openTime = -1;
    private boolean closing = false;
    private long closingStartTime = -1;

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Component.literal(module.getName() + " Settings"));
        this.parent = parent;
        this.module = module;
        for (Parameter<?> p : module.getParameters()) {
            paramElements.add(new ParameterElement(p));
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        if (openTime == -1) openTime = now;

        float progress = 1.0f;
        if (closing) {
            long elapsed = now - closingStartTime;
            progress = Math.max(0.0f, 1.0f - (elapsed / 130.0f));
            if (progress <= 0.001f) {
                Minecraft.getInstance().setScreen(parent);
                return;
            }
        } else {
            long elapsed = now - openTime;
            progress = Math.min(1.0f, elapsed / 150.0f);
        }

        float scale = progress * (2.0f - progress);
        int activeColor = ColorUtility.getActiveColor();
        int w = this.width;
        int h = this.height;

        int bgAlpha = (int)(progress * 0x77);
        graphics.fill(0, 0, w, h, (bgAlpha << 24) | 0x05050E);

        int panelW = Math.min(280, w - 40);
        int panelX = (w - panelW) / 2;
        int maxPanelH = h - 80;
        int headerH = 32;

        int totalContentH = 0;
        for (ParameterElement pe : paramElements) {
            if (pe.getParameter().isVisible()) totalContentH += pe.getHeight();
        }
        int panelH = Math.min(totalContentH + headerH + 8, maxPanelH);
        int panelY = (h - panelH) / 2;

        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(w / 2.0f, h / 2.0f);
        pose.scale(scale, scale);
        pose.translate(-w / 2.0f, -h / 2.0f);

        float cx = w / 2.0f;
        float cy = h / 2.0f;
        int mx = scale > 0.01f ? (int) ((mouseX - cx) / scale + cx) : mouseX;
        int my = scale > 0.01f ? (int) ((mouseY - cy) / scale + cy) : mouseY;

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xEE141414);
        Render2DEngine.drawBorder(graphics, panelX, panelY, panelW, panelH, 1, 0xFF2C2C2C);

        graphics.fill(panelX, panelY, panelX + panelW, panelY + headerH, 0xFF1C1C1C);
        graphics.fill(panelX, panelY + headerH - 2, panelX + panelW, panelY + headerH, activeColor);

        FontRenderUtility.drawString(graphics, module.getName() + " Settings", panelX + 10, panelY + 8, 0xFFFFFFFF, true);
        FontRenderUtility.drawString(graphics, "ESC to close", panelX + panelW - 80, panelY + 10, 0xFF606080, false);

        int contentY = panelY + headerH + 4;
        int contentH = panelH - headerH - 8;

        float maxScroll = Math.max(0, totalContentH - contentH);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));

        graphics.enableScissor(panelX + 4, contentY, panelX + panelW - 4, contentY + contentH);

        int py = contentY - (int) scrollOffset;
        for (ParameterElement pe : paramElements) {
            if (!pe.getParameter().isVisible()) continue;
            int pHeight = pe.getHeight();
            if (py + pHeight >= contentY - 20 && py <= contentY + contentH + 20) {
                pe.render(graphics, panelX + 10, py, panelW - 20, pHeight, mx, my);
            }
            py += pHeight;
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            int sbX = panelX + panelW - 6;
            int sbY = contentY;
            int sbH = contentH;
            float ratio = sbH / (float) totalContentH;
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
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (ClickGUI.activeColorPalette != null) {
            return true;
        }
        if (closing) return true;
        long now = System.currentTimeMillis();
        float progress = Math.min(1.0f, (now - openTime) / 150.0f);
        float scale = progress * (2.0f - progress);
        if (scale <= 0.05f) return true;

        float cx = width / 2.0f;
        float cy = height / 2.0f;
        double mx = (mouseX - cx) / scale + cx;
        double my = (mouseY - cy) / scale + cy;

        int panelW = Math.min(280, width - 40);
        int panelX = (width - panelW) / 2;
        
        int totalContentH = 0;
        for (ParameterElement pe : paramElements) {
            if (pe.getParameter().isVisible()) totalContentH += pe.getHeight();
        }
        int maxPanelH = height - 80;
        int headerH = 32;
        int panelH = Math.min(totalContentH + headerH + 8, maxPanelH);
        int panelY = (height - panelH) / 2;
        int contentH = panelH - headerH - 8;

        float maxScroll = Math.max(0, totalContentH - contentH);
        if (mx >= panelX && mx <= panelX + panelW && my >= panelY && my <= panelY + panelH) {
            scrollOffset = (float) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * 12));
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
        long now = System.currentTimeMillis();
        float progress = Math.min(1.0f, (now - openTime) / 150.0f);
        float scale = progress * (2.0f - progress);
        if (scale <= 0.05f) return true;

        float cx = width / 2.0f;
        float cy = height / 2.0f;
        double mx = (event.x() - cx) / scale + cx;
        double my = (event.y() - cy) / scale + cy;

        int panelW = Math.min(280, width - 40);
        int panelX = (width - panelW) / 2;
        
        int totalContentH = 0;
        for (ParameterElement pe : paramElements) {
            if (pe.getParameter().isVisible()) totalContentH += pe.getHeight();
        }
        int maxPanelH = height - 80;
        int headerH = 32;
        int panelH = Math.min(totalContentH + headerH + 8, maxPanelH);
        int panelY = (height - panelH) / 2;

        int contentY = panelY + headerH + 4;
        int py = contentY - (int) scrollOffset;
        for (ParameterElement pe : paramElements) {
            if (!pe.getParameter().isVisible()) continue;
            int pHeight = pe.getHeight();
            if (pe.mouseClicked(mx, my, event.button(), panelX + 10, py, panelW - 20, pHeight)) {
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
