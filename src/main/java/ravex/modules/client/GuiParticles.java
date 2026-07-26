package ravex.modules.client;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
@ModuleInfo(name = "GuiParticles", category = "Client")
public class GuiParticles extends ravex.modules.Module {
public final ModeParameter type = new ModeParameter("Type", "Star",
        java.util.List.of("Star", "Bone", "Fire", "Sun", "Thunder", "Wave"));
    public final ColorParameter color = new ColorParameter("Color", 0xFFFFFFFF);
    public final NumberParameter amount = new NumberParameter("Amount", 55, 10, 150, 5);
    public final NumberParameter size = new NumberParameter("Size", 3, 1, 15, 0.5);
    public final NumberParameter speed = new NumberParameter("Speed", 1.0, 0.1, 5.0, 0.1);
    public GuiParticles() {
        
        enabled = false;
    }

    public static GuiParticles itz() {
        return ravex.manager.ModuleManager.delegate(GuiParticles.class);
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