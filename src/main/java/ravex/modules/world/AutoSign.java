package ravex.modules.world;
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.parameter.StringParameter;
public class AutoSign extends Module {
    public final StringParameter line1 = new StringParameter("Line1", "RaveX");
    public final StringParameter line2 = new StringParameter("Line2", "Client");
    public final StringParameter line3 = new StringParameter("Line3", "OnTop");
    public final StringParameter line4 = new StringParameter("Line4", "");

    public static boolean maybeEnabled() {
        return maybeEnabled(AutoSign.class);
    }
    public static AutoSign itz() {
        return ModuleManager.get(AutoSign.class);
    }
}
