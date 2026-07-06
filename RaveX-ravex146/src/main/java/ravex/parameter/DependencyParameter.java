package ravex.parameter;

public class DependencyParameter<T, P extends Parameter<T>> extends Parameter<T> {
    private final P delegate;
    private final Parameter<?> parent;
    private final Object requiredValue;

    public DependencyParameter(P delegate, Parameter<?> parent, Object requiredValue) {
        super(delegate.getName(), delegate.getValue());
        this.delegate = delegate;
        this.parent = parent;
        this.requiredValue = requiredValue;
        
        
        this.setVisible(() -> parent.getValue().equals(requiredValue));
    }

    public P getDelegate() {
        return delegate;
    }

    @Override
    public T getValue() {
        return delegate.getValue();
    }

    @Override
    public void setValue(T value) {
        delegate.setValue(value);
    }
}
