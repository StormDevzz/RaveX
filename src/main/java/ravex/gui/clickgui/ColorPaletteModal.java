package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import ravex.parameter.ColorParameter;
import ravex.utility.render.FontRenderUtility;
import org.lwjgl.glfw.GLFW;

public class ColorPaletteModal {
    private final ColorParameter parameter;
    
    // Core coordinates
    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float value = 1.0f;
    private float alpha = 1.0f;
    
    // Dragging state
    private boolean draggingSV = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;

    // Dimensions
    private final int modalWidth = 200;
    private final int modalHeight = 245;
    
    private final int svSize = 120;
    private final int sliderHeight = 10;
    
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

    // High performance inline HSB to RGB converter to avoid heavy library allocations
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

    public void render(GuiGraphics graphics, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        // Modal coordinates (centered)
        int mx = (screenWidth - modalWidth) / 2;
        int my = (screenHeight - modalHeight) / 2;

        // Dark frosted-glass overlay across the whole screen
        graphics.fill(0, 0, screenWidth, screenHeight, 0xAA07070B);

        // Update drag inputs if left mouse button is pressed
        long win = Minecraft.getInstance().getWindow().handle();
        boolean lmb = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!lmb) {
            draggingSV = draggingHue = draggingAlpha = false;
        }

        // SV Dragging
        int svX = mx + 15;
        int svY = my + 35;
        if (draggingSV) {
            float relX = Math.max(0, Math.min(svSize, mouseX - svX)) / (float) svSize;
            float relY = Math.max(0, Math.min(svSize, mouseY - svY)) / (float) svSize;
            saturation = relX;
            value = 1.0f - relY;
            parameter.setValue(getArgb());
        }

        // Hue Dragging
        int hueX = mx + 15;
        int hueY = my + 165;
        int hueW = svSize;
        if (draggingHue) {
            float relX = Math.max(0, Math.min(hueW, mouseX - hueX)) / (float) hueW;
            hue = relX;
            parameter.setValue(getArgb());
        }

        // Alpha Dragging
        int alphaX = mx + 15;
        int alphaY = my + 183;
        int alphaW = svSize;
        if (draggingAlpha) {
            float relX = Math.max(0, Math.min(alphaW, mouseX - alphaX)) / (float) alphaW;
            alpha = relX;
            parameter.setValue(getArgb());
        }

        // Modal background (frost dark slate)
        graphics.fill(mx, my, mx + modalWidth, my + modalHeight, 0xF50D0D14);
        
        // Inner elegant border
        graphics.fill(mx + 1, my + 1, mx + modalWidth - 1, my + 2, 0xFF1C1C2A);
        graphics.fill(mx + 1, my + modalHeight - 2, mx + modalWidth - 1, my + modalHeight - 1, 0xFF1C1C2A);
        graphics.fill(mx + 1, my + 1, mx + 2, my + modalHeight - 1, 0xFF1C1C2A);
        graphics.fill(mx + modalWidth - 2, my + 1, mx + modalWidth - 1, my + modalHeight - 1, 0xFF1C1C2A);

        // Active color glowing outer neon border (1px offset)
        int activeColor = getArgb() | 0xFF000000;
        graphics.fill(mx - 1, my - 1, mx + modalWidth + 1, my, activeColor);
        graphics.fill(mx - 1, my + modalHeight, mx + modalWidth + 1, my + modalHeight + 1, activeColor);
        graphics.fill(mx - 1, my - 1, mx, my + modalHeight + 1, activeColor);
        graphics.fill(mx + modalWidth, my - 1, mx + modalWidth + 1, my + modalHeight + 1, activeColor);

        // Header Title
        FontRenderUtility.drawString(graphics, parameter.getName() + " Palette", mx + 12, my + 10, 0xFFE5E5F0, false);
        
        // Horizontal divider line
        graphics.fill(mx + 10, my + 23, mx + modalWidth - 10, my + 24, 0xFF252535);

