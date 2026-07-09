package ravex.modules.misc;
<<<<<<< HEAD
import ravex.integrations.baritone.BaritoneIntegration;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
public class PauseBaritone extends Module {
=======
import ravex.integrations.BaritoneIntegration;
import ravex.modules.Category;
import ravex.modules.Module;
public class PauseBaritone extends Module {
    public static final PauseBaritone INSTANCE = new PauseBaritone();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private final BaritoneIntegration baritone = new BaritoneIntegration();
    private PauseBaritone() {
        super("PauseBaritone");
        setVisibleCondition(BaritoneIntegration::isBaritonePresent);
    }
    @Override
    public void toggle() {
        if (!getEnabled()) {
            super.setEnabled(true);
        }
    }
    @Override
    protected boolean hasToggleSound() {
        return false;
    }
    @Override
    protected void onEnable() {
        if (baritone.init()) {
            baritone.cancelPathing();
        }
        super.setEnabled(false);
    }
<<<<<<< HEAD

    public static PauseBaritone itz() {
        return ModuleManager.get(PauseBaritone.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
