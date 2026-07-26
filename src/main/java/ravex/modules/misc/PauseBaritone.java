package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.integrations.baritone.BaritoneIntegration;

@ModuleInfo(name = "PauseBaritone", category = "Misc")
public class PauseBaritone implements ModuleAccess {
private final BaritoneIntegration baritone = new BaritoneIntegration();
    public void onEnable() {
        if (baritone.init()) {
            baritone.cancelPathing();
        }
        ravex.manager.ModuleManager.INSTANCE.getByName("PauseBaritone").setEnabled(false);
    }

    public static PauseBaritone itz() {
        return ravex.manager.ModuleManager.delegate(PauseBaritone.class);
    }


}