package ravex.utility.render;

import net.minecraft.client.gui.GuiGraphics;

import java.awt.*;

public class Render2DEngine {

    public static void drawRect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, color);
    }

    public static void drawGradientRect(GuiGraphics graphics, int x, int y, int width, int height, int startColor, int endColor) {
        graphics.fillGradient(x, y, x + width, y + height, startColor, endColor);
    }

    public static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int thickness, int color) {
        graphics.fill(x, y, x + width, y + thickness, color);
        graphics.fill(x, y + height - thickness, x + width, y + height, color);
        graphics.fill(x, y + thickness, x + thickness, y + height - thickness, color);
        graphics.fill(x + width - thickness, y + thickness, x + width, y + height - thickness, color);
    }

    public static void drawRound(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        drawRound(graphics, x, y, width, height, radius, r, g, b, a);
    }

    public static void drawRound(GuiGraphics graphics, int x, int y, int width, int height, int radius, int r, int g, int b, int a) {
        if (radius < 1) {
            graphics.fill(x, y, x + width, y + height, (a << 24) | (r << 16) | (g << 8) | b);
            return;
        }
        int rs = Math.min(radius, Math.min(width, height) / 2);
        graphics.fill(x + rs, y, x + width - rs, y + height, (a << 24) | (r << 16) | (g << 8) | b);
        graphics.fill(x, y + rs, x + rs, y + height - rs, (a << 24) | (r << 16) | (g << 8) | b);
        graphics.fill(x + width - rs, y + rs, x + width, y + height - rs, (a << 24) | (r << 16) | (g << 8) | b);
        int aa = Math.max(1, a / 3);
        int ab = (a << 24) | (r << 16) | (g << 8) | b;
        int aab = (aa << 24) | (r << 16) | (g << 8) | b;
        int aab2 = (Math.min(255, aa * 2) << 24) | (r << 16) | (g << 8) | b;
        drawCorner(graphics, x + rs, y + rs, false, false, rs, ab);
        drawCorner(graphics, x + width - rs, y + rs, true, false, rs, ab);
        drawCorner(graphics, x + rs, y + height - rs, false, true, rs, ab);
        drawCorner(graphics, x + width - rs, y + height - rs, true, true, rs, ab);
    }

    private static void drawCorner(GuiGraphics graphics, int cx, int cy, boolean right, boolean bottom, int radius, int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        for (int dy = 0; dy < radius; dy++) {
            int yPos = bottom ? cy + dy : cy - dy - 1;
            int xOff = (int) Math.sqrt(radius * radius - (radius - dy) * (radius - dy));
            int x1 = right ? cx + radius - xOff : cx - radius + xOff;
            int x2 = right ? cx + radius : cx - radius + xOff + 1;
            if (x2 > x1) {
                int alpha = (int) (a * Math.max(0, Math.min(1, (radius - dy) / (float) radius)));
                int col = (Math.min(255, alpha) << 24) | (r << 16) | (g << 8) | b;
                graphics.fill(x1, yPos, x2, yPos + 1, col);
            }
        }
    }

    public static void drawRoundGradient(GuiGraphics graphics, int x, int y, int width, int height, int radius, Color c1, Color c2, Color c3, Color c4) {
        if (width < 1 || height < 1) return;
        int rs = Math.min(radius, Math.min(width, height) / 2);
        int cx = x + width / 2;
        int cy = y + height / 2;

        for (int py = y; py < y + height; py++) {
            float tY = (py - y) / (float) height;
            Color leftColor = interpolateColorC(c1, c3, tY);
            Color rightColor = interpolateColorC(c2, c4, tY);
            for (int px = x; px < x + width; px++) {
                float tX = (px - x) / (float) width;
                Color col = interpolateColorC(leftColor, rightColor, tX);

                int dx = Math.min(px - x, x + width - 1 - px);
                int dy = Math.min(py - y, y + height - 1 - py);
                int distToEdge = Math.min(dx, dy);
                if (distToEdge < rs) {
                    float cornerProgress = 1f - (distToEdge / (float) rs);
                    float alphaMul = 1f - cornerProgress * cornerProgress;
                    int finalA = Math.max(0, Math.min(255, (int) (col.getAlpha() * alphaMul)));
                    if (finalA > 0) {
                        int argb = (finalA << 24) | (col.getRed() << 16) | (col.getGreen() << 8) | col.getBlue();
                        graphics.fill(px, py, px + 1, py + 1, argb);
                    }
                } else {
                    graphics.fill(px, py, px + 1, py + 1, col.getRGB());
                }
            }
        }
    }

    public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static Color injectAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    public static Color applyOpacity(Color color, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
    }

    public static int applyOpacity(int colorInt, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        Color c = new Color(colorInt);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (c.getAlpha() * opacity)).getRGB();
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        return new Color(
            interpolateInt(color1.getRed(), color2.getRed(), amount),
            interpolateInt(color1.getGreen(), color2.getGreen(), amount),
            interpolateInt(color1.getBlue(), color2.getBlue(), amount),
            interpolateInt(color1.getAlpha(), color2.getAlpha(), amount)
        );
    }

    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
        return (float) (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return (int) (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static Color darker(Color color, float factor) {
        return new Color(
            Math.max((int) (color.getRed() * factor), 0),
            Math.max((int) (color.getGreen() * factor), 0),
            Math.max((int) (color.getBlue() * factor), 0),
            color.getAlpha()
        );
    }

    public static Color skyRainbow(int speed, int index) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        return Color.getHSBColor((float) angle / 360f, 0.5f, 1.0f);
    }

    public static Color rainbow(int speed, int index, float saturation, float brightness, float opacity) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        float hue = angle / 360f;
        Color color = new Color(Color.HSBtoRGB(hue, saturation, brightness));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, (int) (opacity * 255))));
    }

    public static Color fade(int speed, int index, Color color, float alpha) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        Color colorHSB = new Color(Color.HSBtoRGB(hsb[0], hsb[1], angle / 360f));
        return new Color(colorHSB.getRed(), colorHSB.getGreen(), colorHSB.getBlue(), Math.max(0, Math.min(255, (int) (alpha * 255))));
    }

    public static Color twoColorEffect(Color cl1, Color cl2, double speed, double count) {
        int angle = (int) (((System.currentTimeMillis()) / speed + count) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return interpolateColorC(cl1, cl2, angle / 360f);
    }

    public static Color interpolateColorsBackAndForth(int speed, int index, Color start, Color end, boolean trueColor) {
        int angle = (int) (((System.currentTimeMillis()) / speed + index) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return interpolateColorC(start, end, angle / 360f);
    }

    public static Color getAnalogousColor(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return new Color(Color.HSBtoRGB(hsb[0] - 0.84f, hsb[1], hsb[2]));
    }

    public static float fastAnimation(float current, float target, float speed) {
        return current + (target - current) * Math.min(1, speed * 0.05f);
    }
}
