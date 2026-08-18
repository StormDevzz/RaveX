package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.Random;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "NoRotate", category = "Movement")
public class NoRotate {
    @Parameter(name = "Mode", modes = {"Normal", "Strict"})
    public String mode = "Normal";
    private float savedYaw;
    private float savedPitch;
    private final Random random = new Random();

    public void saveRotation() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player != null) {
            savedYaw = player.getYRot();
            savedPitch = player.getXRot();
        }
    }
    public void restoreRotation() {
        if (!Modules.enabled(NoRotate.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player != null) {
            if ("Strict".equals(mode)) {
                float yaw = savedYaw;
                float pitch = savedPitch;
                if (yaw % 90 == 0) yaw += (random.nextFloat() - 0.5f) * 0.1f;
                if (yaw % 180 == 0) yaw += (random.nextFloat() - 0.5f) * 0.1f;
                if (pitch == 0 || pitch == 90 || pitch == -90) pitch += (random.nextFloat() - 0.5f) * 0.1f;
                player.setYRot(yaw);
                player.setXRot(pitch);
            } else {
                player.setYRot(savedYaw);
                player.setXRot(savedPitch);
            }
        }
    }
}
