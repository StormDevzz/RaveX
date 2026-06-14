package ravex.utility.notification;

public class Notification {
    public final String text;
    public final int color;
    public final int duration;
    public final long startTime;

    public Notification(String text, int color, int duration) {
        this.text = text;
        this.color = color;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - startTime > duration;
    }

    public float getAlpha() {
        long elapsed = System.currentTimeMillis() - startTime;
        long remaining = duration - elapsed;
        if (remaining <= 0) return 0f;
        if (remaining < 500) return remaining / 500f;
        return 1f;
    }
}
