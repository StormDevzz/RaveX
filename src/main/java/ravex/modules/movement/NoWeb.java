package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;

/**
 * NoWeb — allows the player to bypass cobweb slowdowns.
 * 
 * Modes:
 *   Custom   — fully customize horizontal and vertical cobweb speed multipliers.
 *   Positive — complete bypass, move at standard normal speed inside cobwebs.
 *   Positive2 — legit bypass (0.6x horizontal speed, 0.5x vertical speed), bypasses many anticheats.
 *   Positive3 — highly legit bypass (0.4x horizontal speed, 0.3x vertical speed), very smooth and safe.
 */
public class NoWeb extends Module {
    public static final NoWeb INSTANCE = new NoWeb();

    public final ModeParameter mode = new ModeParameter("Mode", "Positive",
            List.of("Custom", "Positive", "Positive2", "Positive3"));

    public final NumberParameter horizontalSpeed = new NumberParameter("Horizontal Speed", 1.0, 0.25, 1.0, 0.05);
    public final NumberParameter verticalSpeed   = new NumberParameter("Vertical Speed", 1.0, 0.05, 1.0, 0.05);

    private NoWeb() {
        super("NoWeb", Category.MOVEMENT);
        addParameter(mode);
        addParameter(horizontalSpeed);
        addParameter(verticalSpeed);

        // Make sliders visible only when using Custom mode
        horizontalSpeed.setVisible(() -> mode.getValue().equals("Custom"));
        verticalSpeed.setVisible(() -> mode.getValue().equals("Custom"));
    }
}
