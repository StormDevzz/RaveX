package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import ravex.utility.player.PlayerUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "AutoLog", category = "Misc")
public class AutoLog {
    @Parameter(name = "LowHealth")
    public boolean onLowHealth = true;
    @Parameter(name = "MinHP", min = 1.0, max = 20.0, step = 0.5)
    public double healthLimit = 6.0;
    @Parameter(name = "PlayerNearby")
    public boolean onPlayerNearby = false;
    @Parameter(name = "Range", min = 4.0, max = 64.0, step = 1.0)
    public double playerRange = 16.0;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;
        if (onLowHealth && PlayerUtility.getHealth(player) <= healthLimit) {
            disconnect("LowHealthTriggered(" + PlayerUtility.getHealth(player) + " HP)");
            return;
        }
        for (var other : mc.getLevel().players()) {
            if (other == player) continue;
            double dist = EntityUtility.distanceToPlayer(other);
            if (onPlayerNearby && dist <= playerRange) {
                disconnect("net.minecraft.world.entity.player.Player " + other.getGameProfile().name() + " is too close (" + String.format("%.1f", dist) + "m)");
                return;
            }
        }
    }
    private void disconnect(String reason) {
        var mc = MinecraftWrapper.getWrapper();
        var connection = mc.getConnection();
        if (connection != null) {
            connection.getConnection().disconnect(net.minecraft.network.chat.Component.literal("§c[RaveX AutoLog] §f" + reason));
        }
        Modules.setEnabled(AutoLog.class, false);
    }
}
