package ravex.parameter;

import java.lang.reflect.Field;

public abstract class Parameter<T> {
    private final String name;
    private T value;
    private java.util.function.Supplier<Boolean> visibility = () -> true;
    private boolean expanded = false;
    private Field boundField;
    private Object boundOwner;

    public Parameter(String name, T defaultValue) {
        this.name = name.replace(" ", "");
        this.value = defaultValue;
    }

    public void bind(Field field, Object owner) {
        this.boundField = field;
        this.boundOwner = owner;
        this.boundField.setAccessible(true);
    }

    public boolean isBound() {
        return boundField != null && boundOwner != null;
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

    @SuppressWarnings("unchecked")
    public T getValue() {
        if (isBound()) {
            try {
                Class<?> ft = boundField.getType();
                if (ft == int.class) return (T) Integer.valueOf(boundField.getInt(boundOwner));
                if (ft == double.class) return (T) Double.valueOf(boundField.getDouble(boundOwner));
                if (ft == float.class) return (T) Float.valueOf(boundField.getFloat(boundOwner));
                if (ft == long.class) return (T) Long.valueOf(boundField.getLong(boundOwner));
                if (ft == boolean.class) return (T) Boolean.valueOf(boundField.getBoolean(boundOwner));
                return (T) boundField.get(boundOwner);
            } catch (IllegalAccessException ignored) {}
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public void setValue(T value) {
        if (isBound()) {
            try {
                Class<?> ft = boundField.getType();
                if (value instanceof Number num) {
                    if (ft == int.class) boundField.setInt(boundOwner, num.intValue());
                    else if (ft == double.class) boundField.setDouble(boundOwner, num.doubleValue());
                    else if (ft == float.class) boundField.setFloat(boundOwner, num.floatValue());
                    else if (ft == long.class) boundField.setLong(boundOwner, num.longValue());
                    else boundField.set(boundOwner, value);
                } else if (ft == boolean.class) {
                    boundField.setBoolean(boundOwner, (Boolean) value);
                } else {
                    boundField.set(boundOwner, value);
                }
                return;
            } catch (IllegalAccessException ignored) {}
        }
        this.value = value;
    }

    public Parameter<T> setVisible(java.util.function.Supplier<Boolean> visibility) {
        this.visibility = visibility;
        return this;
    }

    @SuppressWarnings("unchecked")
    public boolean setValueFromObject(Object raw) {
        try {
            if (raw instanceof Number num) {
                Object cur = getValue();
                if (cur instanceof Integer || cur instanceof Long || cur instanceof Short || cur instanceof Byte) {
                    setValue((T) Integer.valueOf(num.intValue()));
                } else {
                    setValue((T) Double.valueOf(num.doubleValue()));
                }
            } else {
                setValue((T) raw);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Parameter<?> parent = null;

    public Parameter<T> visibleIfExpanded(Parameter<?> parent) {
        this.parent = parent;
        this.visibility = () -> parent.isExpanded();
        return this;
    }

    public Parameter<?> getParent() {
        return parent;
    }

    public boolean isVisible() {
        return visibility.get();
    }
}
