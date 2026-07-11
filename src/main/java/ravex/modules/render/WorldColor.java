package ravex.modules.render;

import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;

public class WorldColor extends Module {
    public final BooleanParameter fog = new BooleanParameter("Fog", false);
    public final ColorParameter fogColor = ((ColorParameter) new ColorParameter("FogColor", 0xFFFF5500).setVisible(() -> fog.getValue()));

    public final BooleanParameter sky = new BooleanParameter("Sky", false);
    public final ColorParameter skyColor = ((ColorParameter) new ColorParameter("SkyColor", 0xFF4FC3F7).setVisible(() -> sky.getValue()));

    public final BooleanParameter cloud = new BooleanParameter("Cloud", false);
    public final ColorParameter cloudColor = ((ColorParameter) new ColorParameter("CloudColor", 0xFFFFFFFF).setVisible(() -> cloud.getValue()));

    private WorldColor() {
        super("WorldColor");
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(WorldColor.class);
    }

    public static WorldColor itz() {
        return ModuleManager.get(WorldColor.class);
    }
}
