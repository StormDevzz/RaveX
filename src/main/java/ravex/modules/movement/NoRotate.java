package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;

import ravex.parameter.ModeParameter;
import java.util.List;
import java.util.Random;
@ModuleInfo(name = "NoRotate", category = "Movement")
public class NoRotate extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Normal", List.of("Normal", "Strict"));
    private float savedYaw;
    private float savedPitch;
    private final Random random = new Random();

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
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoRotate").getEnabled();
    }
    public static NoRotate itz() {
        return ravex.manager.ModuleManager.delegate(NoRotate.class);
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