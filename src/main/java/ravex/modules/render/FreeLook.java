package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "FreeLook", category = "Render")
public class FreeLook implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"net.minecraft.world.entity.player.Player", "net.minecraft.client.Camera"})
    public String mode = "net.minecraft.world.entity.player.Player";
    private float lookYaw = 0.0f;
    private float lookPitch = 0.0f;
    private int originalPerspective = 0;
    public void onEnable() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player != null) {
            lookYaw = mc.player.getYRot();
            lookPitch = mc.player.getXRot();
            originalPerspective = mc.options.getCameraType().ordinal();
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        }
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.options != null) {
            var types = net.minecraft.client.CameraType.values();
            if (originalPerspective >= 0 && originalPerspective < types.length) {
                mc.options.setCameraType(types[originalPerspective]);
            }
        }
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
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("FreeLook").getEnabled();
    }

    public static FreeLook itz() {
        return ravex.manager.ModuleManager.delegate(FreeLook.class);
    }


}