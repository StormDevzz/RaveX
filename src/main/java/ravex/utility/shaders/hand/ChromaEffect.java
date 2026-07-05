package ravex.utility.shaders.hand;

import ravex.utility.shaders.*;
import ravex.utility.shaders.nativec.ShaderNative;

public final class ChromaEffect extends ShaderEffect {
    private ShaderConfig config = new ShaderConfig();

    @Override
    public EffectType type() { return EffectType.CHROMA; }

    @Override
    public void configure(ShaderConfig cfg) { this.config = cfg; }

    @Override
    public EffectOutput process(EffectInput input) {
        EffectOutput out = new EffectOutput();
        float t = System.nanoTime() / 1e9f;
        float d = ShaderNative.vec3Length(input.localPos.x, input.localPos.y, input.localPos.z);
        float hue = (float)((t * 0.5f + d * 1.5f) % 1.0f);
        float[] rgb = new float[3];
        hsbToRgb(hue, 0.9f, 1f, rgb);
        out.color.r = rgb[0];
        out.color.g = rgb[1];
        out.color.b = rgb[2];
        out.color.a = 1f;
        out.alpha = 0.7f * input.intensity;
        out.glow = 0.04f;
        return out;
    }

    @Override
    public void reset() {}

    private static void hsbToRgb(float h, float s, float v, float[] out) {
        float r = v, g = v, b = v;
        if (s > 0) {
            int i = (int)(h * 6);
            float f = h * 6 - i;
            float p = v * (1 - s);
            float q = v * (1 - s * f);
            float t = v * (1 - s * (1 - f));
            switch (i % 6) {
                case 0: r=v; g=t; b=p; break;
                case 1: r=q; g=v; b=p; break;
                case 2: r=p; g=v; b=t; break;
                case 3: r=p; g=q; b=v; break;
                case 4: r=t; g=p; b=v; break;
                case 5: r=v; g=p; b=q; break;
            }
        }
        out[0] = r; out[1] = g; out[2] = b;
    }
}
