package ravex.modules.misc;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
public class AutoLog extends Module {
    public static final AutoLog INSTANCE = new AutoLog();
    public final BooleanParameter onLowHealth = new BooleanParameter("Low Health", true);
    public final NumberParameter healthLimit = new NumberParameter("Min HP", 6.0, 1.0, 20.0, 0.5);
    public final BooleanParameter onPlayerNearby = new BooleanParameter("Player Nearby", false);
    public final NumberParameter playerRange = new NumberParameter("Range", 16.0, 4.0, 64.0, 1.0);

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (onLowHealth.getValue() && mc.player.getHealth() <= healthLimit.getValue()) {
            disconnect("Low Health Triggered (" + mc.player.getHealth() + " HP)");
            return;
        }
        for (Player other : mc.level.players()) {
            if (other == mc.player) continue;
            double dist = mc.player.distanceTo(other);
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
}
