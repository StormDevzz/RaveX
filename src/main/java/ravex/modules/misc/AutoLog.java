package ravex.modules.misc;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
<<<<<<< HEAD
import ravex.utility.misc.MobUtility;
public class AutoLog extends Module {
=======
public class AutoLog extends Module {
    public static final AutoLog INSTANCE = new AutoLog();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final BooleanParameter onLowHealth = new BooleanParameter("LowHealth", true);
    public final NumberParameter healthLimit = new NumberParameter("MinHP", 6.0, 1.0, 20.0, 0.5);
    public final BooleanParameter onPlayerNearby = new BooleanParameter("PlayerNearby", false);
    public final NumberParameter playerRange = new NumberParameter("Range", 16.0, 4.0, 64.0, 1.0);

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (onLowHealth.getValue() && mc.player.getHealth() <= healthLimit.getValue()) {
            disconnect("LowHealthTriggered(" + mc.player.getHealth() + " HP)");
            return;
        }
        for (Player other : mc.level.players()) {
            if (other == mc.player) continue;
<<<<<<< HEAD
            double dist = MobUtility.distanceToPlayer(other);
=======
            double dist = mc.player.distanceTo(other);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (onPlayerNearby.getValue() && dist <= playerRange.getValue()) {
                disconnect("Player " + other.getGameProfile().name() + " is too close (" + String.format("%.1f", dist) + "m)");
                return;
            }
        }
    }
    private void disconnect(String reason) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().getConnection().disconnect(net.minecraft.network.chat.Component.literal("§c[RaveX AutoLog] §f" + reason));
        }
        setEnabled(false);
    }

    public static AutoLog itz() {
        return ModuleManager.get(AutoLog.class);
    }
}
