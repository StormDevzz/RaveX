package ravex.utility.shaders;

public class ChromaEffect extends ShaderEffect {
    @Override
    public EffectType type() {
        return EffectType.CHROMA;
    }

    @Override
    public void configure(ShaderConfig config) {}

    @Override
    public EffectOutput process(EffectInput input) {
        EffectOutput out = new EffectOutput();
        float speed = 2.0f;
        float scale = 0.5f;
        float timeFactor = input.normalizedTime * speed;
        Vec3 pos = input.worldPos != null ? input.worldPos : input.vertex.position;
        float hue = (pos.x + pos.y + pos.z) * scale + timeFactor;
        hue = hue - (float) Math.floor(hue);

        float[] rgb = new float[3];
        hsbToRgb(hue, 0.85f, 1.0f, rgb);
        out.color = new ColorRGBA(rgb[0], rgb[1], rgb[2], 1.0f);
        out.alpha = 1.0f;
        out.glow = 0.0f;
        out.offset = new Vec3();
        return out;
    }

    private static void hsbToRgb(float hue, float saturation, float brightness, float[] rgb) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float)Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - saturation * (1.0f - f));
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
                    g = (int) (t * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 3:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (q * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 4:
                    r = (int) (t * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 5:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (q * 255.0f + 0.5f);
                    break;
            }
        }
        rgb[0] = r / 255.0f;
        rgb[1] = g / 255.0f;
        rgb[2] = b / 255.0f;
    }

    @Override
    public void reset() {}
}
