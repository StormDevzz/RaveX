package ravex.modules.movement;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.BooleanParameter;
public class Speed extends Module {
    public static final Speed INSTANCE = new Speed();
    public static boolean cancelVertical = false;
    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla",
        java.util.List.of("Vanilla", "Strafe", "NCP", "Matrix"));
    public final NumberParameter speed = new NumberParameter("Speed", 1.5, 0.5, 5.0, 0.1);
    public final BooleanParameter strafeJump = new BooleanParameter("StrafeJump", true);
    private Speed() {
        super("Speed");
        strafeJump.setVisible(() -> "Strafe".equals(mode.getValue()));
    }
}
