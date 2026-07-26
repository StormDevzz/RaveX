package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
@ModuleInfo(name = "PortalGui", category = "Misc")
public class PortalGui implements ModuleAccess {
public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("PortalGui").getEnabled();
    }

    public static PortalGui itz() {
        return ravex.manager.ModuleManager.delegate(PortalGui.class);
    }


}