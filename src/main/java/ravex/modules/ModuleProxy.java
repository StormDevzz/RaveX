package ravex.modules;

import ravex.modules.Module;

import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.MultiSelectParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.Parameter;
import ravex.parameter.StringParameter;
import net.minecraft.client.gui.GuiGraphics;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ModuleProxy extends Module {
    private final Object component;
    private final Class<?> compClass;
    private final List<Method> tickMethods = new ArrayList<>();
    private final List<Method> enableMethods = new ArrayList<>();
    private final List<Method> disableMethods = new ArrayList<>();
    private final List<Method> renderMethods = new ArrayList<>();
    private Method pojoGetX, pojoSetX, pojoGetY, pojoSetY;
    private Method pojoGetW, pojoSetW, pojoGetH, pojoSetH;

    public ModuleProxy(Object component) {
        this.component = component;
        this.compClass = component.getClass();
        scanLifecycle();
        scanParameters();
        scanHudDelegates();
    }

    private void scanLifecycle() {
        for (Method m : compClass.getDeclaredMethods()) {
            String name = m.getName();
            m.setAccessible(true);
            if ("onTick".equals(name)) tickMethods.add(m);
            if ("onEnable".equals(name)) enableMethods.add(m);
            if ("onDisable".equals(name)) disableMethods.add(m);
            if ("render".equals(name)) renderMethods.add(m);
        }
    }

    private void scanParameters() {
        Class<?> cl = compClass;
        while (cl != null && cl != Object.class) {
            for (Field f : cl.getDeclaredFields()) {
                if (Parameter.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    try {
                        Parameter<?> p = (Parameter<?>) f.get(component);
                        if (p != null) addParameter(p);
                    } catch (IllegalAccessException ignored) {}
                } else if (f.isAnnotationPresent(ravex.modules.annotations.Parameter.class)) {
                    f.setAccessible(true);
                    Parameter<?> p = createParameterFromAnnotation(f);
                    if (p != null) {
                        p.bind(f, component);
                        addParameter(p);
                    }
                }
            }
            cl = cl.getSuperclass();
        }
    }

    private Parameter<?> createParameterFromAnnotation(Field field) {
        ravex.modules.annotations.Parameter ann = field.getAnnotation(ravex.modules.annotations.Parameter.class);
        if (ann == null) return null;
        Class<?> type = field.getType();
        String name = ann.name();
        try {
            if (type == boolean.class || type == Boolean.class) {
                if (ann.maybe()) {
                    try { field.setBoolean(component, true); } catch (IllegalAccessException ignored) {}
                    return new BooleanParameter(name, true);
                }
                return new BooleanParameter(name, field.getBoolean(component));
            }
            if (ann.color() && (type == int.class || type == Integer.class)) {
                return new ColorParameter(name, field.getInt(component));
            }
            if (type == int.class || type == Integer.class) {
                return new NumberParameter(name, field.getInt(component), ann.min(), ann.max(), ann.step());
            }
            if (type == double.class || type == Double.class) {
                return new NumberParameter(name, field.getDouble(component), ann.min(), ann.max(), ann.step());
            }
            if (type == float.class || type == Float.class) {
                return new NumberParameter(name, field.getFloat(component), ann.min(), ann.max(), ann.step());
            }
            if (type == String.class) {
                String[] modes = ann.modes();
                String value = (String) field.get(component);
                if (modes.length > 0) {
                    return new ModeParameter(name, value, Arrays.asList(modes));
                }
                return new StringParameter(name, value);
            }
            if (List.class.isAssignableFrom(type)) {
                String[] opts = ann.options().length > 0 ? ann.options() : ann.modes();
                if (opts.length > 0) {
                    @SuppressWarnings("unchecked")
                    List<String> selected = (List<String>) field.get(component);
                    return new MultiSelectParameter(name, selected, Arrays.asList(opts));
                }
            }
        } catch (IllegalAccessException ignored) {}
        return null;
    }

    private void scanHudDelegates() {
        try { pojoGetX = compClass.getMethod("getX"); } catch (NoSuchMethodException ignored) {}
        try { pojoSetX = compClass.getMethod("setX", int.class); } catch (NoSuchMethodException ignored) {}
        try { pojoGetY = compClass.getMethod("getY"); } catch (NoSuchMethodException ignored) {}
        try { pojoSetY = compClass.getMethod("setY", int.class); } catch (NoSuchMethodException ignored) {}
        try { pojoGetW = compClass.getMethod("getWidth"); } catch (NoSuchMethodException ignored) {}
        try { pojoSetW = compClass.getMethod("setWidth", int.class); } catch (NoSuchMethodException ignored) {}
        try { pojoGetH = compClass.getMethod("getHeight"); } catch (NoSuchMethodException ignored) {}
        try { pojoSetH = compClass.getMethod("setHeight", int.class); } catch (NoSuchMethodException ignored) {}
    }

    public Object getComponent() { return component; }

    @Override
    public int getX() {
        if (pojoGetX != null) { try { return (int) pojoGetX.invoke(component); } catch (Exception ignored) {} }
        return super.getX();
    }

    @Override
    public void setX(int x) {
        if (pojoSetX != null) { try { pojoSetX.invoke(component, x); } catch (Exception ignored) {} }
        super.setX(x);
    }

    @Override
    public int getY() {
        if (pojoGetY != null) { try { return (int) pojoGetY.invoke(component); } catch (Exception ignored) {} }
        return super.getY();
    }

    @Override
    public void setY(int y) {
        if (pojoSetY != null) { try { pojoSetY.invoke(component, y); } catch (Exception ignored) {} }
        super.setY(y);
    }

    @Override
    public int getWidth() {
        if (pojoGetW != null) { try { return (int) pojoGetW.invoke(component); } catch (Exception ignored) {} }
        return super.getWidth();
    }

    @Override
    public void setWidth(int w) {
        if (pojoSetW != null) { try { pojoSetW.invoke(component, w); } catch (Exception ignored) {} }
        super.setWidth(w);
    }

    @Override
    public int getHeight() {
        if (pojoGetH != null) { try { return (int) pojoGetH.invoke(component); } catch (Exception ignored) {} }
        return super.getHeight();
    }

    @Override
    public void setHeight(int h) {
        if (pojoSetH != null) { try { pojoSetH.invoke(component, h); } catch (Exception ignored) {} }
        super.setHeight(h);
    }

    @Override
    public void onTick() {
        invokeMethods(tickMethods);
    }

    @Override
    protected void onEnable() {
        invokeMethods(enableMethods);
    }

    @Override
    protected void onDisable() {
        invokeMethods(disableMethods);
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        for (Method m : renderMethods) {
            try {
                Class<?>[] types = m.getParameterTypes();
                if (types.length == 2 && types[0] == GuiGraphics.class && types[1] == float.class) {
                    m.invoke(component, graphics, partialTicks);
                } else if (types.length == 0) {
                    m.invoke(component);
                }
            } catch (Exception ignored) {}
        }
    }

    private void invokeMethods(List<Method> methods) {
        for (Method m : methods) {
            try {
                m.invoke(component);
            } catch (Exception ignored) {}
        }
    }
}
