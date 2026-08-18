package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.CameraType;
@Module(name = "FreeLook", category = "Render")
public class FreeLook {
    @Parameter(name = "Mode", modes = {"Player", "Camera"})
    public String mode = "Player";
    private float lookYaw = 0.0f;
    private float lookPitch = 0.0f;
    private int originalPerspective = 0;
    public void onEnable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() != null) {
            lookYaw = mc.getPlayer().getYRot();
            lookPitch = mc.getPlayer().getXRot();
            originalPerspective = mc.getOptions().getCameraType().ordinal();
            mc.getOptions().setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getOptions() != null) {
            var types = CameraType.values();
            if (originalPerspective >= 0 && originalPerspective < types.length) {
                mc.getOptions().setCameraType(types[originalPerspective]);
            }
        }
    }
    public boolean isCameraMode() {
        return "Camera".equals(mode);
    }
    public boolean isPlayerMode() {
        return "Player".equals(mode);
    }
    public void turn(double yRot, double xRot) {
        lookYaw += (float) yRot;
        lookPitch += (float) xRot;
        lookPitch = Math.max(-90.0f, Math.min(90.0f, lookPitch));
    }
    public float getLookYaw() {
        return lookYaw;
    }
    public float getLookPitch() {
        return lookPitch;
    }
}
