package ravex.parameter;

public class ColorParameter extends Parameter<Integer> {
    private boolean themeSync = false;

    public ColorParameter(String name, int defaultArgb) {
        super(name, defaultArgb);
    }

    public boolean isThemeSync() {
        return themeSync;
    }

    public void setThemeSync(boolean themeSync) {
        this.themeSync = themeSync;
    }

    @Override
    public Integer getValue() {
        if (themeSync) {
            return ravex.gui.clickgui.ColorUtility.getActiveColor();
        }
        return super.getValue();
    }
}
