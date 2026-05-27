package ravex.modules.misc;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;

public class AntiAfk extends Module {
    public static final AntiAfk INSTANCE = new AntiAfk();

    private final NumberParameter delay = new NumberParameter("DelaySec", 10.0, 5.0, 60.0, 1.0);

    private AntiAfk() {
        super("AntiAfk", Category.MISC);
        addParameter(delay);
    }
}
