package ravex.parameter;

public abstract class Parameter<T> {
    private final String name;
    private T value;
    private java.util.function.Supplier<Boolean> visibility = () -> true;
    private boolean expanded = false;

    public Parameter(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public Parameter<T> setVisible(java.util.function.Supplier<Boolean> visibility) {
        this.visibility = visibility;
        return this;
    }

    public Parameter<T> visibleIfExpanded(Parameter<?> parent) {
        this.visibility = () -> parent.isExpanded();
        return this;
    }

    public boolean isVisible() {
        return visibility.get();
    }
}
