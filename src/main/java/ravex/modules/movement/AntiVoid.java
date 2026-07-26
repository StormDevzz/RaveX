package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.NumberParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.movement.VoidUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import ravex.utility.misc.PhysicUtility;
import java.util.List;
@ModuleInfo(name = "AntiVoid", category = "Movement")
public class AntiVoid extends ravex.modules.Module {
public final NumberParameter fallDistance = new NumberParameter("Distance", 5.0, 1.0, 10.0, 0.5);
    public final ModeParameter mode = new ModeParameter("Mode", "Teleport", List.of("Teleport", "Bounce"));
    private net.minecraft.world.phys.Vec3 lastOnGroundPos = null;
    protected void onEnable() {
        lastOnGroundPos = null;
    }
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        if (p.onGround()) {
            lastOnGroundPos = p.position();
        } else if (lastOnGroundPos != null && lastOnGroundPos.y - p.getY() > fallDistance.getValue()) {
            if (VoidUtility.isFallingIntoVoid(p)) {
                if (mode.getValue().equals("Teleport")) {
                    p.setDeltaMovement(0, 0, 0);
                    p.teleportTo(lastOnGroundPos.x, lastOnGroundPos.y, lastOnGroundPos.z);
                } else if (mode.getValue().equals("Bounce")) {
                    p.setDeltaMovement(p.getDeltaMovement().x, 0.45, p.getDeltaMovement().z);
                }
            }
        }
    }
    public static AntiVoid itz() {
        return ravex.manager.ModuleManager.delegate(AntiVoid.class);
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