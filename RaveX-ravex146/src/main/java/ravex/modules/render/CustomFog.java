package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ColorParameter;

public class CustomFog extends Module {
    public static final CustomFog INSTANCE = new CustomFog();

    public final ColorParameter color = new ColorParameter("Fog Color", 0xFFFF5500);

    private CustomFog() {
        super("CustomFog", Category.RENDER);
        addParameter(color);
    }
}
