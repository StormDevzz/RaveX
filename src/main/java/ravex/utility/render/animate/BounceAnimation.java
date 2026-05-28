package ravex.utility.render.animate;

public class BounceAnimation {
    private double prevTime = 0.0;
    private double time = 0.0;

    public double update(double speed, double amplitude) {
        prevTime = time;
        time += speed;
        if (time > Math.PI * 2) {
            time -= Math.PI * 2;
            prevTime -= Math.PI * 2; // Keep in sync
        }
        return Math.sin(time) * amplitude;
    }

    public double getValue(float partialTicks, double amplitude) {
        double interpolatedTime = prevTime + (time - prevTime) * partialTicks;
        return Math.sin(interpolatedTime) * amplitude;
    }

    public double getValue(double amplitude) {
        return Math.sin(time) * amplitude;
    }

    public void reset() {
        time = 0.0;
        prevTime = 0.0;
    }
}
