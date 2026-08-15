package ravex.parameter;

public class NumberParameter extends Parameter<Double> {
    private final double min;
    private final double max;
    private final double step;

    public NumberParameter(String name, double defaultValue, double min, double max, double step) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    public Double getValue() {
        Object raw = super.getValue();
        if (raw instanceof Number num) {
            return num.doubleValue();
        }
        return null;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }
}
