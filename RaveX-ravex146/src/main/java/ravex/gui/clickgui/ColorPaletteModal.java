package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import ravex.parameter.ColorParameter;
import ravex.utility.render.FontRenderUtility;
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

    private final int modalWidth = 200;
    private final int modalHeight = 305;

    private final int svSize = 120;
    private final int sliderHeight = 10;
    private final int swatchSize = 14;
    private final int swatchGap = 4;

    public ColorPaletteModal(ColorParameter parameter) {
        this.parameter = parameter;
        setFromArgb(parameter.getValue());
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
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float)Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (t * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 1:
                    r = (int) (q * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 2:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (t * 255.0f + 0.5f);
                    break;
                case 3:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (q * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 4:
                    r = (int) (t * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 5:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (q * 255.0f + 0.5f);
                    break;
            }
        }
        return 0xff000000 | (r << 16) | (g << 8) | b;
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

        graphics.fill(0, 0, screenWidth, screenHeight, 0xAA07070B);

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
            parameter.setValue(getArgb());
        }

        int hueX = mx + 15;
        int hueY = my + 165;
        int hueW = svSize;
        if (draggingHue) {
            float relX = Math.max(0, Math.min(hueW, mouseX - hueX)) / (float) hueW;
            hue = relX;
            parameter.setValue(getArgb());
        }

        int alphaX = mx + 15;
        int alphaY = my + 183;
        int alphaW = svSize;
        if (draggingAlpha) {
            float relX = Math.max(0, Math.min(alphaW, mouseX - alphaX)) / (float) alphaW;
            alpha = relX;
            parameter.setValue(getArgb());
        }

        graphics.fill(mx, my, mx + modalWidth, my + modalHeight, 0xF50D0D14);

        graphics.fill(mx + 1, my + 1, mx + modalWidth - 1, my + 2, 0xFF1C1C2A);
        graphics.fill(mx + 1, my + modalHeight - 2, mx + modalWidth - 1, my + modalHeight - 1, 0xFF1C1C2A);
        graphics.fill(mx + 1, my + 1, mx + 2, my + modalHeight - 1, 0xFF1C1C2A);
        graphics.fill(mx + modalWidth - 2, my + 1, mx + modalWidth - 1, my + modalHeight - 1, 0xFF1C1C2A);

        int activeColor = getArgb() | 0xFF000000;
        graphics.fill(mx - 1, my - 1, mx + modalWidth + 1, my, activeColor);
        graphics.fill(mx - 1, my + modalHeight, mx + modalWidth + 1, my + modalHeight + 1, activeColor);
        graphics.fill(mx - 1, my - 1, mx, my + modalHeight + 1, activeColor);
        graphics.fill(mx + modalWidth, my - 1, mx + modalWidth + 1, my + modalHeight + 1, activeColor);

        FontRenderUtility.drawString(graphics, parameter.getName() + " Palette", mx + 12, my + 10, 0xFFE5E5F0, false);

        graphics.fill(mx + 10, my + 23, mx + modalWidth - 10, my + 24, 0xFF252535);

        int step = 4;
        for (int py = 0; py < svSize; py += step) {
            float v = 1.0f - py / (float) svSize;
            for (int px = 0; px < svSize; px += step) {
                float s = px / (float) svSize;
                int cellColor = hsbToRgb(hue, s, v);
                graphics.fill(svX + px, svY + py, svX + px + step, svY + py + step, cellColor);
            }
        }

        graphics.fill(svX - 1, svY - 1, svX + svSize + 1, svY, 0xFF353545);
        graphics.fill(svX - 1, svY + svSize, svX + svSize + 1, svY + svSize + 1, 0xFF353545);
        graphics.fill(svX - 1, svY - 1, svX, svY + svSize + 1, 0xFF353545);
        graphics.fill(svX + svSize, svY - 1, svX + svSize + 1, svY + svSize + 1, 0xFF353545);

        int curX = svX + (int)(saturation * svSize);
        int curY = svY + (int)((1.0f - value) * svSize);

        graphics.fill(curX - 4, curY - 4, curX + 5, curY - 3, 0xFF000000);
        graphics.fill(curX - 4, curY + 4, curX + 5, curY + 5, 0xFF000000);
        graphics.fill(curX - 4, curY - 3, curX - 3, curY + 4, 0xFF000000);
        graphics.fill(curX + 4, curY - 3, curX + 5, curY + 4, 0xFF000000);

        graphics.fill(curX - 3, curY - 3, curX + 4, curY - 2, 0xFFFFFFFF);
        graphics.fill(curX - 3, curY + 3, curX + 4, curY + 4, 0xFFFFFFFF);
        graphics.fill(curX - 3, curY - 2, curX - 2, curY + 3, 0xFFFFFFFF);
        graphics.fill(curX + 3, curY - 2, curX + 4, curY + 3, 0xFFFFFFFF);

        for (int px = 0; px < hueW; px += step) {
            float h = px / (float) hueW;
            int hColor = hsbToRgb(h, 1.0f, 1.0f);
            graphics.fill(hueX + px, hueY, hueX + px + step, hueY + sliderHeight, hColor);
        }

        graphics.fill(hueX - 1, hueY - 1, hueX + hueW + 1, hueY, 0xFF353545);
        graphics.fill(hueX - 1, hueY + sliderHeight, hueX + hueW + 1, hueY + sliderHeight + 1, 0xFF353545);
        graphics.fill(hueX - 1, hueY - 1, hueX, hueY + sliderHeight + 1, 0xFF353545);
        graphics.fill(hueX + hueW, hueY - 1, hueX + hueW + 1, hueY + sliderHeight + 1, 0xFF353545);

        int hkX = hueX + (int)(hue * hueW);
        graphics.fill(hkX - 1, hueY - 2, hkX + 2, hueY + sliderHeight + 2, 0xFFFFFFFF);
        graphics.fill(hkX - 2, hueY - 2, hkX - 1, hueY + sliderHeight + 2, 0xFF000000);
        graphics.fill(hkX + 2, hueY - 2, hkX + 3, hueY + sliderHeight + 2, 0xFF000000);

        int checkSize = 4;
        for (int px = 0; px < alphaW; px += checkSize) {
            for (int py = 0; py < sliderHeight; py += checkSize) {
                boolean light = ((px / checkSize + py / checkSize) % 2 == 0);
                int chk = light ? 0xFFAAAAAA : 0xFF555555;
                graphics.fill(alphaX + px, alphaY + py,
                              alphaX + Math.min(px + checkSize, alphaW),
                              alphaY + Math.min(py + checkSize, sliderHeight), chk);
            }
        }

        int currentRgb = hsbToRgb(hue, saturation, value) & 0x00FFFFFF;
        graphics.fillGradient(alphaX, alphaY, alphaX + alphaW, alphaY + sliderHeight,
                               currentRgb, (0xFF << 24) | currentRgb);

        graphics.fill(alphaX - 1, alphaY - 1, alphaX + alphaW + 1, alphaY, 0xFF353545);
        graphics.fill(alphaX - 1, alphaY + sliderHeight, alphaX + alphaW + 1, alphaY + sliderHeight + 1, 0xFF353545);
        graphics.fill(alphaX - 1, alphaY - 1, alphaX, alphaY + sliderHeight + 1, 0xFF353545);
        graphics.fill(alphaX + alphaW, alphaY - 1, alphaX + alphaW + 1, alphaY + sliderHeight + 1, 0xFF353545);

        int akX = alphaX + (int)(alpha * alphaW);
        graphics.fill(akX - 1, alphaY - 2, akX + 2, alphaY + sliderHeight + 2, 0xFFFFFFFF);
        graphics.fill(akX - 2, alphaY - 2, akX - 1, alphaY + sliderHeight + 2, 0xFF000000);
        graphics.fill(akX + 2, alphaY - 2, akX + 3, alphaY + sliderHeight + 2, 0xFF000000);

        int previewX = mx + 145;
        int previewY = my + 35;
        int previewW = 40;
        int previewH = 40;

        for (int px = 0; px < previewW; px += 8) {
            for (int py = 0; py < previewH; py += 8) {
                boolean light = ((px / 8 + py / 8) % 2 == 0);
                int chk = light ? 0xFF999999 : 0xFF444444;
                graphics.fill(previewX + px, previewY + py, previewX + px + 8, previewY + py + 8, chk);
            }
        }

        graphics.fill(previewX, previewY, previewX + previewW, previewY + previewH, getArgb());

        graphics.fill(previewX - 1, previewY - 1, previewX + previewW + 1, previewY, 0xFF4A4A5A);
        graphics.fill(previewX - 1, previewY + previewH, previewX + previewW + 1, previewY + previewH + 1, 0xFF4A4A5A);
        graphics.fill(previewX - 1, previewY - 1, previewX, previewY + previewH + 1, 0xFF4A4A5A);
        graphics.fill(previewX + previewW, previewY - 1, previewX + previewW + 1, previewY + previewH + 1, 0xFF4A4A5A);

        String hex = editingHex ? hexInput : String.format("#%08X", getArgb());
        int hw = FontRenderUtility.getStringWidth(hex);
        int hexX = previewX + (previewW - hw) / 2;
        int hexY = previewY + previewH + 6;
        FontRenderUtility.drawString(graphics, hex, hexX, hexY, editingHex ? 0xFF5599FF : 0xFFAAAAAA, false);

        if (editingHex) {
            graphics.fill(hexX - 1, hexY + FontRenderUtility.getFontHeight() + 1,
                          hexX + hw + 1, hexY + FontRenderUtility.getFontHeight() + 3, 0xFF5599FF);
        }

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

        int btnY = my + modalHeight - 24;
        int btnW = 50;
        int btnH = 14;
        int cancelX = mx + 15;
        int applyX = mx + modalWidth - btnW - 15;

        boolean cancelHovered = mouseX >= cancelX && mouseX <= cancelX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        boolean applyHovered = mouseX >= applyX && mouseX <= applyX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

        int cancelBg = cancelHovered ? 0x44222233 : 0xFF14141E;
        int cancelBorder = cancelHovered ? 0xFF555555 : 0xFF3E3E4E;
        graphics.fill(cancelX, btnY, cancelX + btnW, btnY + btnH, cancelBg);
        graphics.fill(cancelX - 1, btnY - 1, cancelX + btnW + 1, btnY, cancelBorder);
        graphics.fill(cancelX - 1, btnY + btnH, cancelX + btnW + 1, btnY + btnH + 1, cancelBorder);
        graphics.fill(cancelX - 1, btnY - 1, cancelX, btnY + btnH + 1, cancelBorder);
        graphics.fill(cancelX + btnW, btnY - 1, cancelX + btnW + 1, btnY + btnH + 1, cancelBorder);
        int cw = FontRenderUtility.getStringWidth("Cancel");
        FontRenderUtility.drawString(graphics, "Cancel", cancelX + (btnW - cw) / 2, btnY + 3, cancelHovered ? 0xFFD0D0E8 : 0xFF808090, false);

        int applyBg = applyHovered ? ((activeColor & 0x00FFFFFF) | 0x66000000) : 0xFF14141E;
        int applyBorder = applyHovered ? activeColor : 0xFF3E3E4E;
        graphics.fill(applyX, btnY, applyX + btnW, btnY + btnH, applyBg);
        graphics.fill(applyX - 1, btnY - 1, applyX + btnW + 1, btnY, applyBorder);
        graphics.fill(applyX - 1, btnY + btnH, applyX + btnW + 1, btnY + btnH + 1, applyBorder);
        graphics.fill(applyX - 1, btnY - 1, applyX, btnY + btnH + 1, applyBorder);
        graphics.fill(applyX + btnW, btnY - 1, applyX + btnW + 1, btnY + btnH + 1, applyBorder);
        int aw = FontRenderUtility.getStringWidth("Apply");
        FontRenderUtility.drawString(graphics, "Apply", applyX + (btnW - aw) / 2, btnY + 3, applyHovered ? 0xFFFFFFFF : 0xFFD0D0E8, false);

        int syncX = mx + 145;
        int syncY = my + 105;
        int syncW = 40;
        int syncH = 14;

        FontRenderUtility.drawString(graphics, "ThemeSync", syncX, syncY, 0xFF7A7A8A, false);

        boolean syncHovered = mouseX >= syncX && mouseX <= syncX + syncW && mouseY >= syncY + 10 && mouseY <= syncY + 10 + syncH;
        int syncBg = parameter.isThemeSync() ? activeColor : (syncHovered ? 0x44222233 : 0xFF14141E);
        int syncBorder = syncHovered ? (parameter.isThemeSync() ? 0xFFFFFFFF : 0xFF555555) : 0xFF3E3E4E;

        graphics.fill(syncX, syncY + 10, syncX + syncW, syncY + 10 + syncH, syncBg);
        graphics.fill(syncX - 1, syncY + 9, syncX + syncW + 1, syncY + 10, syncBorder);
        graphics.fill(syncX - 1, syncY + 10 + syncH, syncX + syncW + 1, syncY + 10 + syncH + 1, syncBorder);
        graphics.fill(syncX - 1, syncY + 9, syncX, syncY + 10 + syncH + 1, syncBorder);
        graphics.fill(syncX + syncW, syncY + 9, syncX + syncW + 1, syncY + 10 + syncH + 1, syncBorder);

        String syncText = parameter.isThemeSync() ? "ON" : "OFF";
        int stw = FontRenderUtility.getStringWidth(syncText);
        FontRenderUtility.drawString(graphics, syncText, syncX + (syncW - stw) / 2, syncY + 13, parameter.isThemeSync() ? 0xFFFFFFFF : 0xFF808090, false);
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

        int previewX = mxLeft + 145;
        int previewY = myTop + 35;
        int previewW = 40;
        int previewH = 40;
        String hex = editingHex ? hexInput : String.format("#%08X", getArgb());
        int hw = FontRenderUtility.getStringWidth(hex);
        int hexX = previewX + (previewW - hw) / 2;
        int hexY = previewY + previewH + 6;
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

        int syncX = mxLeft + 145;
        int syncY = myTop + 105;
        int syncW = 40;
        int syncH = 14;
        if (mx >= syncX && mx <= syncX + syncW && my >= syncY + 10 && my <= syncY + 10 + syncH) {
            parameter.setThemeSync(!parameter.isThemeSync());
            if (parameter.isThemeSync()) {
                setFromArgb(ColorUtility.getActiveColor());
                parameter.setValue(ColorUtility.getActiveColor());
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

        int btnY = myTop + modalHeight - 24;
        int btnW = 50;
        int btnH = 14;
        int cancelX = mxLeft + 15;
        int applyX = mxLeft + modalWidth - btnW - 15;

        if (mx >= cancelX && mx <= cancelX + btnW && my >= btnY && my <= btnY + btnH) {
            parameter.setValue(parameter.getValue());
            editingHex = false;
            close();
            playClickSound();
            return true;
        }

        if (mx >= applyX && mx <= applyX + btnW && my >= btnY && my <= btnY + btnH) {
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
        ClickGUI.activeColorParameter = null;
        ClickGUI.activeColorPalette = null;
    }

    private void playClickSound() {
    }
}
