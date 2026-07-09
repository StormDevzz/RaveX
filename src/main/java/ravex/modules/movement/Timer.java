package ravex.modules.movement;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
public class Timer extends Module {
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
    public static boolean maybeEnabled() {
        return maybeEnabled(Timer.class);
    }
    public static Timer itz() {
        return ModuleManager.get(Timer.class);
    }
}
