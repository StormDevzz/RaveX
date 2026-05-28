package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import net.minecraft.client.Minecraft;

/**
 * FreeLook Module
 * Decouples camera rotation in third-person view with wall-collision protections.
 */
public class FreeLook extends Module {
    public static final FreeLook INSTANCE = new FreeLook();

    private float lookYaw = 0.0f;
    private float lookPitch = 0.0f;
    private int originalPerspective = 0; // 0 = FIRST_PERSON

    private FreeLook() {
        super("FreeLook", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            lookYaw = mc.player.getYRot();
            lookPitch = mc.player.getXRot();
            originalPerspective = mc.options.getCameraType().ordinal();
            // Force 3rd person back perspective
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        }
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            // Restore perspective
            var types = net.minecraft.client.CameraType.values();
            if (originalPerspective >= 0 && originalPerspective < types.length) {
                mc.options.setCameraType(types[originalPerspective]);
            }
        }
    }

    public void turn(double yRot, double xRot) {
        // Direct, crisp looking controls matching vanilla mouse handler feeling exactly
        lookYaw += (float) (yRot * 0.15D);
        lookPitch += (float) (xRot * 0.15D);
        
        // Clamp pitch to avoid turning upside down
        lookPitch = Math.max(-90.0f, Math.min(90.0f, lookPitch));
    }

    public float getLookYaw() {
        return lookYaw;
    }

    public float getLookPitch() {
        return lookPitch;
    }
}
