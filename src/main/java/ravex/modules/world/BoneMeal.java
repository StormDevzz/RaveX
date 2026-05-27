package ravex.modules.world;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;

public class BoneMeal extends Module {
    public static final BoneMeal INSTANCE = new BoneMeal();

    private final NumberParameter range = new NumberParameter("Range", 4.5, 2.0, 6.0, 0.1);

    private BoneMeal() {
        super("BoneMeal", Category.WORLD);
        addParameter(range);
    }
}
