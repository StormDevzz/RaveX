package ravex.modules.movement;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.BooleanParameter;
public class Speed extends Module {
    public static boolean cancelVertical = false;
    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla",
        java.util.List.of("Vanilla", "Strafe", "NCP", "NCPStrict", "Matrix", "Grim", "GrimStrict"));
    public final NumberParameter speed = new NumberParameter("Speed", 1.5, 0.5, 5.0, 0.1);
    public final BooleanParameter strafeJump = new BooleanParameter("StrafeJump", true);
<<<<<<< HEAD
    public final BooleanParameter autoJump = new BooleanParameter("AutoJump", true);
    public final NumberParameter speedLimit = new NumberParameter("SpeedLimit", 0.28, 0.1, 1.0, 0.01);
    public final NumberParameter grimBoost = new NumberParameter("GrimBoost", 1.0, 0.1, 2.0, 0.1);

=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private Speed() {
        super("Speed");
        strafeJump.setVisible(() -> "Strafe".equals(mode.getValue()));
        grimBoost.setVisible(() -> "Grim".equals(mode.getValue()) || "GrimStrict".equals(mode.getValue()));
        autoJump.setVisible(() -> !"GrimStrict".equals(mode.getValue()) && !"Grim".equals(mode.getValue()));
        speedLimit.setVisible(() -> !"GrimStrict".equals(mode.getValue()));
        speed.setVisible(() -> !"GrimStrict".equals(mode.getValue()));
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(Speed.class);
    }
    public static Speed itz() {
        return ModuleManager.get(Speed.class);
    }
}