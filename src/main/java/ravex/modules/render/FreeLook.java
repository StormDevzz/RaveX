package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import net.minecraft.client.Minecraft;

import java.util.List;

public class FreeLook extends Module {
    public static final FreeLook INSTANCE = new FreeLook();

    public final ModeParameter mode = new ModeParameter("Mode", "Player", List.of("Player", "Camera"));

    private float lookYaw = 0.0f;
    private float lookPitch = 0.0f;
    private int originalPerspective = 0;

    private FreeLook() {
        super("FreeLook", Category.RENDER);
        addParameter(mode);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            lookYaw = mc.player.getYRot();
            lookPitch = mc.player.getXRot();
            originalPerspective = mc.options.getCameraType().ordinal();
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        }
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
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
}
