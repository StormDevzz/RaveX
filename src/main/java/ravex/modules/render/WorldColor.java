package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.parameter.ColorParameter;

@ModuleInfo(name = "WorldColor", category = "Render")
public class WorldColor implements ModuleAccess {
    @Parameter(name = "Fog")
    public boolean fog = false;
    public final ColorParameter fogColor = ((ColorParameter) new ColorParameter("FogColor", 0xFFFF5500).setVisible(() -> fog));

    @Parameter(name = "Sky")
    public boolean sky = false;
    public final ColorParameter skyColor = ((ColorParameter) new ColorParameter("SkyColor", 0xFF4FC3F7).setVisible(() -> sky));

    @Parameter(name = "Cloud")
    public boolean cloud = false;
    public final ColorParameter cloudColor = ((ColorParameter) new ColorParameter("CloudColor", 0xFFFFFFFF).setVisible(() -> cloud));

    private WorldColor() {
        
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("WorldColor").getEnabled();
    }

    public static WorldColor itz() {
        return ravex.manager.ModuleManager.delegate(WorldColor.class);
    }


}