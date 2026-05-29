package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;

public class Sounds extends Module {
    public static final Sounds INSTANCE = new Sounds();

    public final NumberParameter volume = new NumberParameter("Volume", 1.0, 0.0, 1.0, 0.1);

    private Sounds() {
        super("Sounds", Category.RENDER);
        addParameter(volume);
        setEnabled(true); // Enabled by default so sounds are active right away!
    }
}
