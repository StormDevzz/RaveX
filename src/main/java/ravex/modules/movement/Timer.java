package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "Timer", category = "Movement")
public class Timer {
    @Parameter(name = "Mode", modes = {"Default", "Verus"})
    public String mode = "Default";
    @Parameter(name = "Speed", min = 1.0, max = 20.0, step = 0.5)
    public double speed = 2.0;
    @Parameter(name = "PulseInterval", min = 2.0, max = 40.0, step = 1.0, visible = "mode=Verus")
    public double pulseInterval = 8.0;
    @Parameter(name = "PulseDuration", min = 1.0, max = 10.0, step = 1.0, visible = "mode=Verus")
    public double pulseDuration = 2.0;
    @Parameter(name = "StrafeFix")
    public boolean strafeFix = true;

    private int tick = 0;
    private double currentSpeed;

    public void onEnable() {
        tick = 0;
        currentSpeed = 1.0;
    }

    public void onDisable() {
        currentSpeed = 1.0;
    }

    public void onTick() {
        String m = mode;
        if ("Verus".equals(m)) {
            tick++;
            int interval = (int) pulseInterval;
            int duration = (int) pulseDuration;
            if (tick <= duration) {
                currentSpeed = speed;
            } else {
                currentSpeed = 1.0;
            }
            if (tick >= interval) {
                tick = 0;
            }
        } else {
            currentSpeed = speed;
        }
    }

    public double getEffectiveSpeed() {
        return currentSpeed;
    }
}
