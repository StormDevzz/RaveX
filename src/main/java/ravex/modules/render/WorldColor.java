package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;

@ModuleInfo(name = "WorldColor", category = "Render")
public class WorldColor extends ravex.modules.Module {
public final BooleanParameter fog = new BooleanParameter("Fog", false);
    public final ColorParameter fogColor = ((ColorParameter) new ColorParameter("FogColor", 0xFFFF5500).setVisible(() -> fog.getValue()));

    public final BooleanParameter sky = new BooleanParameter("Sky", false);
    public final ColorParameter skyColor = ((ColorParameter) new ColorParameter("SkyColor", 0xFF4FC3F7).setVisible(() -> sky.getValue()));

    public final BooleanParameter cloud = new BooleanParameter("Cloud", false);
    public final ColorParameter cloudColor = ((ColorParameter) new ColorParameter("CloudColor", 0xFFFFFFFF).setVisible(() -> cloud.getValue()));

    private WorldColor() {
        
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("WorldColor").getEnabled();
    }

    public static WorldColor itz() {
        return ravex.manager.ModuleManager.delegate(WorldColor.class);
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