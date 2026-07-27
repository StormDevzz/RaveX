package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "Timer", category = "Movement")
public class Timer {
public static float multiplier = 1.0f;
    @Parameter(name = "Speed", min = 1.0, max = 20.0, step = 0.5)
    public double speed = 2.0;
    @Parameter(name = "StrafeFix")
    public boolean strafeFix = true;
    public void onTick() {
        multiplier = (float) speed;
    }
    public void onDisable() {
        multiplier = 1.0f;
    }




}