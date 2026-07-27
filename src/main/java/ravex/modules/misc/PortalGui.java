package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.Modules;
@Module(name = "PortalGui", category = "Misc")
public class PortalGui {
public static boolean maybeEnabled() {
        return Modules.enabled(PortalGui.class);
    }




}