package ravex.utility.render.animate;

import net.minecraft.world.phys.Vec3;

public class SlideAnimation {
    private double currentX, currentY, currentZ;
    private boolean initialized = false;

    public Vec3 update(double targetX, double targetY, double targetZ, double speed) {
        if (!initialized) {
            currentX = targetX;
            currentY = targetY;
            currentZ = targetZ;
            initialized = true;
        } else {
            currentX += (targetX - currentX) * speed;
            currentY += (targetY - currentY) * speed;
            currentZ += (targetZ - currentZ) * speed;
        }
        return new Vec3(currentX, currentY, currentZ);
    }

    public void reset() {
        initialized = false;
    }
}
