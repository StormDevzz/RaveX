package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;

@ModuleInfo(name = "ViewLock", category = "net.minecraft.world.entity.player.Player")
public class ViewLock extends ravex.modules.Module {
public final BooleanParameter lockYaw = new BooleanParameter("LockYaw", true);
    public final BooleanParameter lockPitch = new BooleanParameter("LockPitch", true);
    public final ModeParameter mode = new ModeParameter("Mode", "Freeze",
            java.util.List.of("Freeze", "Smooth", "net.minecraft.core.Direction"));
    public final NumberParameter smoothSpeed = new NumberParameter("SmoothSpeed", 0.3, 0.05, 1.0, 0.05);
    public final NumberParameter sensitivity = new NumberParameter("Sensitivity", 1.0, 0.1, 3.0, 0.1);
    public final NumberParameter savedYaw = new NumberParameter("SavedYaw", 0.0, -180.0, 180.0, 1.0);
    public final NumberParameter savedPitch = new NumberParameter("SavedPitch", 0.0, -90.0, 90.0, 1.0);

    private float targetYaw = 0;
    private float targetPitch = 0;
    private boolean hasTarget = false;
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
        return getEnabled() && "net.minecraft.core.Direction".equals(mode.getValue());
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ViewLock").getEnabled();
    }

    public static ViewLock itz() {
        return ravex.manager.ModuleManager.delegate(ViewLock.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}