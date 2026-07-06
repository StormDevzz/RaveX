package ravex.shaders;

public final class ColorRGB {
    public float r, g, b;
    public ColorRGB() {}
    public ColorRGB(float r, float g, float b) { this.r = r; this.g = g; this.b = b; }
    public int toInt() {
        return 0xff000000 | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }
}
