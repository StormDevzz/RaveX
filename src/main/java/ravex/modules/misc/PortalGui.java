package ravex.modules.misc;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import ravex.modules.Module;
public class PortalGui extends Module {

    public static boolean maybeEnabled() {
        return maybeEnabled(PortalGui.class);
    }

    public static PortalGui itz() {
        return ModuleManager.get(PortalGui.class);
    }
=======
import ravex.modules.Category;
import ravex.modules.Module;
public class PortalGui extends Module {
    public static final PortalGui INSTANCE = new PortalGui();

>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
