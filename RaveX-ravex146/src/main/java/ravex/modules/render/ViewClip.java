package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;


public class ViewClip extends Module {
    public static final ViewClip INSTANCE = new ViewClip();

    public final BooleanParameter bypassWalls = new BooleanParameter("Bypass Walls", true);
    public final NumberParameter cameraDistance = new NumberParameter("Distance", 4.0, 1.0, 20.0, 0.5);

    private ViewClip() {
        super("ViewClip", Category.RENDER);
        addParameter(bypassWalls);
        addParameter(cameraDistance);
    }
}
