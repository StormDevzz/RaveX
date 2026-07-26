package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;

@ModuleInfo(name = "ViewLock", category = "net.minecraft.world.entity.player.Player")
public class ViewLock implements ModuleAccess {
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            targetYaw = mc.player.getYRot();
            targetPitch = mc.player.getXRot();
            hasTarget = true;
        }
    }

    public void saveCurrentAngle() {
        Minecraft mc = Minecraft.getInstance();
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
        return ravex.manager.ModuleManager.INSTANCE.getByName("ViewLock").getEnabled() && lockYaw;
    }

    public boolean shouldLockPitch(double yRot, double xRot) {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ViewLock").getEnabled() && lockPitch;
    }

    public float getSensitivity() {
        return (float) sensitivity;
    }

    public float getSmoothSpeed() {
        return (float) smoothSpeed;
    }

    public boolean isSmoothMode() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ViewLock").getEnabled() && "Smooth".equals(mode);
    }

    public boolean isDirectionMode() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ViewLock").getEnabled() && "net.minecraft.core.Direction".equals(mode);
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ViewLock").getEnabled();
    }

    public static ViewLock itz() {
        return ravex.manager.ModuleManager.delegate(ViewLock.class);
    }


}