package ravex.utility.render.animate;

public class FadeAnimation {
    private float prevAlpha = 0.0f;
    private float currentAlpha = 0.0f;

    public float update(boolean active, float speed) {
        prevAlpha = currentAlpha;
        float target = active ? 1.0f : 0.0f;
        currentAlpha += (target - currentAlpha) * speed;
        if (currentAlpha < 0.001f) currentAlpha = 0.0f;
        if (currentAlpha > 0.999f) currentAlpha = 1.0f;
        return currentAlpha;
    }

    public float getAlpha(float partialTicks) {
        return prevAlpha + (currentAlpha - prevAlpha) * partialTicks;
    }

    public float getAlpha() {
        return currentAlpha;
    }

    public void reset() {
        prevAlpha = 0.0f;
        currentAlpha = 0.0f;
    }
}
