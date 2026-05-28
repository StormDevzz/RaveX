package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;

/**
 * FreeCam Module
 * Spectator-mode camera flying with JNI-accelerated C++ physics and controls.
 */
public class FreeCam extends Module {
    public static final FreeCam INSTANCE = new FreeCam();

    // Public fields read by MixinCamera.java for frame-rate independent rendering
    public double x, y, z;
    public float yaw, pitch;
    public double prevX, prevY, prevZ;
    public float prevYaw, prevPitch;

    private double targetX, targetY, targetZ;

    public final NumberParameter speed = new NumberParameter("Speed", 0.5, 0.1, 2.0, 0.1);
    public final NumberParameter smoothness = new NumberParameter("Smoothness", 0.2, 0.0, 0.9, 0.05);

    private static boolean nativeAvailable = false;

    static {
        try {
            ravex.utility.misc.NativeLoader.load();
            nativeAvailable = true;
        } catch (Throwable t) {
            nativeAvailable = false;
        }
    }

    private FreeCam() {
        super("FreeCam", Category.RENDER);
        addParameter(speed);
        addParameter(smoothness);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            double startX = mc.player.getX();
            double startY = mc.player.getY() + mc.player.getEyeHeight();
            double startZ = mc.player.getZ();
            float startYaw = mc.player.getYRot();
            float startPitch = mc.player.getXRot();

            if (nativeAvailable) {
                try {
                    nativeReset(startX, startY, startZ, startYaw, startPitch);
                    syncFromNative();
                    return;
                } catch (UnsatisfiedLinkError e) {
                    nativeAvailable = false;
                }
            }

            // Java Fallback
            this.prevX = this.x = startX;
            this.prevY = this.y = startY;
            this.prevZ = this.z = startZ;
            this.prevYaw = this.yaw = startYaw;
            this.prevPitch = this.pitch = startPitch;

            this.targetX = this.x;
            this.targetY = this.y;
            this.targetZ = this.z;
        }
    }

    public void turn(double yRot, double xRot) {
        if (nativeAvailable) {
            try {
                nativeTurn(yRot, xRot);
                syncFromNative();
                return;
            } catch (UnsatisfiedLinkError e) {
                nativeAvailable = false;
            }
        }

        // Java Fallback (vanilla sensitivity behavior scaled by 0.15)
        this.yaw += (float) (yRot * 0.15D);
        this.pitch += (float) (xRot * 0.15D);
        this.pitch = Math.max(-90.0f, Math.min(90.0f, this.pitch));
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double moveSpeed = speed.getValue();
        double smoothVal = smoothness.getValue();

        boolean keyUp = mc.options.keyUp.isDown();
        boolean keyDown = mc.options.keyDown.isDown();
        boolean keyLeft = mc.options.keyLeft.isDown();
        boolean keyRight = mc.options.keyRight.isDown();
        boolean keyJump = mc.options.keyJump.isDown();
        boolean keyShift = mc.options.keyShift.isDown();

        if (nativeAvailable) {
            try {
                nativeUpdatePosition(keyUp, keyDown, keyLeft, keyRight, keyJump, keyShift, moveSpeed, smoothVal);
                syncFromNative();
                mc.player.setDeltaMovement(0, 0, 0);
                return;
            } catch (UnsatisfiedLinkError e) {
                nativeAvailable = false;
            }
        }

        // Java Fallback Tick Behavior
        this.prevX = this.x;
        this.prevY = this.y;
        this.prevZ = this.z;
        this.prevYaw = this.yaw;
        this.prevPitch = this.pitch;

        float f = this.yaw * ((float)Math.PI / 180F);
        double sinYaw = Math.sin(f);
        double cosYaw = Math.cos(f);

        double dx = 0;
        double dy = 0;
        double dz = 0;

        if (keyUp) {
            dx -= sinYaw * moveSpeed;
            dz += cosYaw * moveSpeed;
        }
        if (keyDown) {
            dx += sinYaw * moveSpeed;
            dz -= cosYaw * moveSpeed;
        }
        if (keyLeft) {
            dx -= cosYaw * moveSpeed;
            dz -= sinYaw * moveSpeed;
        }
        if (keyRight) {
            dx += cosYaw * moveSpeed;
            dz += sinYaw * moveSpeed;
        }
        if (keyJump) {
            dy += moveSpeed;
        }
        if (keyShift) {
            dy -= moveSpeed;
        }

        this.targetX += dx;
        this.targetY += dy;
        this.targetZ += dz;

        double factor = 1.0 - smoothVal;
        this.x += (this.targetX - this.x) * factor;
        this.y += (this.targetY - this.y) * factor;
        this.z += (this.targetZ - this.z) * factor;

        mc.player.setDeltaMovement(0, 0, 0);
    }

    public double[] getCorrectedRenderCoordinates(double partialTicks) {
        double[] output = new double[5];
        if (nativeAvailable) {
            try {
                nativeGetCorrected(partialTicks, output);
                return output;
            } catch (UnsatisfiedLinkError e) {
                nativeAvailable = false;
            }
        }

        // Java Fallback: buttery smooth positioning and angle-wrap check
        double renderX = this.prevX + (this.x - this.prevX) * partialTicks;
        double renderY = this.prevY + (this.y - this.prevY) * partialTicks;
        double renderZ = this.prevZ + (this.z - this.prevZ) * partialTicks;

        float diffYaw = this.yaw - this.prevYaw;
        while (diffYaw <= -180.0f) diffYaw += 360.0f;
        while (diffYaw > 180.0f) diffYaw -= 360.0f;
        float renderYaw = this.prevYaw + diffYaw * (float) partialTicks;

        float renderPitch = this.prevPitch + (this.pitch - this.prevPitch) * (float) partialTicks;

        output[0] = renderX;
        output[1] = renderY;
        output[2] = renderZ;
        output[3] = renderYaw;
        output[4] = renderPitch;
        return output;
    }

    private void syncFromNative() {
        if (nativeAvailable) {
            try {
                this.x = nativeGetX();
                this.y = nativeGetY();
                this.z = nativeGetZ();
                this.yaw = nativeGetYaw();
                this.pitch = nativeGetPitch();
                this.prevX = nativeGetPrevX();
                this.prevY = nativeGetPrevY();
                this.prevZ = nativeGetPrevZ();
                this.prevYaw = nativeGetPrevYaw();
                this.prevPitch = nativeGetPrevPitch();
            } catch (UnsatisfiedLinkError e) {
                nativeAvailable = false;
            }
        }
    }

    // ── Native Bindings ──────────────────────────────────────────────────────
    private static native void nativeReset(double x, double y, double z, float yaw, float pitch);
    private static native void nativeTurn(double yRot, double xRot);
    private static native void nativeUpdatePosition(boolean keyUp, boolean keyDown, boolean keyLeft, boolean keyRight, boolean keyJump, boolean keyShift, double speed, double smoothness);
    private static native void nativeGetCorrected(double partialTicks, double[] output);
    
    private static native double nativeGetX();
    private static native double nativeGetY();
    private static native double nativeGetZ();
    private static native float nativeGetYaw();
    private static native float nativeGetPitch();
    
    private static native double nativeGetPrevX();
    private static native double nativeGetPrevY();
    private static native double nativeGetPrevZ();
    private static native float nativeGetPrevYaw();
    private static native float nativeGetPrevPitch();
}
