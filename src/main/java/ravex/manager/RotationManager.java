package ravex.manager;

import ravex.mcwrapper.MinecraftWrapper;

public class RotationManager {
    public static final RotationManager INSTANCE = new RotationManager();

    private float yaw;
    private float pitch;
    private boolean rotating;

    private RotationManager() {}

    public void setRotations(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.rotating = true;
    }

    public void reset() {
        this.rotating = false;
    }

    public float getYaw() {
        var p = MinecraftWrapper.getWrapper().getPlayer();
        return rotating ? yaw : (p != null ? p.getYRot() : 0);
    }

    public float getPitch() {
        var p = MinecraftWrapper.getWrapper().getPlayer();
        return rotating ? pitch : (p != null ? p.getXRot() : 0);
    }

    public boolean isRotating() {
        return rotating;
    }
}
