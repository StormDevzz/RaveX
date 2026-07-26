package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.PhysicUtility;

import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
@ModuleInfo(name = "RidingHelper", category = "Movement")
public class RidingHelper extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Normal", List.of("Normal", "Custom"));
    public final NumberParameter speed = new NumberParameter("Speed", 2.0, 1.0, 5.0, 0.1);
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        net.minecraft.world.entity.Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) return;
        double mult = mode.getValue().equals("Custom") ? speed.getValue() : 2.0;
        net.minecraft.world.phys.Vec3 motion = vehicle.getDeltaMovement();
        vehicle.setDeltaMovement(motion.x * mult, motion.y, motion.z * mult);
    }
    public static RidingHelper itz() {
        return ravex.manager.ModuleManager.delegate(RidingHelper.class);
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