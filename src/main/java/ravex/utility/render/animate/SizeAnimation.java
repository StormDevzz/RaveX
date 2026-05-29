package ravex.utility.render.animate;

public class SizeAnimation {
    private double size = 0.0;

    public double update(boolean active, double speed) {
        double target = active ? 1.0 : 0.0;
        size += (target - size) * speed;
        if (size < 0.001) size = 0.0;
        if (size > 0.999) size = 1.0;
        return size;
    }

    public double getSize() {
        return size;
    }

    public void reset() {
        size = 0.0;
    }
}
