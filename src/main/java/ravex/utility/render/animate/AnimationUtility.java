package ravex.utility.render.animate;

import java.util.function.Function;

public class AnimationUtility {

    public enum Easing {
        LINEAR(t -> t),
        SINE_IN(t -> (float) (1 - Math.cos(t * Math.PI / 2))),
        SINE_OUT(t -> (float) Math.sin(t * Math.PI / 2)),
        SINE_BOTH(t -> (float) (-(Math.cos(Math.PI * t) - 1) / 2)),
        QUAD_IN(t -> t * t),
        QUAD_OUT(t -> t * (2 - t)),
        QUAD_BOTH(t -> t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t),
        CUBIC_IN(t -> t * t * t),
        CUBIC_OUT(t -> (float) (1 - Math.pow(1 - t, 3))),
        CUBIC_BOTH(t -> t < 0.5 ? 4 * t * t * t : (float) (1 - Math.pow(-2 * t + 2, 3) / 2)),
        QUART_IN(t -> t * t * t * t),
        QUART_OUT(t -> (float) (1 - Math.pow(1 - t, 4))),
        QUART_BOTH(t -> t < 0.5 ? 8 * t * t * t * t : (float) (1 - Math.pow(-2 * t + 2, 4) / 2)),
        QUINT_IN(t -> t * t * t * t * t),
        QUINT_OUT(t -> (float) (1 - Math.pow(1 - t, 5))),
        QUINT_BOTH(t -> t < 0.5 ? 16 * t * t * t * t * t : (float) (1 - Math.pow(-2 * t + 2, 5) / 2)),
        EXPO_IN(t -> (float) (t == 0 ? 0 : Math.pow(2, 10 * (t - 1)))),
        EXPO_OUT(t -> (float) (t == 1 ? 1 : 1 - Math.pow(2, -10 * t))),
        EXPO_BOTH(t -> {
            if (t == 0) return 0f;
            if (t == 1) return 1f;
            return (float) (t < 0.5 ? Math.pow(2, 20 * t - 10) / 2 : (2 - Math.pow(2, -20 * t + 10)) / 2);
        }),
        CIRC_IN(t -> (float) (1 - Math.sqrt(1 - t * t))),
        CIRC_OUT(t -> (float) Math.sqrt(1 - (t - 1) * (t - 1))),
        CIRC_BOTH(t -> (float) (t < 0.5 ? (1 - Math.sqrt(1 - Math.pow(2 * t, 2))) / 2
            : (Math.sqrt(1 - Math.pow(-2 * t + 2, 2)) + 1) / 2)),
        BACK_IN(t -> { float c = 1.70158f; return (c + 1) * t * t * t - c * t * t; }),
        BACK_OUT(t -> { float c = 1.70158f; return 1 + (c + 1) * (float) Math.pow(t - 1, 3) + c * (float) Math.pow(t - 1, 2); }),
        BACK_BOTH(t -> {
            float c = 1.70158f;
            float c2 = c * 1.525f;
            return (float) (t < 0.5
                ? (Math.pow(2 * t, 2) * ((c2 + 1) * 2 * t - c2)) / 2
                : (Math.pow(2 * t - 2, 2) * ((c2 + 1) * (t * 2 - 2) + c2) + 2) / 2);
        }),
        ELASTIC_IN(t -> {
            if (t == 0 || t == 1) return t;
            return (float) (-Math.pow(2, 10 * t - 10) * Math.sin((t * 10 - 10.75) * 2 * Math.PI / 3));
        }),
        ELASTIC_OUT(t -> {
            if (t == 0 || t == 1) return t;
            return (float) (Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * 2 * Math.PI / 3) + 1);
        }),
        ELASTIC_BOTH(t -> {
            if (t == 0 || t == 1) return t;
            return (float) (t < 0.5
                ? -(Math.pow(2, 20 * t - 10) * Math.sin((20 * t - 11.125) * 2 * Math.PI / 4.5)) / 2
                : Math.pow(2, -20 * t + 10) * Math.sin((20 * t - 11.125) * 2 * Math.PI / 4.5) / 2 + 1);
        }),
        BOUNCE_OUT(t -> {
            float n1 = 7.5625f, d1 = 2.75f;
            if (t < 1 / d1) return n1 * t * t;
            else if (t < 2 / d1) return n1 * (t -= 1.5f / d1) * t + 0.75f;
            else if (t < 2.5f / d1) return n1 * (t -= 2.25f / d1) * t + 0.9375f;
            else return n1 * (t -= 2.625f / d1) * t + 0.984375f;
        }),
        BOUNCE_IN(t -> 1 - BOUNCE_OUT.apply(1 - t)),
        BOUNCE_BOTH(t -> t < 0.5
            ? (1 - BOUNCE_OUT.apply(1 - 2 * t)) / 2
            : (1 + BOUNCE_OUT.apply(2 * t - 1)) / 2);

