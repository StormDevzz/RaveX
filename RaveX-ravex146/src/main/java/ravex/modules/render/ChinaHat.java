git commit -m "Опиши тут кратко, что изменил"package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;

public class ChinaHat extends Module {
    public static final ChinaHat INSTANCE = new ChinaHat();

    public final ColorParameter color = new ColorParameter("Color", 0xFFFFFFFF);
    public final NumberParameter alpha = new NumberParameter("Alpha", 200.0, 0.0, 255.0, 1.0);
    public final NumberParameter radius = new NumberParameter("Radius", 0.6, 0.3, 1.5, 0.05);
    public final NumberParameter height = new NumberParameter("Height", 0.4, 0.1, 1.0, 0.05);

    private ChinaHat() {
        super("ChinaHat", Category.RENDER);
        addParameter(color);
        addParameter(alpha);
        addParameter(radius);
        addParameter(height);
    }
}
