package ravex.modules.world;
<<<<<<< HEAD
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.parameter.StringParameter;
public class AutoSign extends Module {
<<<<<<< HEAD
=======
    public static final AutoSign INSTANCE = new AutoSign();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final StringParameter line1 = new StringParameter("Line1", "RaveX");
    public final StringParameter line2 = new StringParameter("Line2", "Client");
    public final StringParameter line3 = new StringParameter("Line3", "OnTop");
    public final StringParameter line4 = new StringParameter("Line4", "");

<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(AutoSign.class);
    }
    public static AutoSign itz() {
        return ModuleManager.get(AutoSign.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
