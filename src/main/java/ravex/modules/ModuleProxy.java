package ravex.modules;

import ravex.event.EventBusHolder;
import ravex.parameter.Parameter;
import ravex.parameter.ParameterFactory;
import net.minecraft.client.gui.GuiGraphics;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
    private Method pojoConsumesKeyBind;

    public ModuleProxy(Object component) {
        this.component = component;
        this.compClass = component.getClass();
        var ann = compClass.getDeclaredAnnotation(ravex.modules.annotations.Module.class);
        if (ann != null) {
            setName(ann.name());
        }
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
        try { pojoConsumesKeyBind = compClass.getMethod("consumesKeyBindPress"); } catch (NoSuchMethodException ignored) {}
    }

    private void scanParameters() {
        for (Parameter<?> p : ParameterFactory.scan(component, Object.class)) {
            addParameter(p);
        }
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
    public boolean consumesKeyBindPress() {
        if (pojoConsumesKeyBind != null) {
            try { return (boolean) pojoConsumesKeyBind.invoke(component); } catch (Exception ignored) {}
        }
        return super.consumesKeyBindPress();
    }

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
        EventBusHolder.get().subscribe(component);
        invokeMethods(enableMethods);
    }

    @Override
    protected void onDisable() {
        EventBusHolder.get().unsubscribe(component);
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
