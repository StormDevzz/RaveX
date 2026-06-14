package ravex.modules.client;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;

public class Settings extends Module {
    public static final Settings INSTANCE = new Settings();

    public final NumberParameter fontSize = new NumberParameter("Font Size", 0.9, 0.5, 1.5, 0.05);

    private Settings() {
        super("Settings", Category.CLIENT);
        addParameter(fontSize);
        setEnabled(true);
    }
}
