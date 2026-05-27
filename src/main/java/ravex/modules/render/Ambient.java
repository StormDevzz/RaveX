package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;

public class Ambient extends Module {
    public static final Ambient INSTANCE = new Ambient();

    public final NumberParameter r = new NumberParameter("Red", 255.0, 0.0, 255.0, 1.0);
    public final NumberParameter g = new NumberParameter("Green", 255.0, 0.0, 255.0, 1.0);
    public final NumberParameter b = new NumberParameter("Blue", 255.0, 0.0, 255.0, 1.0);
    public final NumberParameter a = new NumberParameter("Alpha", 30.0, 0.0, 255.0, 1.0);

    private Ambient() {
        super("Ambient", Category.RENDER);
        addParameter(r);
        addParameter(g);
        addParameter(b);
        addParameter(a);
    }
}
