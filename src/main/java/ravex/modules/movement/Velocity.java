package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;

/**
 * Velocity — modifies knockback received by the player.
 *
 * Modes:
 *   Cancel — zero out all horizontal knockback (and optionally vertical).
 *   Matrix — reduce knockback with slight randomization (bypass Matrix AC).
 *   NCP    — reduce horizontal, keep vertical (bypass NoCheatPlus).
 */
public class Velocity extends Module {
    public static final Velocity INSTANCE = new Velocity();

    public final ModeParameter mode       = new ModeParameter("Mode", "Cancel",
            List.of("Cancel", "Matrix", "NCP"));
    public final NumberParameter horizontal = new NumberParameter("Horizontal", 0.0, 0.0, 1.0, 0.05);
    public final NumberParameter vertical   = new NumberParameter("Vertical",   0.0, 0.0, 1.0, 0.05);
    public final BooleanParameter explosion = new BooleanParameter("Explosion", true);

    private Velocity() {
        super("Velocity", Category.MOVEMENT);
        addParameter(mode);
        addParameter(horizontal);
        addParameter(vertical);
        addParameter(explosion);
        // Horizontal/vertical sliders only relevant in NCP/Matrix modes
        horizontal.setVisible(() -> !mode.getValue().equals("Cancel"));
        vertical.setVisible(() -> !mode.getValue().equals("Cancel"));
    }
}
