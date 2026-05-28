package ravex.utility.render.animate;

public class FillAnimation {
    private double prevProgress = 0.0;
    private double currentProgress = 0.0;

    /**
     * Updates the animation progress.
     * @param active whether the animation is currently filling/active
     * @param speed the rate of filling/interpolation
     * @return current progress value
     */
    public double update(boolean active, double speed) {
        prevProgress = currentProgress;
        double target = active ? 1.0 : 0.0;
        currentProgress += (target - currentProgress) * speed;
        if (currentProgress < 0.001) currentProgress = 0.0;
        if (currentProgress > 0.999) currentProgress = 1.0;
        return currentProgress;
    }

    public double getProgress(float partialTicks) {
        return prevProgress + (currentProgress - prevProgress) * partialTicks;
    }

    public double getProgress() {
        return currentProgress;
    }

    public void setProgress(double value) {
        this.currentProgress = value;
        this.prevProgress = value;
    }

    public void reset() {
        prevProgress = 0.0;
        currentProgress = 0.0;
    }
}
