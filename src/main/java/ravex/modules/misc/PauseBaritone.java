package ravex.modules.misc;
import ravex.integrations.baritone.BaritoneIntegration;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
public class PauseBaritone extends Module {
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

    public static PauseBaritone itz() {
        return ModuleManager.get(PauseBaritone.class);
    }
}
