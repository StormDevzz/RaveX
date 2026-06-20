package ravex.addon.template;

import ravex.addon.Addon;
import ravex.addon.AddonModule;
import ravex.modules.Category;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;

public class AnotherModule extends AddonModule {
    public final BooleanParameter someSetting = new BooleanParameter("Some Setting", true);
    public final NumberParameter speedMultiplier = new NumberParameter("Speed Multiplier", 1.5, 0.1, 5.0, 0.1);

    public AnotherModule(Addon parent) {
        super("Another", Category.CUSTOM, parent);
        addParameter(someSetting);
        addParameter(speedMultiplier);
    }

    @Override
    public void onTick() {
        if (someSetting.getValue()) {
            // Do some cool custom actions here!
        }
    }

    @Override
    public void onEnable() {
        // Log or run initialization
    }

    @Override
    public void onDisable() {
        // Log or cleanup
    }
}
