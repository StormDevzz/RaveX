package ravex.utility.render.animate;

public class FadeAnimation {
    private float alpha = 0.0f;

    public float update(boolean active, float speed) {
        float target = active ? 1.0f : 0.0f;
        alpha += (target - alpha) * speed;
        if (alpha < 0.001f) alpha = 0.0f;
        if (alpha > 0.999f) alpha = 1.0f;
        return alpha;
    }

    public float getAlpha() {
        return alpha;
    }

    public void reset() {
        alpha = 0.0f;
    }
}
