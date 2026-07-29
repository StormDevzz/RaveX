package ravex.utility.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import ravex.mcwrapper.MinecraftWrapper;

public class MovementUtility {
    private static Minecraft mc() { return MinecraftWrapper.getInstance(); }

    public static void strafe(double speed) {
        if (mc().player == null) return;
        double yaw = Math.toRadians(mc().player.getYRot());
        Vec3 vel = mc().player.getDeltaMovement();
        Vec2 moveInput = mc().player.input.getMoveVector();
        double forward = moveInput.y;
        double strafe = moveInput.x;
        if (forward == 0 && strafe == 0) {
            mc().player.setDeltaMovement(0, vel.y, 0);
            return;
        }
        double mx = -Math.sin(yaw) * speed;
        double mz = Math.cos(yaw) * speed;
        if (forward < 0) {
            mx *= -0.5;
            mz *= -0.5;
        }
        if (strafe != 0) {
            double sr = yaw + Math.PI / 2 * (strafe > 0 ? -1 : 1);
            mx += -Math.sin(sr) * speed * 0.4;
            mz += Math.cos(sr) * speed * 0.4;
        }
        mc().player.setDeltaMovement(mx, vel.y, mz);
    }
}
