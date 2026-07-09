package ravex.manager;

import net.minecraft.client.Minecraft;

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
        return rotating ? yaw : (Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getYRot() : 0);
    }

    public float getPitch() {
        return rotating ? pitch : (Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getXRot() : 0);
    }

    public boolean isRotating() {
        return rotating;
    }
}
