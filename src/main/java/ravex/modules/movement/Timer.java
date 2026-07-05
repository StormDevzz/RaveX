package ravex.modules.movement;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
public class Timer extends Module {
    public static final Timer INSTANCE = new Timer();
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
}
