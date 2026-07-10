package ravex.parameter;

import java.util.ArrayList;
import java.util.List;

public class MultiSelectParameter extends Parameter<List<String>> {
    private final List<String> options;

    public MultiSelectParameter(String name, List<String> defaultSelections, List<String> options) {
        super(name, new ArrayList<>(defaultSelections));
        this.options = options;
    }

    public List<String> getOptions() {
        return options;
    }

    public boolean isSelected(String option) {
        return getValue().contains(option);
    }

    public void select(String option) {
        if (options.contains(option) && !getValue().contains(option)) {
            getValue().add(option);
        }
    }

    public void deselect(String option) {
        getValue().remove(option);
    }

    public void toggle(String option) {
        if (isSelected(option)) {
            deselect(option);
        } else {
            select(option);
        }
    }
}
