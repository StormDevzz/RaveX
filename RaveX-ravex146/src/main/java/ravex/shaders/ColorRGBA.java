package ravex.shaders;

public final class ColorRGBA {
    public float r, g, b, a;
    public ColorRGBA() {}
    public ColorRGBA(float r, float g, float b, float a) { this.r = r; this.g = g; this.b = b; this.a = a; }

    public static ColorRGBA fromInt(int argb) {
        return new ColorRGBA(
            ((argb >> 16) & 0xff) / 255f,
            ((argb >> 8) & 0xff) / 255f,
            (argb & 0xff) / 255f,
            ((argb >> 24) & 0xff) / 255f
        );
    }
    public int toInt() {
        return ((int)(a * 255) << 24) | ((int)(r * 255) << 16)
             | ((int)(g * 255) << 8) | (int)(b * 255);
    }
}
