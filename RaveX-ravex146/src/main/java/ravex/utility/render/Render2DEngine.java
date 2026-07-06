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
        if (radius <= 0) {
            graphics.fill(x, y, x + width, y + height, color);
            return;
        }
        graphics.fill(x + radius, y, x + width - radius, y + height, color);
        graphics.fill(x, y + radius, x + radius, y + height - radius, color);
        graphics.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        if (radius == 3) {
            
            graphics.fill(x + 2, y, x + 3, y + 1, color);
            graphics.fill(x + 1, y + 1, x + 3, y + 2, color);
            graphics.fill(x, y + 2, x + 3, y + 3, color);
            
            graphics.fill(x + width - 3, y, x + width - 2, y + 1, color);
            graphics.fill(x + width - 3, y + 1, x + width - 1, y + 2, color);
            graphics.fill(x + width - 3, y + 2, x + width, y + 3, color);
            
            graphics.fill(x, y + height - 3, x + 3, y + height - 2, color);
            graphics.fill(x + 1, y + height - 2, x + 3, y + height - 1, color);
            graphics.fill(x + 2, y + height - 1, x + 3, y + height, color);
            
            graphics.fill(x + width - 3, y + height - 3, x + width, y + height - 2, color);
            graphics.fill(x + width - 3, y + height - 2, x + width - 1, y + height - 1, color);
            graphics.fill(x + width - 2, y + height - 1, x + width - 1, y + height, color);
        } else if (radius == 2) {
            
            graphics.fill(x + 1, y, x + 2, y + 1, color);
            graphics.fill(x, y + 1, x + 2, y + 2, color);
            
            graphics.fill(x + width - 2, y, x + width - 1, y + 1, color);
            graphics.fill(x + width - 2, y + 1, x + width, y + 2, color);
            
            graphics.fill(x, y + height - 2, x + 2, y + height - 1, color);
            graphics.fill(x + 1, y + height - 1, x + 2, y + height, color);
            
            graphics.fill(x + width - 2, y + height - 2, x + width, y + height - 1, color);
            graphics.fill(x + width - 2, y + height - 1, x + width - 1, y + height, color);
        } else {
            graphics.fill(x, y, x + width, y + height, color);
        }
    }

    public static void drawRound(GuiGraphics graphics, int x, int y, int width, int height, int radius, int r, int g, int b, int a) {
        int color = (a << 24) | (r << 16) | (g << 8) | b;
        drawRound(graphics, x, y, width, height, radius, color);
    }

    public static void drawSmoothRound(GuiGraphics graphics, float x, float y, float w, float h, float r, int color) {
        if (w <= 0f || h <= 0f) return;
        r = Math.max(0f, Math.min(r, Math.min(w, h) / 2.0f));
        if (r <= 0f) {
            graphics.fill((int)x, (int)y, (int)(x + w), (int)(y + h), color);
            return;
        }

        int aBase = (color >> 24) & 0xFF;
        int rCol = (color >> 16) & 0xFF;
        int gCol = (color >> 8) & 0xFF;
        int bCol = color & 0xFF;

        float cx = x + w / 2.0f;
        float cy = y + h / 2.0f;
        float innerHalfW = w / 2.0f - r;
        float innerHalfH = h / 2.0f - r;

        int minX = (int) Math.floor(x);
        int maxX = (int) Math.ceil(x + w);
        int minY = (int) Math.floor(y);
        int maxY = (int) Math.ceil(y + h);

        float smoothWidth = 1.35f;
        float halfSmooth = smoothWidth / 2.0f;

        for (int py = minY; py < maxY; py++) {
            float pCenterY = py + 0.5f;
            float dy = Math.abs(pCenterY - cy) - innerHalfH;

            int startX = -1;
            for (int px = minX; px < maxX; px++) {
                float pCenterX = px + 0.5f;
                float dx = Math.abs(pCenterX - cx) - innerHalfW;

                float dist;
                if (dx > 0 && dy > 0) {
                    dist = (float) Math.sqrt(dx * dx + dy * dy) - r;
                } else {
                    dist = Math.max(dx, dy) - r;
                }

                if (dist <= -halfSmooth) {
                    if (startX == -1) {
                        startX = px;
                    }
                } else {
                    if (startX != -1) {
                        graphics.fill(startX, py, px, py + 1, color);
                        startX = -1;
                    }

                    if (dist < halfSmooth) {
                        float t = (dist + halfSmooth) / smoothWidth;
                        float alphaFactor = 1.0f - t;
                        alphaFactor = alphaFactor * alphaFactor * (3.0f - 2.0f * alphaFactor);
                        int alpha = Math.round(aBase * alphaFactor);
                        if (alpha > 0) {
                            int edgeColor = (alpha << 24) | (rCol << 16) | (gCol << 8) | bCol;
                            graphics.fill(px, py, px + 1, py + 1, edgeColor);
                        }
                    }
                }
            }
            if (startX != -1) {
                graphics.fill(startX, py, maxX, py + 1, color);
            }
        }
    }

    public static void drawRoundGradient(GuiGraphics graphics, int x, int y, int width, int height, int radius, Color c1, Color c2, Color c3, Color c4) {
        graphics.fillGradient(x, y, x + width, y + height, c1.getRGB(), c3.getRGB());
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
