package ravex.parameter;

import java.util.List;

public class ModeParameter extends Parameter<String> {
    private final List<String> modes;

    public ModeParameter(String name, String defaultValue, List<String> modes) {
        super(name, defaultValue);
        this.modes = modes;
    }

    public List<String> getModes() {
        return modes;
    }
}
