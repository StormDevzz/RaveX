package ravex.modules.movement;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
public class Timer extends Module {
<<<<<<< HEAD
=======
    public static final Timer INSTANCE = new Timer();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public static float multiplier = 1.0f;
    public final NumberParameter speed = new NumberParameter("Speed", 2.0, 1.0, 20.0, 0.5);
    public final BooleanParameter strafeFix = new BooleanParameter("StrafeFix", true);

    @Override
    public void onTick() {
        multiplier = speed.getValue().floatValue();
    }
    @Override
    protected void onDisable() {
        multiplier = 1.0f;
    }
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(Timer.class);
    }
    public static Timer itz() {
        return ModuleManager.get(Timer.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
