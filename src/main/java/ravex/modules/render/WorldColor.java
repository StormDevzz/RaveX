package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.parameter.ColorParameter;

@Module(name = "WorldColor", category = "Render")
public class WorldColor {
    @Parameter(name = "Fog")
    public boolean fog = false;
    public final ColorParameter fogColor = ((ColorParameter) new ColorParameter("FogColor", 0xFFFF5500).setVisible(() -> fog));

    @Parameter(name = "Sky")
    public boolean sky = false;
    public final ColorParameter skyColor = ((ColorParameter) new ColorParameter("SkyColor", 0xFF4FC3F7).setVisible(() -> sky));

    @Parameter(name = "Cloud")
    public boolean cloud = false;
    public final ColorParameter cloudColor = ((ColorParameter) new ColorParameter("CloudColor", 0xFFFFFFFF).setVisible(() -> cloud));

}