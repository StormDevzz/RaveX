package ravex.addon.template;

import ravex.addon.Addon;
import ravex.addon.module.AddonModule;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.PlayerUtility;

public class DemoModule extends AddonModule {
    @Parameter(name = "FeatureEnabled")
    public boolean featureEnabled = true;

    @Parameter(name = "Speed", min = 0.1, max = 5.0, step = 0.1)
    public double speed = 1.5;

    @Parameter(name = "Mode", modes = {"Basic", "Advanced"})
    public String mode = "Basic";

    @Parameter(name = "Color", color = true)
    public int color = 0xFF00FF00;

    public DemoModule(Addon parent) {
        super("DemoModule", "Custom", parent);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onTick() {
        if (!featureEnabled) {
            return;
        }
        if (PlayerUtility.getPlayer() == null) {
            return;
        }
        if (isWindows()) {
            tickWindows();
        } else {
            tickLinux();
        }
    }

    private boolean isWindows() {
        return MainAddon.isWindows();
    }

    private void tickWindows() {
    }

    private void tickLinux() {
    }
}
