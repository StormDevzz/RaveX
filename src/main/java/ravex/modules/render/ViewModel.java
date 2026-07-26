package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;

@ModuleInfo(name = "ViewModel", category = "Render")
public class ViewModel extends ravex.modules.Module {
public final NumberParameter mainX = new NumberParameter("MainX", 0.0, -2.0, 2.0, 0.01);
    public final NumberParameter mainY = new NumberParameter("MainY", 0.0, -2.0, 2.0, 0.01);
    public final NumberParameter mainZ = new NumberParameter("MainZ", 0.0, -2.0, 2.0, 0.01);
    public final NumberParameter mainRotX = new NumberParameter("MainRotX", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter mainRotY = new NumberParameter("MainRotY", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter mainRotZ = new NumberParameter("MainRotZ", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter mainScale = new NumberParameter("MainScale", 1.0, 0.1, 3.0, 0.05);
    public final NumberParameter offX = new NumberParameter("OffX", 0.0, -2.0, 2.0, 0.01);
    public final NumberParameter offY = new NumberParameter("OffY", 0.0, -2.0, 2.0, 0.01);
    public final NumberParameter offZ = new NumberParameter("OffZ", 0.0, -2.0, 2.0, 0.01);
    public final NumberParameter offRotX = new NumberParameter("OffRotX", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter offRotY = new NumberParameter("OffRotY", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter offRotZ = new NumberParameter("OffRotZ", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter offScale = new NumberParameter("OffScale", 1.0, 0.1, 3.0, 0.05);
    public final NumberParameter swingSpeed = new NumberParameter("SwingSpeed", 1.0, 0.1, 3.0, 0.05);
    public final BooleanParameter hideMainHand = new BooleanParameter("HideMain", false);
    public final BooleanParameter hideOffHand = new BooleanParameter("HideOff", false);
    public final BooleanParameter noSwing = new BooleanParameter("NoSwing", false);

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ViewModel").getEnabled();
    }

    public static ViewModel itz() {
        return ravex.manager.ModuleManager.delegate(ViewModel.class);
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