package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.ModeParameter;
import net.minecraft.client.Minecraft;
import java.util.List;
@ModuleInfo(name = "FreeLook", category = "Render")
public class FreeLook extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Player", List.of("Player", "Camera"));
    private float lookYaw = 0.0f;
    private float lookPitch = 0.0f;
    private int originalPerspective = 0;
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            lookYaw = mc.player.getYRot();
            lookPitch = mc.player.getXRot();
            originalPerspective = mc.options.getCameraType().ordinal();
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        }
    }
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
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("FreeLook").getEnabled();
    }

    public static FreeLook itz() {
        return ravex.manager.ModuleManager.delegate(FreeLook.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}