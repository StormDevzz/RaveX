package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import ravex.parameter.ColorParameter;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;
import ravex.utility.render.TextureLoader;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ColorPaletteModal {
    private final ColorParameter parameter;

    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float value = 1.0f;
    private float alpha = 1.0f;

    private boolean draggingSV = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;

    private static final List<Integer> recentColors = new ArrayList<>();
    private static final int MAX_RECENT = 10;

    private boolean editingHex = false;
    private String hexInput = "";
    private boolean open = true;
    private Runnable onClose = null;

    public boolean isOpen() { return open; }

    private final int modalWidth = 220;
    private final int modalHeight = 305;

    private final int svSize = 120;
    private final int sliderHeight = 10;
    private final int swatchSize = 14;
    private final int swatchGap = 4;

    public ColorPaletteModal(ColorParameter parameter) {
        this.parameter = parameter;
        setFromArgb(parameter.getValue());
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    private void setFromArgb(int argb) {
        alpha = ((argb >>> 24) & 0xFF) / 255.0f;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8)  & 0xFF;
        int b =  argb         & 0xFF;
        float[] hsv = java.awt.Color.RGBtoHSB(r, g, b, null);
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
    }

    public int getArgb() {
        int rgb = hsbToRgb(hue, saturation, value) & 0x00FFFFFF;
        int a = Math.round(alpha * 255) << 24;
        return a | rgb;
    }

    private static int hsbToRgb(float hue, float saturation, float brightness) {
        return java.awt.Color.HSBtoRGB(hue, saturation, brightness);
    }

    private static void addRecentColor(int argb) {
        recentColors.remove((Integer) argb);
        recentColors.add(0, argb);
        while (recentColors.size() > MAX_RECENT) {
            recentColors.remove(recentColors.size() - 1);
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int mx = (screenWidth - modalWidth) / 2;
        int my = (screenHeight - modalHeight) / 2;

        // Dark translucent overlay on screen
        graphics.fill(0, 0, screenWidth, screenHeight, 0x9007070B);

        long win = Minecraft.getInstance().getWindow().handle();
        boolean lmb = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!lmb) {
            draggingSV = draggingHue = draggingAlpha = false;
        }

        int svX = mx + 15;
        int svY = my + 35;
        if (draggingSV) {
            float relX = Math.max(0, Math.min(svSize, mouseX - svX)) / (float) svSize;
            float relY = Math.max(0, Math.min(svSize, mouseY - svY)) / (float) svSize;
            saturation = relX;
            value = 1.0f - relY;
            parameter.setThemeSync(false);
            parameter.setValue(getArgb());
        }

        int hueX = mx + 15;
        int hueY = my + 165;
        int hueW = svSize;
        if (draggingHue) {
            float relX = Math.max(0, Math.min(hueW, mouseX - hueX)) / (float) hueW;
            hue = relX;
            parameter.setThemeSync(false);
            parameter.setValue(getArgb());
        }

        int alphaX = mx + 15;
        int alphaY = my + 183;
        int alphaW = svSize;
        if (draggingAlpha) {
            float relX = Math.max(0, Math.min(alphaW, mouseX - alphaX)) / (float) alphaW;
            alpha = relX;
            parameter.setThemeSync(false);
            parameter.setValue(getArgb());
        }

        // Translucent glass container (8px rounded corners, 75% opacity)
        Render2DEngine.drawRound(graphics, mx, my, modalWidth, modalHeight, 8, 0xCC0D0D14);
        Render2DEngine.drawSmoothRoundOutline(graphics, mx, my, modalWidth, modalHeight, 8, 1, 0xFF1C1C2A);

        int activeColor = getArgb() | 0xFF000000;

        // Static header "Color Palette" as requested
        FontRenderUtility.drawString(graphics, "Color Palette", mx + 12, my + 10, 0xFFE5E5F0, false);

        graphics.fill(mx + 10, my + 23, mx + modalWidth - 10, my + 24, 0xFF252535);

        // 1. Highly optimized SV Gradient Area (uses exactly 2 draw calls instead of 900!)
        int hColor = hsbToRgb(hue, 1.0f, 1.0f);
        Render2DEngine.drawGradientRectHorizontal(graphics, svX, svY, svSize, svSize, 0xFFFFFFFF, hColor);
        Render2DEngine.drawGradientRect(graphics, svX, svY, svSize, svSize, 0x00000000, 0xFF000000);

        graphics.fill(svX - 1, svY - 1, svX + svSize + 1, svY, 0xFF353545);
        graphics.fill(svX - 1, svY + svSize, svX + svSize + 1, svY + svSize + 1, 0xFF353545);
        graphics.fill(svX - 1, svY - 1, svX, svY + svSize + 1, 0xFF353545);
        graphics.fill(svX + svSize, svY - 1, svX + svSize + 1, svY + svSize + 1, 0xFF353545);

        int curX = svX + (int)(saturation * svSize);
        int curY = svY + (int)((1.0f - value) * svSize);

        // Draw a premium fully filled and smoothed white circle for the SV picker cursor
        Render2DEngine.fillCircle(graphics, curX, curY, 4, 0xFF000000);
        Render2DEngine.fillCircle(graphics, curX, curY, 3, 0xFFFFFFFF);

        // 2. Highly optimized Hue slider (uses exactly 6 draw calls instead of 30!)
        int hueSegments = 6;
        int segW = hueW / hueSegments;
        int[] hueColors = { 0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000 };
        for (int s = 0; s < hueSegments; s++) {
            Render2DEngine.drawGradientRectHorizontal(graphics, hueX + s * segW, hueY, segW, sliderHeight, hueColors[s], hueColors[s + 1]);
        }

        graphics.fill(hueX - 1, hueY - 1, hueX + hueW + 1, hueY, 0xFF353545);
        graphics.fill(hueX - 1, hueY + sliderHeight, hueX + hueW + 1, hueY + sliderHeight + 1, 0xFF353545);
        graphics.fill(hueX - 1, hueY - 1, hueX, hueY + sliderHeight + 1, 0xFF353545);
        graphics.fill(hueX + hueW, hueY - 1, hueX + hueW + 1, hueY + sliderHeight + 1, 0xFF353545);

        int hkX = hueX + (int)(hue * hueW);
        graphics.fill(hkX - 1, hueY - 2, hkX + 2, hueY + sliderHeight + 2, 0xFFFFFFFF);
        graphics.fill(hkX - 2, hueY - 2, hkX - 1, hueY + sliderHeight + 2, 0xFF000000);
        graphics.fill(hkX + 2, hueY - 2, hkX + 3, hueY + sliderHeight + 2, 0xFF000000);

        // 3. Highly optimized Alpha background grid (uses exactly 12 draw calls instead of 60!)
        int cellW = 10;
        for (int px = 0; px < alphaW; px += cellW) {
            boolean light = (px / cellW) % 2 == 0;
            int chk = light ? 0xFF2A2A3A : 0xFF1A1A25;
            graphics.fill(alphaX + px, alphaY, alphaX + Math.min(px + cellW, alphaW), alphaY + sliderHeight, chk);
        }

        int currentRgb = hsbToRgb(hue, saturation, value) & 0x00FFFFFF;
        Render2DEngine.drawGradientRectHorizontal(graphics, alphaX, alphaY, alphaW, sliderHeight, currentRgb, (0xFF << 24) | currentRgb);

        graphics.fill(alphaX - 1, alphaY - 1, alphaX + alphaW + 1, alphaY, 0xFF353545);
        graphics.fill(alphaX - 1, alphaY + sliderHeight, alphaX + alphaW + 1, alphaY + sliderHeight + 1, 0xFF353545);
        graphics.fill(alphaX - 1, alphaY - 1, alphaX, alphaY + sliderHeight + 1, 0xFF353545);
        graphics.fill(alphaX + alphaW, alphaY - 1, alphaX + alphaW + 1, alphaY + sliderHeight + 1, 0xFF353545);

        int akX = alphaX + (int)(alpha * alphaW);
        graphics.fill(akX - 1, alphaY - 2, akX + 2, alphaY + sliderHeight + 2, 0xFFFFFFFF);
        graphics.fill(akX - 2, alphaY - 2, akX - 1, alphaY + sliderHeight + 2, 0xFF000000);
        graphics.fill(akX + 2, alphaY - 2, akX + 3, alphaY + sliderHeight + 2, 0xFF000000);

        // Right side section alignment (preview circle, hex and switcher)
        int previewRadius = 20;
        int previewCX = mx + 175;
        int previewCY = my + 55;

        // Draw preview circle using drawRound with corner radius = 20 (forming a perfect, accurately colored vector circle!)
        Render2DEngine.drawRound(graphics, previewCX - previewRadius, previewCY - previewRadius, previewRadius * 2, previewRadius * 2, previewRadius, getArgb());
        Render2DEngine.drawSmoothRoundOutline(graphics, previewCX - previewRadius, previewCY - previewRadius, previewRadius * 2, previewRadius * 2, previewRadius, 1, 0xFF4A4A5A);

        // Format hex WITHOUT the '#' symbol (e.g. FFC5D3DD)
        String hex = editingHex ? hexInput : String.format("%08X", getArgb() & 0xFFFFFFFF);
        int hw = FontRenderUtility.getStringWidth(hex);
        int hexX = previewCX - hw / 2;
        int hexY = my + 82;
        FontRenderUtility.drawString(graphics, hex, hexX, hexY, editingHex ? 0xFF5599FF : 0xFFAAAAAA, false);

        if (editingHex) {
            graphics.fill(hexX - 1, hexY + FontRenderUtility.getFontHeight() + 1,
                          hexX + hw + 1, hexY + FontRenderUtility.getFontHeight() + 3, 0xFF5599FF);
        }

        // Recent colors title & presets drawing
        int sectionX = mx + 15;
        int recentY = my + 200;
        FontRenderUtility.drawString(graphics, "Recent", sectionX, recentY, 0xFF75758A, false);

        int recentSwatchY = recentY + 12;
        for (int i = 0; i < Math.min(recentColors.size(), 10); i++) {
            int col = i % 10;
            int row = i / 10;
            int px = sectionX + (col * (swatchSize + swatchGap));
            int py = recentSwatchY + (row * (swatchSize + swatchGap));
            boolean hovered = mouseX >= px && mouseX <= px + swatchSize && mouseY >= py && mouseY <= py + swatchSize;
            graphics.fill(px, py, px + swatchSize, py + swatchSize, recentColors.get(i));
            graphics.fill(px, py, px + 1, py + swatchSize, hovered ? 0x99FFFFFF : 0xFF2A2A3A);
            graphics.fill(px + swatchSize - 1, py, px + swatchSize, py + swatchSize, hovered ? 0x99FFFFFF : 0xFF2A2A3A);
            graphics.fill(px, py, px + swatchSize, py + 1, hovered ? 0x99FFFFFF : 0xFF2A2A3A);
            graphics.fill(px, py + swatchSize - 1, px + swatchSize, py + swatchSize, hovered ? 0x99FFFFFF : 0xFF2A2A3A);
        }

        int presetY = my + 232;
        FontRenderUtility.drawString(graphics, "Presets", sectionX, presetY, 0xFF75758A, false);

        int[] presets = {
            0xFFFFFFFF, 0xFFC0C0C0, 0xFF808080, 0xFF000000,
            0xFFFF5555, 0xFFFF8800, 0xFFFFAA00, 0xFFFFFF55,
            0xFF55FF55, 0xFF00CC88, 0xFF5555FF, 0xFF8844FF,
            0xFF55FFFF, 0xFF00AAAA, 0xFFFF55FF, 0xFFFF4488
        };

        int presetSwatchY = presetY + 12;
        for (int i = 0; i < presets.length; i++) {
            int col = i % 8;
            int row = i / 8;
            int px = sectionX + (col * (swatchSize + swatchGap));
            int py = presetSwatchY + (row * (swatchSize + swatchGap));
            boolean hovered = mouseX >= px && mouseX <= px + swatchSize && mouseY >= py && mouseY <= py + swatchSize;
            graphics.fill(px, py, px + swatchSize, py + swatchSize, presets[i]);
            int borderColor = hovered ? 0xFFFFFFFF : 0xFF2A2A3A;
            graphics.fill(px - 1, py - 1, px + swatchSize + 1, py, borderColor);
            graphics.fill(px - 1, py + swatchSize, px + swatchSize + 1, py + swatchSize + 1, borderColor);
            graphics.fill(px - 1, py - 1, px, py + swatchSize + 1, borderColor);
            graphics.fill(px + swatchSize, py - 1, px + swatchSize + 1, py + swatchSize + 1, borderColor);
        }

        // Apply/Cancel image buttons (disable.png and enable.png)
        int iconSize = 16;
        int btnY = my + modalHeight - 24;
        int cancelX = mx + 15;
        int applyX = mx + modalWidth - iconSize - 15;

        boolean cancelHovered = mouseX >= cancelX - 2 && mouseX <= cancelX + iconSize + 2 && mouseY >= btnY - 2 && mouseY <= btnY + iconSize + 2;
        boolean applyHovered = mouseX >= applyX - 2 && mouseX <= applyX + iconSize + 2 && mouseY >= btnY - 2 && mouseY <= btnY + iconSize + 2;

        // Cancel/Disable texture button (soft white highlight)
        int cancelCol = cancelHovered ? 0xDDFFFFFF : 0x77FFFFFF;
        graphics.pose().pushMatrix();
        graphics.pose().translate(cancelX, btnY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TextureLoader.DISABLE, 0, 0, 0f, 0f, iconSize, iconSize, iconSize, iconSize, cancelCol);
        graphics.pose().popMatrix();

        // Apply/Enable texture button (soft white highlight)
        int applyCol = applyHovered ? 0xDDFFFFFF : 0x77FFFFFF;
        graphics.pose().pushMatrix();
        graphics.pose().translate(applyX, btnY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TextureLoader.ENABLE, 0, 0, 0f, 0f, iconSize, iconSize, iconSize, iconSize, applyCol);
        graphics.pose().popMatrix();

        // ClickGUI Style Switcher for ThemeSync
        int labelW = FontRenderUtility.getStringWidth("ThemeSync");
        int syncLabelX = mx + 185 - labelW / 2;
        int syncLabelY = my + 105;
        FontRenderUtility.drawString(graphics, "ThemeSync", syncLabelX, syncLabelY, 0xFF7A7A8A, false);

        int swW = 28;
        int swH = 14;
        int swX = mx + 175 - swW / 2;
        int swY = my + 120;

        int trackColor = parameter.isThemeSync() ? activeColor : 0xFF2A2A3A;
        graphics.pose().pushMatrix();
        graphics.pose().translate(swX, swY);
        Render2DEngine.drawRound(graphics, 0, 0, swW, swH, swH / 2, trackColor);
        graphics.pose().popMatrix();

        float knobSize = 12f;
        float knobRange = swW - knobSize - 2;
        float knobDrawX = swX + 1f + (parameter.isThemeSync() ? 1f : 0f) * knobRange;
        float knobDrawY = swY + 1f;

        // Use high-quality smooth circle with dynamic anti-aliasing for the switcher knob
        int cx = Math.round(knobDrawX + knobSize / 2f);
        int cy = Math.round(knobDrawY + knobSize / 2f);
        Render2DEngine.fillCircle(graphics, cx, cy, (int)(knobSize / 2), 0xFFFFFFFF);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int mxLeft = (screenWidth - modalWidth) / 2;
        int myTop = (screenHeight - modalHeight) / 2;

        if (mx < mxLeft || mx > mxLeft + modalWidth || my < myTop || my > myTop + modalHeight) {
            editingHex = false;
            close();
            return true;
        }

        int svX = mxLeft + 15;
        int svY = myTop + 35;
        if (mx >= svX && mx < svX + svSize && my >= svY && my < svY + svSize) {
            draggingSV = true;
            saturation = (mx - svX) / (float) svSize;
            value = 1.0f - (my - svY) / (float) svSize;
            parameter.setThemeSync(false);
            parameter.setValue(getArgb());
            return true;
        }

        int hueX = mxLeft + 15;
        int hueY = myTop + 165;
        if (mx >= hueX && mx < hueX + svSize && my >= hueY && my < hueY + sliderHeight) {
            draggingHue = true;
            hue = (mx - hueX) / (float) svSize;
            parameter.setThemeSync(false);
            parameter.setValue(getArgb());
            return true;
        }

        int alphaX = mxLeft + 15;
        int alphaY = myTop + 183;
        if (mx >= alphaX && mx < alphaX + svSize && my >= alphaY && my < alphaY + sliderHeight) {
            draggingAlpha = true;
            alpha = (mx - alphaX) / (float) svSize;
            parameter.setThemeSync(false);
            parameter.setValue(getArgb());
            return true;
        }

        int previewCX = mxLeft + 175;
        String hex = editingHex ? hexInput : String.format("%08X", getArgb() & 0xFFFFFFFF);
        int hw = FontRenderUtility.getStringWidth(hex);
        int hexX = previewCX - hw / 2;
        int hexY = myTop + 82;
        int fh = FontRenderUtility.getFontHeight();
        if (mx >= hexX - 2 && mx <= hexX + hw + 2 && my >= hexY - 2 && my <= hexY + fh + 4) {
            editingHex = !editingHex;
            hexInput = String.format("%08X", getArgb() & 0xFFFFFFFF);
            if (!editingHex) {
                applyHexInput();
            }
            playClickSound();
            return true;
        }

        int swW = 28;
        int swH = 14;
        int swX = mxLeft + 175 - swW / 2;
        int swY = myTop + 120;
        if (mx >= swX && mx <= swX + swW && my >= swY && my <= swY + swH) {
            parameter.setThemeSync(!parameter.isThemeSync());
            if (parameter.isThemeSync()) {
                setFromArgb(ravex.gui.clickgui.ColorUtility.getActiveColor());
                parameter.setValue(ravex.gui.clickgui.ColorUtility.getActiveColor());
            }
            playClickSound();
            return true;
        }

        int sectionX = mxLeft + 15;
        int recentY = myTop + 200;
        int recentSwatchY = recentY + 12;
        for (int i = 0; i < Math.min(recentColors.size(), 10); i++) {
            int col = i % 10;
            int row = i / 10;
            int px = sectionX + (col * (swatchSize + swatchGap));
            int py = recentSwatchY + (row * (swatchSize + swatchGap));
            if (mx >= px && mx <= px + swatchSize && my >= py && my <= py + swatchSize) {
                setFromArgb(recentColors.get(i));
                parameter.setThemeSync(false);
                parameter.setValue(recentColors.get(i));
                editingHex = false;
                playClickSound();
                return true;
            }
        }

        int presetY = myTop + 232;
        int[] presets = {
            0xFFFFFFFF, 0xFFC0C0C0, 0xFF808080, 0xFF000000,
            0xFFFF5555, 0xFFFF8800, 0xFFFFAA00, 0xFFFFFF55,
            0xFF55FF55, 0xFF00CC88, 0xFF5555FF, 0xFF8844FF,
            0xFF55FFFF, 0xFF00AAAA, 0xFFFF55FF, 0xFFFF4488
        };
        int presetSwatchY = presetY + 12;
        for (int i = 0; i < presets.length; i++) {
            int col = i % 8;
            int row = i / 8;
            int px = sectionX + (col * (swatchSize + swatchGap));
            int py = presetSwatchY + (row * (swatchSize + swatchGap));
            if (mx >= px && mx <= px + swatchSize && my >= py && my <= py + swatchSize) {
                setFromArgb(presets[i]);
                parameter.setThemeSync(false);
                parameter.setValue(presets[i]);
                editingHex = false;
                playClickSound();
                return true;
            }
        }

        int iconSize = 16;
        int btnY = myTop + modalHeight - 24;
        int cancelX = mxLeft + 15;
        int applyX = mxLeft + modalWidth - iconSize - 15;

        // Cancel / Disable icon click
        if (mx >= cancelX - 2 && mx <= cancelX + iconSize + 2 && my >= btnY - 2 && my <= btnY + iconSize + 2) {
            parameter.setValue(parameter.getValue());
            editingHex = false;
            close();
            playClickSound();
            return true;
        }

        // Apply / Enable icon click
        if (mx >= applyX - 2 && mx <= applyX + iconSize + 2 && my >= btnY - 2 && my <= btnY + iconSize + 2) {
            editingHex = false;
            addRecentColor(getArgb());
            close();
            playClickSound();
            return true;
        }

        return true;
    }

    public boolean keyPressed(int key) {
        if (editingHex) {
            if (key == GLFW.GLFW_KEY_ENTER) {
                applyHexInput();
                editingHex = false;
                playClickSound();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                editingHex = false;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE && !hexInput.isEmpty()) {
                hexInput = hexInput.substring(0, hexInput.length() - 1);
                return true;
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
            addRecentColor(getArgb());
            close();
            playClickSound();
            return true;
        }
        return true;
    }

    public boolean charTyped(char codePoint) {
        if (editingHex && hexInput.length() < 8) {
            String hexChars = "0123456789abcdefABCDEF";
            if (hexChars.indexOf(codePoint) >= 0) {
                hexInput += Character.toUpperCase(codePoint);
                return true;
            }
        }
        return true;
    }

    private void applyHexInput() {
        if (hexInput.length() != 8) return;
        try {
            int argb = (int) Long.parseLong(hexInput, 16);
            setFromArgb(argb);
            parameter.setThemeSync(false);
            parameter.setValue(argb);
        } catch (NumberFormatException ignored) {}
    }

    private void close() {
        open = false;
        ClickGUI.activeColorParameter = null;
        ClickGUI.activeColorPalette = null;
        if (onClose != null) onClose.run();
    }

    private void playClickSound() {
    }
}
