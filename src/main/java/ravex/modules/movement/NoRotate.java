package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;

import java.util.List;
import java.util.Random;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "NoRotate", category = "Movement")
public class NoRotate implements ModuleAccess {
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
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("NoRotate").getEnabled()) return;
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
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoRotate").getEnabled();
    }
    public static NoRotate itz() {
        return ravex.manager.ModuleManager.delegate(NoRotate.class);
    }


}