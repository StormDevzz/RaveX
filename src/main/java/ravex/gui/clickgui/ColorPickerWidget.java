package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/**
 * Inline HSV colour-picker + alpha slider widget.
 * Fits inside the module settings drawer of ClickGUI.
 * Call render() and mouseClicked() from ParameterElement.
 */
public class ColorPickerWidget {

    // Dimensions (fit inside a 90px wide settings panel)
    public static final int SV_SIZE  = 64;  // SV square size
    public static final int H_WIDTH  = 10;  // Hue strip width
    public static final int A_HEIGHT = 8;   // Alpha slider height
    public static final int TOTAL_W  = SV_SIZE + 4 + H_WIDTH;
    public static final int TOTAL_H  = SV_SIZE + 4 + A_HEIGHT;

    private float hue        = 0.0f;
    private float saturation = 1.0f;
    private float value      = 1.0f;
    private float alpha      = 1.0f;

    private boolean draggingSV    = false;
    private boolean draggingHue   = false;
    private boolean draggingAlpha = false;

    /** Load initial color from an ARGB integer. */
    public void setFromArgb(int argb) {
        alpha = ((argb >>> 24) & 0xFF) / 255.0f;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8)  & 0xFF;
        int b =  argb         & 0xFF;
        float[] hsv = java.awt.Color.RGBtoHSB(r, g, b, null);
        hue        = hsv[0];
        saturation = hsv[1];
        value      = hsv[2];
    }

    /** Get current colour as an ARGB integer. */
    public int getArgb() {
        int rgb = java.awt.Color.HSBtoRGB(hue, saturation, value) & 0x00FFFFFF;
        int a   = Math.round(alpha * 255) << 24;
        return a | rgb;
    }

    /**
     * Render the widget at (wx, wy). Returns nothing – callers must know total height.
     */
    public void render(GuiGraphics graphics, Font font, int wx, int wy, int mouseX, int mouseY) {
        // ── Update drag states ──────────────────────────────────────────────────
        long win = net.minecraft.client.Minecraft.getInstance().getWindow().handle();
        boolean lmb = org.lwjgl.glfw.GLFW.glfwGetMouseButton(win, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT)
                      == org.lwjgl.glfw.GLFW.GLFW_PRESS;

        if (!lmb) {
            draggingSV = draggingHue = draggingAlpha = false;
        }

        if (draggingSV) {
            float relX = Math.max(0, Math.min(SV_SIZE, mouseX - wx)) / (float) SV_SIZE;
            float relY = Math.max(0, Math.min(SV_SIZE, mouseY - wy)) / (float) SV_SIZE;
            saturation = relX;
            value      = 1.0f - relY;
        }
        if (draggingHue) {
            float relY = Math.max(0, Math.min(SV_SIZE, mouseY - wy)) / (float) SV_SIZE;
            hue = relY;
        }
        if (draggingAlpha) {
            int ax = wx;
            int aw = TOTAL_W;
            float relX = Math.max(0, Math.min(aw, mouseX - ax)) / (float) aw;
            alpha = relX;
        }

        // ── SV Square ──────────────────────────────────────────────────────────
        int pureHue = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f) | 0xFF000000;
        // Render pixel-by-pixel via fill() calls (cheap because SV_SIZE is small)
        int step = 2; // 2 px per cell for perf
        for (int py = 0; py < SV_SIZE; py += step) {
            float v = 1.0f - py / (float) SV_SIZE;
            for (int px = 0; px < SV_SIZE; px += step) {
                float s = px / (float) SV_SIZE;
                int argb = (java.awt.Color.HSBtoRGB(hue, s, v) & 0x00FFFFFF) | 0xFF000000;
                graphics.fill(wx + px, wy + py, wx + px + step, wy + py + step, argb);
            }
        }

        // SV cursor circle (crosshair)
        int curX = wx + (int)(saturation * SV_SIZE);
        int curY = wy + (int)((1.0f - value) * SV_SIZE);
        graphics.fill(curX - 2, curY,     curX + 3, curY + 1, 0xFFFFFFFF);
        graphics.fill(curX,     curY - 2, curX + 1, curY + 3, 0xFFFFFFFF);
        graphics.fill(curX - 1, curY - 1, curX + 2, curY + 2, 0xFF000000);

        // ── Hue Strip ──────────────────────────────────────────────────────────
        int hx = wx + SV_SIZE + 4;
        for (int py = 0; py < SV_SIZE; py += step) {
            float h = py / (float) SV_SIZE;
            int argb = (java.awt.Color.HSBtoRGB(h, 1.0f, 1.0f) & 0x00FFFFFF) | 0xFF000000;
            graphics.fill(hx, wy + py, hx + H_WIDTH, wy + py + step, argb);
        }
        // Hue indicator bar
        int hueY = wy + (int)(hue * SV_SIZE);
        graphics.fill(hx - 1, hueY, hx + H_WIDTH + 1, hueY + 2, 0xFFFFFFFF);

        // ── Alpha Slider ───────────────────────────────────────────────────────
        int ay = wy + SV_SIZE + 4;
        // Checkerboard background
        int checkSize = 4;
        for (int px = 0; px < TOTAL_W; px += checkSize) {
            for (int py2 = 0; py2 < A_HEIGHT; py2 += checkSize) {
                boolean light = ((px / checkSize + py2 / checkSize) % 2 == 0);
                int chk = light ? 0xFFAAAAAA : 0xFF555555;
                graphics.fill(wx + px, ay + py2,
                              wx + Math.min(px + checkSize, TOTAL_W),
                              ay + Math.min(py2 + checkSize, A_HEIGHT), chk);
            }
        }
        // Gradient overlay alpha
        int rgb = java.awt.Color.HSBtoRGB(hue, saturation, value) & 0x00FFFFFF;
        graphics.fillGradient(wx, ay, wx + TOTAL_W, ay + A_HEIGHT,
                              rgb, (0xFF << 24) | rgb);
        // Knob
        int kx = wx + (int)(alpha * TOTAL_W);
        graphics.fill(kx - 1, ay, kx + 2, ay + A_HEIGHT, 0xFFFFFFFF);

        // ── Colour preview swatch ──────────────────────────────────────────────
        int previewX = wx + TOTAL_W + 4;
        int previewSize = 10;
        graphics.fill(previewX, wy, previewX + previewSize, wy + previewSize, getArgb());
        graphics.fill(previewX - 1, wy - 1, previewX + previewSize + 1, wy, 0xFFFFFFFF);
        graphics.fill(previewX - 1, wy + previewSize, previewX + previewSize + 1, wy + previewSize + 1, 0xFFFFFFFF);
        graphics.fill(previewX - 1, wy - 1, previewX, wy + previewSize + 1, 0xFFFFFFFF);
        graphics.fill(previewX + previewSize, wy - 1, previewX + previewSize + 1, wy + previewSize + 1, 0xFFFFFFFF);
    }

    /** Returns true if a drag started (the caller should consume the event). */
    public boolean mouseClicked(double mouseX, double mouseY, int wx, int wy) {
        int mx = (int) mouseX, my = (int) mouseY;

        // SV square
        if (mx >= wx && mx < wx + SV_SIZE && my >= wy && my < wy + SV_SIZE) {
            draggingSV = true;
            saturation = (mx - wx) / (float) SV_SIZE;
            value      = 1.0f - (my - wy) / (float) SV_SIZE;
            return true;
        }
        // Hue strip
        int hx = wx + SV_SIZE + 4;
        if (mx >= hx && mx < hx + H_WIDTH && my >= wy && my < wy + SV_SIZE) {
            draggingHue = true;
            hue = (my - wy) / (float) SV_SIZE;
            return true;
        }
        // Alpha slider
        int ay = wy + SV_SIZE + 4;
        if (mx >= wx && mx < wx + TOTAL_W && my >= ay && my < ay + A_HEIGHT) {
            draggingAlpha = true;
            alpha = (mx - wx) / (float) TOTAL_W;
            return true;
        }
        return false;
    }

    public float getAlpha() { return alpha; }
    public float getHue()   { return hue; }
    public float getSat()   { return saturation; }
    public float getVal()   { return value; }
}
