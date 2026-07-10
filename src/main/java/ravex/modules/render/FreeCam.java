package ravex.modules.render;
<<<<<<< HEAD
<<<<<<< HEAD
=======
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
>>>>>>> 0ab37177398daa0e9880b2ec0d3ee76a2dbed416
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
public class FreeCam extends Module {
<<<<<<< HEAD
=======
    public static final FreeCam INSTANCE = new FreeCam();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public double x, y, z;
    public float yaw, pitch;
    public double prevX, prevY, prevZ;
    public float prevYaw, prevPitch;
    private double targetX, targetY, targetZ;
    private double frozenX, frozenY, frozenZ;
    private float frozenYaw, frozenPitch;
    public final NumberParameter speed = new NumberParameter("Speed", 0.5, 0.1, 5.0, 0.1);
    public final BooleanParameter freeze = new BooleanParameter("Freeze", true);
    public final BooleanParameter blockInteract = new BooleanParameter("BlockInteract", true);
    public final BooleanParameter entityInteract = new BooleanParameter("EntityInteract", true);
    public final BooleanParameter noSwing = new BooleanParameter("NoSwing", false);

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            double startX = mc.player.getX();
            double startY = mc.player.getY() + mc.player.getEyeHeight();
            double startZ = mc.player.getZ();
            float startYaw = mc.player.getYRot();
            float startPitch = mc.player.getXRot();
            frozenX = startX;
            frozenY = mc.player.getY();
            frozenZ = startZ;
            frozenYaw = startYaw;
            frozenPitch = startPitch;
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
    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.setDeltaMovement(0, 0, 0);
            if (freeze.getValue()) {
                mc.player.setPos(frozenX, frozenY, frozenZ);
                mc.player.setYRot(frozenYaw);
                mc.player.setXRot(frozenPitch);
            }
        }
    }
    public void turnMixin(double yRot, double xRot) {
        this.yaw += (float) yRot;
        this.pitch += (float) xRot;
        this.pitch = Math.max(-90.0f, Math.min(90.0f, this.pitch));
    }
    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (freeze.getValue()) {
            mc.player.setYRot(frozenYaw);
            mc.player.setXRot(frozenPitch);
            mc.player.setPos(frozenX, frozenY, frozenZ);
        } else {
            float diff = this.yaw - frozenYaw;
            while (diff <= -180.0f) diff += 360.0f;
            while (diff > 180.0f) diff -= 360.0f;
            float clamped = Math.max(-90.0f, Math.min(90.0f, diff));
            mc.player.setYRot(frozenYaw + clamped);
            mc.player.setXRot(this.pitch);
        }
        double moveSpeed = speed.getValue();
        boolean keyUp = mc.options.keyUp.isDown();
        boolean keyDown = mc.options.keyDown.isDown();
        boolean keyLeft = mc.options.keyLeft.isDown();
        boolean keyRight = mc.options.keyRight.isDown();
        boolean keyJump = mc.options.keyJump.isDown();
        boolean keyShift = mc.options.keyShift.isDown();
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
            dx += cosYaw * moveSpeed;
            dz += sinYaw * moveSpeed;
        }
        if (keyRight) {
            dx -= cosYaw * moveSpeed;
            dz -= sinYaw * moveSpeed;
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
        this.x += this.targetX - this.x;
        this.y += this.targetY - this.y;
        this.z += this.targetZ - this.z;
        mc.player.setDeltaMovement(0, 0, 0);
    }
    public double[] getCorrectedRenderCoordinates(double partialTicks) {
        double[] output = new double[5];
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
    public Vec3 getEyePosition(float tickDelta) {
        return new Vec3(
            this.prevX + (this.x - this.prevX) * tickDelta,
            this.prevY + (this.y - this.prevY) * tickDelta,
            this.prevZ + (this.z - this.prevZ) * tickDelta
        );
    }
    public record Vec3(double x, double y, double z) {}
    public static boolean maybeEnabled() {
        return maybeEnabled(FreeCam.class);
    }

    public static FreeCam itz() {
        return ModuleManager.get(FreeCam.class);
    }
}
