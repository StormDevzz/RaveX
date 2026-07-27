package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.integrations.baritone.BaritoneIntegration;
import ravex.modules.Modules;

@Module(name = "PauseBaritone", category = "Misc")
public class PauseBaritone {
private final BaritoneIntegration baritone = new BaritoneIntegration();
    public void onEnable() {
        if (baritone.init()) {
            baritone.cancelPathing();
        }
        Modules.setEnabled(PauseBaritone.class, false);
    }




}