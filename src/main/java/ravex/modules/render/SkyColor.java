package ravex.modules.render;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ColorParameter;
public class SkyColor extends Module {
    public static final SkyColor INSTANCE = new SkyColor();
    public final ColorParameter skyColor = new ColorParameter("SkyColor", 0xFF4FC3F7);

}
