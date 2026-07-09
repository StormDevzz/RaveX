package ravex.utility.render.animate;

public class EasingAnimation {
    private double value;

    public double update(boolean active, double speed) {
        double target = active ? 1.0 : 0.0;
        value += (target - value) * speed;
        if (value < 0.001) value = 0.0;
        if (value > 0.999) value = 1.0;
        return value;
    }

    public float updateFloat(boolean active, float speed) {
        return (float) update(active, (double) speed);
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void reset() {
        value = 0.0;
    }
}
