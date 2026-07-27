package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "ViewLock", category = "net.minecraft.world.entity.player.Player")
public class ViewLock {
    @Parameter(name = "LockYaw")
    public boolean lockYaw = true;
    @Parameter(name = "LockPitch")
    public boolean lockPitch = true;
    @Parameter(name = "Mode", modes = {"Freeze", "Smooth", "net.minecraft.core.Direction"})
    public String mode = "Freeze";
    @Parameter(name = "SmoothSpeed", min = 0.05, max = 1.0, step = 0.05)
    public double smoothSpeed = 0.3;
    @Parameter(name = "Sensitivity", min = 0.1, max = 3.0, step = 0.1)
    public double sensitivity = 1.0;
    @Parameter(name = "SavedYaw", min = -180.0, max = 180.0, step = 1.0)
    public double savedYaw = 0.0;
    @Parameter(name = "SavedPitch", min = -90.0, max = 90.0, step = 1.0)
    public double savedPitch = 0.0;

    private float targetYaw = 0;
    private float targetPitch = 0;
    private boolean hasTarget = false;
    public void onEnable() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player != null) {
            targetYaw = mc.player.getYRot();
            targetPitch = mc.player.getXRot();
            hasTarget = true;
        }
    }

    public void saveCurrentAngle() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player != null) {
            savedYaw = mc.player.getYRot();
            savedPitch = mc.player.getXRot();
            targetYaw = mc.player.getYRot();
            targetPitch = mc.player.getXRot();
            hasTarget = true;
        }
    }

    public float getTargetYaw() {
        return targetYaw;
    }

    public float getTargetPitch() {
        return targetPitch;
    }

    public boolean shouldLockYaw(double yRot, double xRot) {
        return Modules.enabled(ViewLock.class) && lockYaw;
    }

    public boolean shouldLockPitch(double yRot, double xRot) {
        return Modules.enabled(ViewLock.class) && lockPitch;
    }

    public float getSensitivity() {
        return (float) sensitivity;
    }

    public float getSmoothSpeed() {
        return (float) smoothSpeed;
    }

    public boolean isSmoothMode() {
        return Modules.enabled(ViewLock.class) && "Smooth".equals(mode);
    }

    public boolean isDirectionMode() {
        return Modules.enabled(ViewLock.class) && "net.minecraft.core.Direction".equals(mode);
    }






}