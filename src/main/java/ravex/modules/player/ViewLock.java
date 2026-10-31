package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.modules.Modules;

@Module(name = "ViewLock", category = "Player")
public class ViewLock {
    @Parameter(name = "LockYaw")
    public boolean lockYaw = true;
    @Parameter(name = "LockPitch")
    public boolean lockPitch = true;
    // how fast the unlocked axis reacts
    @Parameter(name = "Sensitivity", min = 0.1, max = 3.0, step = 0.1)
    public double sensitivity = 1.0;

    public boolean shouldLockYaw(double yRot, double xRot) {
        return Modules.enabled(ViewLock.class) && lockYaw;
    }

    public boolean shouldLockPitch(double yRot, double xRot) {
        return Modules.enabled(ViewLock.class) && lockPitch;
    }

    public float getSensitivity() {
        return (float) sensitivity;
    }
}
