package ravex.modules.player;

import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;

public class ViewLock extends Module {
    public final BooleanParameter lockYaw = new BooleanParameter("LockYaw", true);
    public final BooleanParameter lockPitch = new BooleanParameter("LockPitch", true);
    public final ModeParameter mode = new ModeParameter("Mode", "Freeze",
            java.util.List.of("Freeze", "Smooth", "Direction"));
    public final NumberParameter smoothSpeed = new NumberParameter("SmoothSpeed", 0.3, 0.05, 1.0, 0.05);
    public final NumberParameter sensitivity = new NumberParameter("Sensitivity", 1.0, 0.1, 3.0, 0.1);
    public final NumberParameter savedYaw = new NumberParameter("SavedYaw", 0.0, -180.0, 180.0, 1.0);
    public final NumberParameter savedPitch = new NumberParameter("SavedPitch", 0.0, -90.0, 90.0, 1.0);

    private float targetYaw = 0;
    private float targetPitch = 0;
    private boolean hasTarget = false;

    @Override
    protected void onEnable() {
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
            savedYaw.setValue((double) mc.player.getYRot());
            savedPitch.setValue((double) mc.player.getXRot());
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
        return getEnabled() && lockYaw.getValue();
    }

    public boolean shouldLockPitch(double yRot, double xRot) {
        return getEnabled() && lockPitch.getValue();
    }

    public float getSensitivity() {
        return sensitivity.getValue().floatValue();
    }

    public float getSmoothSpeed() {
        return smoothSpeed.getValue().floatValue();
    }

    public boolean isSmoothMode() {
        return getEnabled() && "Smooth".equals(mode.getValue());
    }

    public boolean isDirectionMode() {
        return getEnabled() && "Direction".equals(mode.getValue());
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(ViewLock.class);
    }

    public static ViewLock itz() {
        return ModuleManager.get(ViewLock.class);
    }
}
