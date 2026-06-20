package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ColorParameter;

public class SkyColor extends Module {
    public static final SkyColor INSTANCE = new SkyColor();

    public final ColorParameter skyColor = new ColorParameter("Sky Color", 0xFF4FC3F7);

    private SkyColor() {
        super("SkyColor", Category.RENDER);
        addParameter(skyColor);
    }
}
