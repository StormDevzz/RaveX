package ravex.parameter;

import java.util.List;

public class ModeParameter extends Parameter<String> {
    private List<String> modes;

    public ModeParameter(String name, String defaultValue, List<String> modes) {
        super(name, defaultValue);
        this.modes = modes;
    }

    public List<String> getModes() {
        return modes;
    }

    // replaces the mode list at runtime, keeps the value if it fits
    public void setModes(List<String> newModes) {
        if (newModes == null || newModes.isEmpty()) return;
        try {
            modes.clear();
            modes.addAll(newModes);
        } catch (UnsupportedOperationException e) {
            // annotation modes come from Arrays.asList, fixed size
            modes = new java.util.ArrayList<>(newModes);
        }
        if (!modes.contains(getValue())) {
            setValue(modes.get(0));
        }
    }
}
