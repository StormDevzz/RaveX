package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;

@Module(name = "WorldColor", category = "Render")
public class WorldColor {
    @Parameter(name = "Fog")
    public boolean fog = false;
    @Parameter(name = "FogColor", color = true, visible = "fog")
    public int fogColor = 0xFFFF5500;

    @Parameter(name = "Sky")
    public boolean sky = false;
    @Parameter(name = "SkyColor", color = true, visible = "sky")
    public int skyColor = 0xFF4FC3F7;

    @Parameter(name = "Cloud")
    public boolean cloud = false;
    @Parameter(name = "CloudColor", color = true, visible = "cloud")
    public int cloudColor = 0xFFFFFFFF;

}