        private final Function<Float, Float> func;
        Easing(Function<Float, Float> func) { this.func = func; }
        public float apply(float t) { return func.apply(t); }
    }

    public static class FloatAnimation {
        private float from, to, value, duration, elapsed;
        private Easing easing = Easing.LINEAR;
        private boolean playing;

        public FloatAnimation(float from, float to, float duration) {
            this.from = from;
            this.to = to;
            this.duration = duration;
            this.value = from;
            this.elapsed = 0;
            this.playing = true;
        }

        public FloatAnimation easing(Easing easing) {
            this.easing = easing;
            return this;
        }

        public void update(float deltaMs) {
            if (!playing) return;
            elapsed = Math.min(duration, elapsed + deltaMs);
            float t = duration > 0 ? Math.min(1, elapsed / duration) : 1;
            value = from + (to - from) * easing.apply(t);
            if (elapsed >= duration) playing = false;
        }

        public float getValue() { return value; }
        public boolean isFinished() { return !playing; }
        public void play() { playing = true; }
        public void pause() { playing = false; }
        public void reset() { elapsed = 0; value = from; playing = true; }
        public void setTarget(float to) {
            if (Math.abs(this.to - to) > 0.001f) {
                this.from = value;
                this.to = to;
                this.elapsed = 0;
                this.playing = true;
            }
        }

        public float getProgress() { return duration > 0 ? elapsed / duration : 1; }
    }

    public static class SpringAnimation {
        private float value, target, velocity;
        private float stiffness = 80f;
        private float damping = 8f;
        private float mass = 1f;
        private float threshold = 0.01f;

        public SpringAnimation(float initial) {
            this.value = initial;
            this.target = initial;
            this.velocity = 0;
        }

        public SpringAnimation stiffness(float s) { this.stiffness = s; return this; }
        public SpringAnimation damping(float d) { this.damping = d; return this; }
        public SpringAnimation mass(float m) { this.mass = m; return this; }

        public void setTarget(float target) {
            if (target != this.target) {
                this.target = target;
                if (Math.abs(target - value) > threshold) {
                    velocity = 0;
                }
            }
        }

        public void update(float deltaMs) {
            float dt = deltaMs / 1000f;
            if (dt > 0.05f) dt = 0.05f;

            float force = -stiffness * (value - target);
            float dampingForce = -damping * velocity;
            float acceleration = (force + dampingForce) / mass;
            velocity += acceleration * dt;
            value += velocity * dt;

            if (Math.abs(value - target) < threshold && Math.abs(velocity) < threshold) {
                value = target;
                velocity = 0;
            }
        }

        public float getValue() { return value; }
        public boolean isSettled() { return Math.abs(value - target) < threshold && Math.abs(velocity) < threshold; }
    }

    public static class SmoothValue {
        private float current, target;
        private float speed;

        public SmoothValue(float initial, float speed) {
            this.current = initial;
            this.target = initial;
            this.speed = speed;
        }

        public void update(float deltaMs) {
            float factor = Math.min(1, speed * deltaMs / 50f);
            current += (target - current) * factor;
            if (Math.abs(target - current) < 0.01f) current = target;
        }

        public float getValue() { return current; }
        public void setTarget(float target) { this.target = target; }
        public void setInstant(float value) { this.current = value; this.target = value; }
        public void setSpeed(float speed) { this.speed = speed; }
        public boolean isDone() { return current == target; }
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.max(0, Math.min(1, t));
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * Math.max(0, Math.min(1, t));
    }

    public static float map(float value, float fromMin, float fromMax, float toMin, float toMax) {
        return toMin + (value - fromMin) / (fromMax - fromMin) * (toMax - toMin);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float approach(float current, float target, float maxDelta) {
        float diff = target - current;
        if (Math.abs(diff) <= maxDelta) return target;
        return current + Math.signum(diff) * maxDelta;
    }

    public static float deltaTime() {
        return MinecraftHelper.getDeltaMs();
    }

    public static float animate(float current, float target, float speed) {
        return current + (target - current) * Math.min(1, speed * 0.05f);
    }

    private static class MinecraftHelper {
        private static long lastTime = System.currentTimeMillis();
        static float getDeltaMs() {
            long now = System.currentTimeMillis();
            float delta = now - lastTime;
            lastTime = now;
            return Math.min(delta, 50);
        }
    }
}
