package ravex.modules.misc;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
public class PortalGui extends Module {

    public static boolean maybeEnabled() {
        return maybeEnabled(PortalGui.class);
    }

    public static PortalGui itz() {
        return ModuleManager.get(PortalGui.class);
    }
}
