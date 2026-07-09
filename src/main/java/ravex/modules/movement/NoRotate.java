package ravex.modules.movement;
import net.minecraft.client.Minecraft;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
<<<<<<< HEAD
import ravex.parameter.ModeParameter;
import java.util.List;
import java.util.Random;
public class NoRotate extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Normal", List.of("Normal", "Strict"));
    private float savedYaw;
    private float savedPitch;
    private final Random random = new Random();
=======
public class NoRotate extends Module {
    public static final NoRotate INSTANCE = new NoRotate();
    private float savedYaw;
    private float savedPitch;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

    public void saveRotation() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            savedYaw = mc.player.getYRot();
            savedPitch = mc.player.getXRot();
        }
    }
    public void restoreRotation() {
        if (!getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if ("Strict".equals(mode.getValue())) {
                float yaw = savedYaw;
                float pitch = savedPitch;
                if (yaw % 90 == 0) yaw += (random.nextFloat() - 0.5f) * 0.1f;
                if (yaw % 180 == 0) yaw += (random.nextFloat() - 0.5f) * 0.1f;
                if (pitch == 0 || pitch == 90 || pitch == -90) pitch += (random.nextFloat() - 0.5f) * 0.1f;
                mc.player.setYRot(yaw);
                mc.player.setXRot(pitch);
            } else {
                mc.player.setYRot(savedYaw);
                mc.player.setXRot(savedPitch);
            }
        }
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(NoRotate.class);
    }
    public static NoRotate itz() {
        return ModuleManager.get(NoRotate.class);
    }
}
