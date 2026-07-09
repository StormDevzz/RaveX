package ravex.modules.render;

import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

import java.util.List;

public class Animations extends Module {

    public final ModeParameter mode = new ModeParameter("Mode", "Dortware", List.of("Dortware"));
    public final NumberParameter swingSpeed = new NumberParameter("Speed", 1.0, 1.0, 15.0, 0.5);

    public Animations() {
        super("Animations");
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(Animations.class);
    }

    public static Animations itz() {
        return ModuleManager.get(Animations.class);
    }
}