        // SV Gradient Box rendered with step = 4 for maximum rendering smoothness and 75%+ FPS savings
        int step = 4;
        for (int py = 0; py < svSize; py += step) {
            float v = 1.0f - py / (float) svSize;
            for (int px = 0; px < svSize; px += step) {
                float s = px / (float) svSize;
                int cellColor = hsbToRgb(hue, s, v);
                graphics.fill(svX + px, svY + py, svX + px + step, svY + py + step, cellColor);
            }
        }
        
        // Border around SV gradient box
        graphics.fill(svX - 1, svY - 1, svX + svSize + 1, svY, 0xFF353545);
        graphics.fill(svX - 1, svY + svSize, svX + svSize + 1, svY + svSize + 1, 0xFF353545);
        graphics.fill(svX - 1, svY - 1, svX, svY + svSize + 1, 0xFF353545);
        graphics.fill(svX + svSize, svY - 1, svX + svSize + 1, svY + svSize + 1, 0xFF353545);

        // Draw modern Circular Ring SV Cursor
        int curX = svX + (int)(saturation * svSize);
        int curY = svY + (int)((1.0f - value) * svSize);
        // Outer dark circle outline
        graphics.fill(curX - 4, curY - 4, curX + 5, curY - 3, 0xFF000000);
        graphics.fill(curX - 4, curY + 4, curX + 5, curY + 5, 0xFF000000);
        graphics.fill(curX - 4, curY - 3, curX - 3, curY + 4, 0xFF000000);
        graphics.fill(curX + 4, curY - 3, curX + 5, curY + 4, 0xFF000000);
        // Inner white circle outline
        graphics.fill(curX - 3, curY - 3, curX + 4, curY - 2, 0xFFFFFFFF);
        graphics.fill(curX - 3, curY + 3, curX + 4, curY + 4, 0xFFFFFFFF);
        graphics.fill(curX - 3, curY - 2, curX - 2, curY + 3, 0xFFFFFFFF);
        graphics.fill(curX + 3, curY - 2, curX + 4, curY + 3, 0xFFFFFFFF);

        // Hue Slider (step = 4 for ultra-smooth rendering performance)
        for (int px = 0; px < hueW; px += step) {
            float h = px / (float) hueW;
            int hColor = hsbToRgb(h, 1.0f, 1.0f);
            graphics.fill(hueX + px, hueY, hueX + px + step, hueY + sliderHeight, hColor);
        }
        
        // Hue slider border
        graphics.fill(hueX - 1, hueY - 1, hueX + hueW + 1, hueY, 0xFF353545);
        graphics.fill(hueX - 1, hueY + sliderHeight, hueX + hueW + 1, hueY + sliderHeight + 1, 0xFF353545);
        graphics.fill(hueX - 1, hueY - 1, hueX, hueY + sliderHeight + 1, 0xFF353545);
        graphics.fill(hueX + hueW, hueY - 1, hueX + hueW + 1, hueY + sliderHeight + 1, 0xFF353545);

        // Hue Knob
        int hkX = hueX + (int)(hue * hueW);
        graphics.fill(hkX - 1, hueY - 2, hkX + 2, hueY + sliderHeight + 2, 0xFFFFFFFF);
        graphics.fill(hkX - 2, hueY - 2, hkX - 1, hueY + sliderHeight + 2, 0xFF000000);
        graphics.fill(hkX + 2, hueY - 2, hkX + 3, hueY + sliderHeight + 2, 0xFF000000);

        // Alpha Slider Checkerboard background
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
        
        // Alpha Gradient overlay
        int currentRgb = hsbToRgb(hue, saturation, value) & 0x00FFFFFF;
        graphics.fillGradient(alphaX, alphaY, alphaX + alphaW, alphaY + sliderHeight,
                               currentRgb, (0xFF << 24) | currentRgb);
                               
