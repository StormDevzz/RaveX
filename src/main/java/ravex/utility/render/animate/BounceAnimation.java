package ravex.utility.render.animate;

public class BounceAnimation {
    private double time = 0.0;

    public double update(double speed, double amplitude) {
        time += speed;
        if (time > Math.PI * 2) {
            time -= Math.PI * 2;
        }
        return Math.sin(time) * amplitude;
    }

    public void reset() {
        time = 0.0;
    }
}
