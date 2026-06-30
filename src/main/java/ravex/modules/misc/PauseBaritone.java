package ravex.modules.misc;

import ravex.integrations.BaritoneIntegration;
import ravex.modules.Category;
import ravex.modules.Module;

public class PauseBaritone extends Module {
    public static final PauseBaritone INSTANCE = new PauseBaritone();

    private final BaritoneIntegration baritone = new BaritoneIntegration();

    private PauseBaritone() {
        super("PauseBaritone", Category.MISC);
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
}
