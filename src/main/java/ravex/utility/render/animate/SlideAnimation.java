package ravex.utility.render.animate;

import net.minecraft.world.phys.Vec3;

public class SlideAnimation {
    private double prevX, prevY, prevZ;
    private double currentX, currentY, currentZ;
    private boolean initialized = false;

    public Vec3 update(double targetX, double targetY, double targetZ, double speed) {
        if (!initialized) {
            currentX = targetX;
            currentY = targetY;
            currentZ = targetZ;
            prevX = targetX;
            prevY = targetY;
            prevZ = targetZ;
            initialized = true;
        } else {
            prevX = currentX;
            prevY = currentY;
            prevZ = currentZ;
            currentX += (targetX - currentX) * speed;
            currentY += (targetY - currentY) * speed;
            currentZ += (targetZ - currentZ) * speed;
        }
        return new Vec3(currentX, currentY, currentZ);
    }

    public Vec3 getPos(float partialTicks) {
        if (!initialized) return new Vec3(0, 0, 0);
        double x = prevX + (currentX - prevX) * partialTicks;
        double y = prevY + (currentY - prevY) * partialTicks;
        double z = prevZ + (currentZ - prevZ) * partialTicks;
        return new Vec3(x, y, z);
    }

    public void reset() {
        initialized = false;
    }
}