        // Alpha slider border
        graphics.fill(alphaX - 1, alphaY - 1, alphaX + alphaW + 1, alphaY, 0xFF353545);
        graphics.fill(alphaX - 1, alphaY + sliderHeight, alphaX + alphaW + 1, alphaY + sliderHeight + 1, 0xFF353545);
        graphics.fill(alphaX - 1, alphaY - 1, alphaX, alphaY + sliderHeight + 1, 0xFF353545);
        graphics.fill(alphaX + alphaW, alphaY - 1, alphaX + alphaW + 1, alphaY + sliderHeight + 1, 0xFF353545);

        // Alpha Knob
        int akX = alphaX + (int)(alpha * alphaW);
        graphics.fill(akX - 1, alphaY - 2, akX + 2, alphaY + sliderHeight + 2, 0xFFFFFFFF);
        graphics.fill(akX - 2, alphaY - 2, akX - 1, alphaY + sliderHeight + 2, 0xFF000000);
        graphics.fill(akX + 2, alphaY - 2, akX + 3, alphaY + sliderHeight + 2, 0xFF000000);

        // Right side - Color Previews & Swatches
        int previewX = mx + 145;
        int previewY = my + 35;
        int previewW = 40;
        int previewH = 40;

        // Big checkerboard preview
        for (int px = 0; px < previewW; px += 8) {
            for (int py = 0; py < previewH; py += 8) {
                boolean light = ((px / 8 + py / 8) % 2 == 0);
                int chk = light ? 0xFF999999 : 0xFF444444;
                graphics.fill(previewX + px, previewY + py, previewX + px + 8, previewY + py + 8, chk);
            }
        }
        
        // Actual Color Overlay
        graphics.fill(previewX, previewY, previewX + previewW, previewY + previewH, getArgb());
        
        // Preview border (modern dark border)
        graphics.fill(previewX - 1, previewY - 1, previewX + previewW + 1, previewY, 0xFF4A4A5A);
        graphics.fill(previewX - 1, previewY + previewH, previewX + previewW + 1, previewY + previewH + 1, 0xFF4A4A5A);
        graphics.fill(previewX - 1, previewY - 1, previewX, previewY + previewH + 1, 0xFF4A4A5A);
        graphics.fill(previewX + previewW, previewY - 1, previewX + previewW + 1, previewY + previewH + 1, 0xFF4A4A5A);

        // Hex Code text label
        String hex = String.format("#%08X", getArgb());
        int hw = FontRenderUtility.getStringWidth(hex);
        FontRenderUtility.drawString(graphics, hex, previewX + (previewW - hw) / 2, previewY + previewH + 6, 0xFFAAAAAA, false);

        // Curated Presets
        int presetY = my + 203;
        FontRenderUtility.drawString(graphics, "Presets", mx + 15, presetY - 10, 0xFF75758A, false);
        
        int[] presets = {
            0xFFFFFFFF, // White
            0xFFFF5555, // Light Red
            0xFF55FF55, // Light Green
            0xFF5555FF, // Light Blue
            0xFFFFAA00, // Gold / Orange
            0xFFFFFF55, // Yellow
            0xFF55FFFF, // Aqua
            0xFFFF55FF  // Pink
        };
        
        for (int i = 0; i < presets.length; i++) {
            int px = mx + 15 + (i * 22);
            boolean hovered = mouseX >= px && mouseX <= px + 16 && mouseY >= presetY && mouseY <= presetY + 16;
            
            graphics.fill(px, presetY, px + 16, presetY + 16, presets[i]);
            
            // Draw border (lights up white on hover!)
            int borderColor = hovered ? 0xFFFFFFFF : 0xFF2A2A3A;
            graphics.fill(px - 1, presetY - 1, px + 17, presetY, borderColor);
            graphics.fill(px - 1, presetY + 16, px + 17, presetY + 17, borderColor);
            graphics.fill(px - 1, presetY - 1, px, presetY + 17, borderColor);
            graphics.fill(px + 16, presetY - 1, px + 17, presetY + 17, borderColor);
        }

