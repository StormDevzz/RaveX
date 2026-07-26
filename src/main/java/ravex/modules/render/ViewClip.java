package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;

@ModuleInfo(name = "ViewClip", category = "Render")
public class ViewClip extends ravex.modules.Module {
public final BooleanParameter bypassWalls = new BooleanParameter("BypassWalls", true);
    public final NumberParameter cameraDistance = new NumberParameter("Distance", 4.0, 1.0, 20.0, 0.5);

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ViewClip").getEnabled();
    }

    public static ViewClip itz() {
        return ravex.manager.ModuleManager.delegate(ViewClip.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}