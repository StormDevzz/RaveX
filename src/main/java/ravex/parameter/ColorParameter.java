package ravex.parameter;

public class ColorParameter extends Parameter<Integer> {
    private static final int RAINBOW_SPEED = 18;
    private boolean themeSync = false;
    private boolean rainbow = false;

    public ColorParameter(String name, int defaultArgb) {
        super(name, defaultArgb);
    }

    public boolean isThemeSync() {
        return themeSync;
    }

    public void setThemeSync(boolean themeSync) {
        this.themeSync = themeSync;
    }

    public boolean isRainbow() {
        return rainbow;
    }

    public void setRainbow(boolean rainbow) {
        this.rainbow = rainbow;
    }

    public int getStoredValue() {
        return super.getValue();
    }

    private int rainbowIndex() {
        return Math.floorMod(getName().hashCode(), 360);
    }

    @Override
    public Integer getValue() {
        if (themeSync) {
            return ravex.utility.render.ColorUtility.getActiveColor();
        }
        if (rainbow) {
            int stored = super.getValue();
            int alpha = (stored >>> 24) & 0xFF;
            if (alpha == 0) alpha = 255;
            return ravex.utility.render.ColorUtility.rainbow(RAINBOW_SPEED, rainbowIndex(), 1f, 1f, alpha / 255f);
        }
        return super.getValue();
    }
}