        // Apply Button
        int btnW = 50;
        int btnH = 14;
        int btnX = mx + modalWidth - btnW - 15;
        int btnY = my + modalHeight - btnH - 10;
        boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        
        // Dynamic active-colored button highlights when hovered!
        int btnBg = btnHovered ? ((activeColor & 0x00FFFFFF) | 0x66000000) : 0xFF14141E;
        int btnBorder = btnHovered ? activeColor : 0xFF3E3E4E;
        
        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnBg);
        graphics.fill(btnX - 1, btnY - 1, btnX + btnW + 1, btnY, btnBorder);
        graphics.fill(btnX - 1, btnY + btnH, btnX + btnW + 1, btnY + btnH + 1, btnBorder);
        graphics.fill(btnX - 1, btnY - 1, btnX, btnY + btnH + 1, btnBorder);
        graphics.fill(btnX + btnW, btnY - 1, btnX + btnW + 1, btnY + btnH + 1, btnBorder);
        
        int bw = FontRenderUtility.getStringWidth("Apply");
        FontRenderUtility.drawString(graphics, "Apply", btnX + (btnW - bw) / 2, btnY + 3, btnHovered ? 0xFFFFFFFF : 0xFFD0D0E8, false);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        // Centered coordinates
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int mxLeft = (screenWidth - modalWidth) / 2;
        int myTop = (screenHeight - modalHeight) / 2;

        // If clicked outside the modal bounds, close it
        if (mx < mxLeft || mx > mxLeft + modalWidth || my < myTop || my > myTop + modalHeight) {
            ClickGUI.activeColorParameter = null;
            ClickGUI.activeColorPalette = null;
            playClickSound();
            return true;
        }

        // SV Square click
        int svX = mxLeft + 15;
        int svY = myTop + 35;
        if (mx >= svX && mx < svX + svSize && my >= svY && my < svY + svSize) {
            draggingSV = true;
            saturation = (mx - svX) / (float) svSize;
            value = 1.0f - (my - svY) / (float) svSize;
            parameter.setValue(getArgb());
            return true;
        }

        // Hue Slider click
        int hueX = mxLeft + 15;
        int hueY = myTop + 165;
        if (mx >= hueX && mx < hueX + svSize && my >= hueY && my < hueY + sliderHeight) {
            draggingHue = true;
            hue = (mx - hueX) / (float) svSize;
            parameter.setValue(getArgb());
            return true;
        }

        // Alpha Slider click
        int alphaX = mxLeft + 15;
        int alphaY = myTop + 183;
        if (mx >= alphaX && mx < alphaX + svSize && my >= alphaY && my < alphaY + sliderHeight) {
            draggingAlpha = true;
            alpha = (mx - alphaX) / (float) svSize;
            parameter.setValue(getArgb());
            return true;
        }

        // Preset clicks
        int presetY = myTop + 203;
        int[] presets = {
            0xFFFFFFFF, 0xFFFF5555, 0xFF55FF55, 0xFF5555FF,
            0xFFFFAA00, 0xFFFFFF55, 0xFF55FFFF, 0xFFFF55FF
        };
        for (int i = 0; i < presets.length; i++) {
            int px = mxLeft + 15 + (i * 22);
            if (mx >= px && mx < px + 16 && my >= presetY && my < presetY + 16) {
                setFromArgb(presets[i]);
                parameter.setValue(presets[i]);
                playClickSound();
                return true;
            }
        }

        // Apply Button click
        int btnW = 50;
        int btnH = 14;
        int btnX = mxLeft + modalWidth - btnW - 15;
        int btnY = myTop + modalHeight - btnH - 10;
        if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
            ClickGUI.activeColorParameter = null;
            ClickGUI.activeColorPalette = null;
            playClickSound();
            return true;
        }

        return true; // Consume all clicks inside modal
    }

    public boolean keyPressed(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
            ClickGUI.activeColorParameter = null;
            ClickGUI.activeColorPalette = null;
            playClickSound();
            return true;
        }
        return true; // Block keyboard typing when modal is open
    }

    private void playClickSound() {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.25f, 1.4f);
        }
    }
}
