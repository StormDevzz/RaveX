package ravex.event.render;

import ravex.event.CancellableEvent;

public class CameraEvent extends CancellableEvent {
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private final CameraMode mode;

    public enum CameraMode { FREE_CAM, FREELOOK, THIRD_PERSON, NORMAL }

    public CameraEvent(CameraMode mode, double x, double y, double z, float yaw, float pitch) {
        this.mode = mode;
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw; this.pitch = pitch;
    }

    public CameraMode getMode() { return mode; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public void setPosition(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    public void setRotation(float yaw, float pitch) { this.yaw = yaw; this.pitch = pitch; }
}
