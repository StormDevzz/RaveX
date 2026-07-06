package ravex.modules.render;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ColorParameter;
public class Fog extends Module {
    public static final Fog INSTANCE = new Fog();
    public final ColorParameter color = new ColorParameter("FogColor", 0xFFFF5500);

}
