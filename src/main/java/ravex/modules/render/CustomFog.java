package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;

public class CustomFog extends Module {
    public static final CustomFog INSTANCE = new CustomFog();

    public final NumberParameter r = new NumberParameter("Red", 255.0, 0.0, 255.0, 1.0);
    public final NumberParameter g = new NumberParameter("Green", 0.0, 0.0, 255.0, 1.0);
    public final NumberParameter b = new NumberParameter("Blue", 0.0, 0.0, 255.0, 1.0);

    private CustomFog() {
        super("CustomFog", Category.RENDER);
        addParameter(r);
        addParameter(g);
        addParameter(b);
    }
}
