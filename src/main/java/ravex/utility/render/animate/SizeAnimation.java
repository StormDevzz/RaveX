package ravex.utility.render.animate;

public class SizeAnimation {
    private double prevSize = 0.0;
    private double currentSize = 0.0;

    public double update(boolean active, double speed) {
        prevSize = currentSize;
        double target = active ? 1.0 : 0.0;
        currentSize += (target - currentSize) * speed;
        if (currentSize < 0.001) currentSize = 0.0;
        if (currentSize > 0.999) currentSize = 1.0;
        return currentSize;
    }

    public double getSize(float partialTicks) {
        return prevSize + (currentSize - prevSize) * partialTicks;
    }

    public double getSize() {
        return currentSize;
    }

    public void reset() {
        prevSize = 0.0;
        currentSize = 0.0;
    }
}
