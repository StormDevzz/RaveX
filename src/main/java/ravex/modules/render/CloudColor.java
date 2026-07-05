package ravex.modules.render;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ColorParameter;
public class CloudColor extends Module {
    public static final CloudColor INSTANCE = new CloudColor();
    public final ColorParameter cloudColor = new ColorParameter("CloudColor", 0xFFFFFFFF);

}
