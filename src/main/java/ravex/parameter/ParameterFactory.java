package ravex.parameter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ParameterFactory {
    private ParameterFactory() {}

    public static List<Parameter<?>> scan(Object owner, Class<?> stopAt) {
        List<Parameter<?>> out = new ArrayList<>();
        Class<?> clazz = owner.getClass();
        while (clazz != null && clazz != stopAt) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Parameter.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    try {
                        Parameter<?> p = (Parameter<?>) field.get(owner);
                        if (p != null && !out.contains(p)) out.add(p);
                    } catch (IllegalAccessException ignored) {}
                } else if (field.isAnnotationPresent(ravex.modules.annotations.Parameter.class)) {
                    field.setAccessible(true);
                    ravex.modules.annotations.Parameter ann = field.getAnnotation(ravex.modules.annotations.Parameter.class);
                    Parameter<?> p = create(field, ann, owner);
                    if (p != null && !out.contains(p)) {
                        p.bind(field, owner);
                        out.add(p);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return out;
    }

    public static Parameter<?> create(Field field, ravex.modules.annotations.Parameter ann, Object owner) {
        Class<?> type = field.getType();
        String name = ann.name();
        Parameter<?> param = null;
        try {
            if (type == boolean.class || type == Boolean.class) {
                if (ann.group()) {
                    param = new GroupParameter(name, field.getBoolean(owner));
                } else if (ann.maybe()) {
                    try { field.setBoolean(owner, true); } catch (IllegalAccessException ignored) {}
                    param = new BooleanParameter(name, true);
                } else {
                    param = new BooleanParameter(name, field.getBoolean(owner));
                }
            } else if (ann.color() && (type == int.class || type == Integer.class)) {
                param = new ColorParameter(name, field.getInt(owner));
            } else if (type == int.class || type == Integer.class) {
                param = new NumberParameter(name, field.getInt(owner), ann.min(), ann.max(), ann.step());
            } else if (type == double.class || type == Double.class) {
                param = new NumberParameter(name, field.getDouble(owner), ann.min(), ann.max(), ann.step());
            } else if (type == float.class || type == Float.class) {
                param = new NumberParameter(name, field.getFloat(owner), ann.min(), ann.max(), ann.step());
            } else if (type == String.class) {
                String[] modes = ann.modes();
                String value = (String) field.get(owner);
                if (modes.length > 0) {
                    param = new ModeParameter(name, value, Arrays.asList(modes));
                } else {
                    param = new StringParameter(name, value);
                }
            } else if (List.class.isAssignableFrom(type)) {
                String[] opts = ann.options().length > 0 ? ann.options() : ann.modes();
                if (opts.length > 0) {
                    @SuppressWarnings("unchecked")
                    List<String> selected = (List<String>) field.get(owner);
                    param = new MultiSelectParameter(name, selected, Arrays.asList(opts));
                }
            }
        } catch (IllegalAccessException ignored) {}

        if (param != null) {
            applyVisibleCondition(param, ann.visible(), owner);
        }
        return param;
    }

    public static void applyVisibleCondition(Parameter<?> param, String visibleExpr, Object owner) {
        if (visibleExpr == null || visibleExpr.isEmpty()) return;
        String[] orParts = visibleExpr.split("\\|\\|");
        List<List<java.util.function.Supplier<Boolean>>> orGroups = new ArrayList<>();
        for (String orPart : orParts) {
            String[] andParts = orPart.split("&&");
            List<java.util.function.Supplier<Boolean>> group = new ArrayList<>();
            for (String part : andParts) {
                java.util.function.Supplier<Boolean> cond = buildVisibleCondition(part.trim(), owner);
                if (cond == null) return;
                group.add(cond);
            }
            orGroups.add(group);
        }
        param.setVisible(() -> {
            for (List<java.util.function.Supplier<Boolean>> group : orGroups) {
                boolean all = true;
                for (java.util.function.Supplier<Boolean> cond : group) {
                    if (!Boolean.TRUE.equals(cond.get())) {
                        all = false;
                        break;
                    }
                }
                if (all) return true;
            }
            return false;
        });
    }

    private static java.util.function.Supplier<Boolean> buildVisibleCondition(String expr, Object owner) {
        if (expr.isEmpty()) return null;
        int neqIdx = expr.indexOf("!=");
        int eqIdx = expr.indexOf('=');
        String fieldName;
        if (neqIdx >= 0) {
            fieldName = expr.substring(0, neqIdx).trim();
            String expectedValue = expr.substring(neqIdx + 2).trim();
            try {
                Field depField = owner.getClass().getDeclaredField(fieldName);
                depField.setAccessible(true);
                return () -> {
                    try {
                        return !expectedValue.equals(depField.get(owner));
                    } catch (IllegalAccessException e) {
                        return false;
                    }
                };
            } catch (NoSuchFieldException ignored) {
                return null;
            }
        } else if (eqIdx >= 0) {
            fieldName = expr.substring(0, eqIdx).trim();
            String expectedValue = expr.substring(eqIdx + 1).trim();
            try {
                Field depField = owner.getClass().getDeclaredField(fieldName);
                depField.setAccessible(true);
                return () -> {
                    try {
                        return expectedValue.equals(depField.get(owner));
                    } catch (IllegalAccessException e) {
                        return false;
                    }
                };
            } catch (NoSuchFieldException ignored) {
                return null;
            }
        } else {
            fieldName = expr.trim();
            boolean negate = fieldName.startsWith("!");
            if (negate) {
                fieldName = fieldName.substring(1).trim();
            }
            boolean neg = negate;
            try {
                Field depField = owner.getClass().getDeclaredField(fieldName);
                depField.setAccessible(true);
                return () -> {
                    try {
                        boolean value = depField.getBoolean(owner);
                        return neg != value;
                    } catch (IllegalAccessException e) {
                        return false;
                    }
                };
            } catch (NoSuchFieldException ignored) {
                return null;
            }
        }
    }
}