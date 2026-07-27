package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;

import java.util.List;
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
        var mc = MinecraftWrapper.getInstance();
        if (mc.player != null) {
            savedYaw = mc.player.getYRot();
            savedPitch = mc.player.getXRot();
        }
    }
    public void restoreRotation() {
        if (!Modules.enabled(NoRotate.class)) return;
        var mc = MinecraftWrapper.getInstance();
        if (mc.player != null) {
            if ("Strict".equals(mode)) {
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




}