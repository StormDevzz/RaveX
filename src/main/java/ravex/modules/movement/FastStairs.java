package ravex.modules.movement;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import java.util.List;
public class FastStairs extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Simple", List.of("Simple", "Boost"));
    public final NumberParameter speed = new NumberParameter("Speed", 1.5, 1.0, 5.0, 0.1);
    public static double calculateClimbSpeed(String mode, double currentY, double speedFactor) {
        double baseSpeed = (currentY > 0.0) ? currentY : 0.15;
        if ("Boost".equals(mode)) {
            return baseSpeed * speedFactor * 1.35;
        }
        return baseSpeed * speedFactor;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.player.onClimbable()) {
            double currentY = mc.player.getDeltaMovement().y;
            if (currentY > 0 && (mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown())) {
                double newY = calculateClimbSpeed(mode.getValue(), currentY, speed.getValue());
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, newY, mc.player.getDeltaMovement().z);
            }
        }
    }
    public static FastStairs itz() {
        return ModuleManager.get(FastStairs.class);
    }
}
