package ravex.modules.combat;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;

public class Hitboxes extends Module {
    public static final Hitboxes INSTANCE = new Hitboxes();

    public final NumberParameter size = new NumberParameter("Size", 0.3, 0.0, 2.0, 0.05);

    private Hitboxes() {
        super("Hitboxes", Category.COMBAT);
        addParameter(size);
    }
}
