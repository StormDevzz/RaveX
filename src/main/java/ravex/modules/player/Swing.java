package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

import java.util.List;

public class Swing extends Module {
    public static final Swing INSTANCE = new Swing();

    public final ModeParameter mode = new ModeParameter("Mode", "1.8", List.of("1.8", "1.12.2", "Custom"));
    public final NumberParameter duration = new NumberParameter("Duration", 6, 1, 20, 1);
    public final ModeParameter swingPath = new ModeParameter("Swing Path", "Normal", List.of("Normal", "Smooth", "Bounce", "Reverse"));
    public final NumberParameter swingCurve = new NumberParameter("Swing Curve", 1.0, 0.1, 5.0, 0.1);
    public final NumberParameter progressCap = new NumberParameter("Progress Cap", 1.0, 0.0, 1.0, 0.05);
    public final NumberParameter progressFloor = new NumberParameter("Progress Floor", 0.0, 0.0, 1.0, 0.05);
    public final BooleanParameter noEquip = new BooleanParameter("No Equip", false);

    private Swing() {
        super("Swing", Category.PLAYER);
        addParameter(mode);
        addParameter(duration);
        addParameter(swingPath);
        addParameter(swingCurve);
        addParameter(progressCap);
        addParameter(progressFloor);
        addParameter(noEquip);

        duration.setVisible(() -> "Custom".equals(mode.getValue()));
        swingPath.setVisible(() -> "Custom".equals(mode.getValue()));
        swingCurve.setVisible(() -> "Custom".equals(mode.getValue()));
        progressCap.setVisible(() -> "Custom".equals(mode.getValue()));
        progressFloor.setVisible(() -> "Custom".equals(mode.getValue()));
        noEquip.setVisible(() -> "Custom".equals(mode.getValue()));
    }
}
