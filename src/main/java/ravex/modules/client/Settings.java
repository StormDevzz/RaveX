package ravex.modules.client;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;

public class Settings extends Module {
    public static final Settings INSTANCE = new Settings();

    public final NumberParameter headerTextX = new NumberParameter("Header Text X", 24, 10, 60, 1);
    public final NumberParameter moduleTextX = new NumberParameter("Module Text X", 9, 3, 30, 1);

    private Settings() {
        super("Settings", Category.CLIENT);
        addParameter(headerTextX);
        addParameter(moduleTextX);
        setEnabled(true);
    }
}
