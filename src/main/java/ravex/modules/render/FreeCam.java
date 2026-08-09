package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;

import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.utility.movement.MoveUtility;
@Module(name = "FreeCam", category = "Render")
public class FreeCam {
public double x, y, z;
    public float yaw, pitch;
    public double prevX, prevY, prevZ;
    public float prevYaw, prevPitch;
    private double targetX, targetY, targetZ;
    private double frozenX, frozenY, frozenZ;
    private float frozenYaw, frozenPitch;
    @Parameter(name = "Speed", min = 0.1, max = 5.0, step = 0.1)
    public double speed = 0.5;
    @Parameter(name = "Freeze")
    public boolean freeze = true;
    @Parameter(name = "BlockInteract")
    public boolean blockInteract = true;
    @Parameter(name = "EntityInteract")
    public boolean entityInteract = true;
    @Parameter(name = "NoSwing")
    public boolean noSwing = false;
    public void onEnable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() != null) {
            double startX = mc.getPlayer().getX();
            double startY = mc.getPlayer().getY() + mc.getPlayer().getEyeHeight();
            double startZ = mc.getPlayer().getZ();
            float startYaw = mc.getPlayer().getYRot();
            float startPitch = mc.getPlayer().getXRot();
            frozenX = startX;
            frozenY = mc.getPlayer().getY();
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
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() != null) {
            MoveUtility.setMotion(0, 0, 0);
            if (freeze) {
                mc.getPlayer().setPos(frozenX, frozenY, frozenZ);
                mc.getPlayer().setYRot(frozenYaw);
                mc.getPlayer().setXRot(frozenPitch);
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
        if (!Modules.enabled(FreeCam.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;
        if (freeze) {
            mc.getPlayer().setYRot(frozenYaw);
            mc.getPlayer().setXRot(frozenPitch);
            mc.getPlayer().setPos(frozenX, frozenY, frozenZ);
        } else {
            float diff = this.yaw - frozenYaw;
            while (diff <= -180.0f) diff += 360.0f;
            while (diff > 180.0f) diff -= 360.0f;
            float clamped = Math.max(-90.0f, Math.min(90.0f, diff));
            mc.getPlayer().setYRot(frozenYaw + clamped);
            mc.getPlayer().setXRot(this.pitch);
        }
        double moveSpeed = speed;
        boolean keyUp = mc.getOptions().keyUp.isDown();
        boolean keyDown = mc.getOptions().keyDown.isDown();
        boolean keyLeft = mc.getOptions().keyLeft.isDown();
        boolean keyRight = mc.getOptions().keyRight.isDown();
        boolean keyJump = mc.getOptions().keyJump.isDown();
        boolean keyShift = mc.getOptions().keyShift.isDown();
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
        MoveUtility.setMotion(0, 0, 0);
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





}