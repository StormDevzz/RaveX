package ravex.modules.movement;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import java.util.List;
import ravex.utility.nativelib.NativeLibrary;
public class FastStairs extends Module {
    public static final FastStairs INSTANCE = new FastStairs();
    public final ModeParameter mode = new ModeParameter("Mode", "Simple", List.of("Simple", "Boost"));
    public final NumberParameter speed = new NumberParameter("Speed", 1.5, 1.0, 5.0, 0.1);
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_faststairs");
    static {
        NATIVE.load();
    }
    public static native double nativeCalculateClimbSpeed(String mode, double currentY, double speedFactor);
    public static double calculateClimbSpeed(String mode, double currentY, double speedFactor) {
        if (NATIVE.isLoaded()) {
            try {
                return nativeCalculateClimbSpeed(mode, currentY, speedFactor);
            } catch (UnsatisfiedLinkError | Exception e) {
            }
        }
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
}
