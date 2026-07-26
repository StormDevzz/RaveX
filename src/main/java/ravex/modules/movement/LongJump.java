package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.List;
@ModuleInfo(name = "LongJump", category = "Movement")
public class LongJump extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Vanilla", List.of("Vanilla"));
    public final NumberParameter boost = new NumberParameter("Boost", 1.5, 1.0, 10.0, 0.1);
    public static boolean jumped = false;

    private LongJump() {
        
    }
    protected void onEnable() {
        jumped = false;
    }
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (mc.player.onGround()) {
            jumped = false;
        } else if (!jumped) {
            double speed = boost.getValue();
            Vec3 motion = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(motion.x * speed, motion.y + 0.05, motion.z * speed);
            jumped = true;
        }
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("LongJump").getEnabled();
    }
    public static LongJump itz() {
        return ravex.manager.ModuleManager.delegate(LongJump.class);
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