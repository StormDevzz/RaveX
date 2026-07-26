package ravex.modules.misc;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.MobUtility;
@ModuleInfo(name = "AutoLog", category = "Misc")
public class AutoLog extends ravex.modules.Module {
public final BooleanParameter onLowHealth = new BooleanParameter("LowHealth", true);
    public final NumberParameter healthLimit = new NumberParameter("MinHP", 6.0, 1.0, 20.0, 0.5);
    public final BooleanParameter onPlayerNearby = new BooleanParameter("PlayerNearby", false);
    public final NumberParameter playerRange = new NumberParameter("Range", 16.0, 4.0, 64.0, 1.0);
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (onLowHealth.getValue() && mc.player.getHealth() <= healthLimit.getValue()) {
            disconnect("LowHealthTriggered(" + mc.player.getHealth() + " HP)");
            return;
        }
        for (net.minecraft.world.entity.player.Player other : mc.level.players()) {
            if (other == mc.player) continue;
            double dist = MobUtility.distanceToPlayer(other);
            if (onPlayerNearby.getValue() && dist <= playerRange.getValue()) {
                disconnect("net.minecraft.world.entity.player.Player " + other.getGameProfile().name() + " is too close (" + String.format("%.1f", dist) + "m)");
                return;
            }
        }
    }
    private void disconnect(String reason) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().getConnection().disconnect(net.minecraft.network.chat.Component.literal("§c[RaveX AutoLog] §f" + reason));
        }
        enabled = false;
    }

    public static AutoLog itz() {
        return ravex.manager.ModuleManager.delegate(AutoLog.class);
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