package ravex.utility.render;

public class ColorRenderUtility {
    public static int toRGBA(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int getRainbow(float seconds, float saturation, float brightness, long index) {
        float hue = ((System.currentTimeMillis() + index) % (int)(seconds * 1000)) / (seconds * 1000f);
        return java.awt.Color.HSBtoRGB(hue, saturation, brightness);
    }
}
