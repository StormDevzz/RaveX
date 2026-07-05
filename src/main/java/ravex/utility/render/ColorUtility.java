package ravex.utility.render;

import java.awt.Color;

public class ColorUtility {

    public static int toRGBA(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int toRGB(int r, int g, int b) {
        return toRGBA(r, g, b, 255);
    }

    public static int getRed(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int getGreen(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int getBlue(int color) {
        return color & 0xFF;
    }

    public static int getAlpha(int color) {
        return (color >> 24) & 0xFF;
    }

    public static int setAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    public static int applyAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (getAlpha(color) * alpha)));
        return setAlpha(color, a);
    }

    public static int interpolate(int color1, int color2, float t) {
        t = Math.max(0, Math.min(1, t));
        int a = (int) (getAlpha(color1) + (getAlpha(color2) - getAlpha(color1)) * t);
        int r = (int) (getRed(color1) + (getRed(color2) - getRed(color1)) * t);
        int g = (int) (getGreen(color1) + (getGreen(color2) - getGreen(color1)) * t);
        int b = (int) (getBlue(color1) + (getBlue(color2) - getBlue(color1)) * t);
        return toRGBA(r, g, b, a);
    }

    public static int interpolateHSB(int color1, int color2, float t) {
        t = Math.max(0, Math.min(1, t));
        float[] hsb1 = Color.RGBtoHSB(getRed(color1), getGreen(color1), getBlue(color1), null);
        float[] hsb2 = Color.RGBtoHSB(getRed(color2), getGreen(color2), getBlue(color2), null);
        float h = hsb1[0] + (hsb2[0] - hsb1[0]) * t;
        float s = hsb1[1] + (hsb2[1] - hsb1[1]) * t;
        float b = hsb1[2] + (hsb2[2] - hsb1[2]) * t;
        int rgb = Color.HSBtoRGB(h, s, b);
        int a = (int) (getAlpha(color1) + (getAlpha(color2) - getAlpha(color1)) * t);
        return toRGBA(getRed(rgb), getGreen(rgb), getBlue(rgb), a);
    }

    public static int rainbow(int speed, int index, float saturation, float brightness, float alpha) {
        int angle = (int) ((System.currentTimeMillis() / (double) speed + index) % 360);
        int rgb = Color.HSBtoRGB(angle / 360f, saturation, brightness);
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return toRGBA(getRed(rgb), getGreen(rgb), getBlue(rgb), a);
    }

    public static int fade(int speed, int index, int color, float alpha) {
        float[] hsb = Color.RGBtoHSB(getRed(color), getGreen(color), getBlue(color), null);
        int angle = (int) ((System.currentTimeMillis() / (double) speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], angle / 360f);
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return toRGBA(getRed(rgb), getGreen(rgb), getBlue(rgb), a);
    }

    public static int astolfo(int speed, int index, float saturation, float brightness, float alpha) {
        int angle = (int) ((System.currentTimeMillis() / (double) speed + index) % 360);
        float hue = angle / 360f;
        if (hue < 0.5f) hue = 0.5f - hue;
        else hue = 1f - (hue - 0.5f);
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return toRGBA(getRed(rgb), getGreen(rgb), getBlue(rgb), a);
    }

    public static int twoColor(int color1, int color2, double speed, double index) {
        int angle = (int) (((System.currentTimeMillis()) / speed + index) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return interpolate(color1, color2, angle / 360f);
    }

    public static int backAndForth(int speed, int index, int color1, int color2) {
        int angle = (int) (((System.currentTimeMillis()) / (double) speed + index) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return interpolate(color1, color2, angle / 360f);
    }

    public static int darker(int color, float factor) {
        return toRGBA(
            Math.max(0, (int) (getRed(color) * factor)),
            Math.max(0, (int) (getGreen(color) * factor)),
            Math.max(0, (int) (getBlue(color) * factor)),
            getAlpha(color)
        );
    }

    public static int brighter(int color, float factor) {
        return toRGBA(
            Math.min(255, (int) (getRed(color) + (255 - getRed(color)) * factor)),
            Math.min(255, (int) (getGreen(color) + (255 - getGreen(color)) * factor)),
            Math.min(255, (int) (getBlue(color) + (255 - getBlue(color)) * factor)),
            getAlpha(color)
        );
    }

    public static int withAlpha(int color, int alpha) {
        return setAlpha(color, alpha);
    }

    public static float[] toFloat(int color) {
        return new float[]{
            getRed(color) / 255f,
            getGreen(color) / 255f,
            getBlue(color) / 255f,
            getAlpha(color) / 255f
        };
    }

    public static int[] toIntArray(int color) {
        return new int[]{getRed(color), getGreen(color), getBlue(color), getAlpha(color)};
    }

    public static int gradient(float speed, int index, int... colors) {
        if (colors.length == 0) return 0xFFFFFFFF;
        if (colors.length == 1) return colors[0];
        float phase = (float) ((System.currentTimeMillis() + index * 100L) % (int) (speed * 1000)) / (speed * 1000);
        float segment = phase * (colors.length - 1);
        int seg = Math.min(colors.length - 2, (int) segment);
        float local = segment - seg;
        return interpolate(colors[seg], colors[seg + 1], local);
    }

    public static int[] gradientText(int color1, int color2, int length) {
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = interpolate(color1, color2, i / (float) (length - 1));
        }
        return result;
    }

    public static int[] rainbowText(int speed, int length, float saturation, float brightness, float alpha) {
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = rainbow(speed, i * 20, saturation, brightness, alpha);
        }
        return result;
    }

    public static int multiplyAlpha(int color, float multiplier) {
        int a = Math.max(0, Math.min(255, Math.round(getAlpha(color) * multiplier)));
        return setAlpha(color, a);
    }

    public static int inverse(int color) {
        return toRGBA(255 - getRed(color), 255 - getGreen(color), 255 - getBlue(color), getAlpha(color));
    }

    public static int average(int color1, int color2) {
        return interpolate(color1, color2, 0.5f);
    }

    public static boolean isDark(int color) {
        return (getRed(color) * 0.299 + getGreen(color) * 0.587 + getBlue(color) * 0.114) < 128;
    }

    public static int overlay(int base, int overlay) {
        float aO = getAlpha(overlay) / 255f;
        float aB = getAlpha(base) / 255f;
        float aOut = aO + aB * (1 - aO);
        if (aOut == 0) return 0;
        return toRGBA(
            (int) ((getRed(overlay) * aO + getRed(base) * aB * (1 - aO)) / aOut),
            (int) ((getGreen(overlay) * aO + getGreen(base) * aB * (1 - aO)) / aOut),
            (int) ((getBlue(overlay) * aO + getBlue(base) * aB * (1 - aO)) / aOut),
            (int) (aOut * 255)
        );
    }

    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;
    public static final int RED = 0xFFFF0000;
    public static final int GREEN = 0xFF00FF00;
    public static final int BLUE = 0xFF0000FF;
    public static final int YELLOW = 0xFFFFFF00;
    public static final int CYAN = 0xFF00FFFF;
    public static final int MAGENTA = 0xFFFF00FF;
    public static final int ORANGE = 0xFFFF8000;
    public static final int PURPLE = 0xFF8000FF;
    public static final int PINK = 0xFFFF69B4;
    public static final int GRAY = 0xFF808080;
    public static final int DARK_GRAY = 0xFF404040;
    public static final int LIGHT_GRAY = 0xFFC0C0C0;
    public static final int TRANSPARENT = 0x00000000;
}
