package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.MobUtility;
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
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (onLowHealth && mc.player.getHealth() <= healthLimit) {
            disconnect("LowHealthTriggered(" + mc.player.getHealth() + " HP)");
            return;
        }
        for (net.minecraft.world.entity.player.Player other : mc.level.players()) {
            if (other == mc.player) continue;
            double dist = MobUtility.distanceToPlayer(other);
            if (onPlayerNearby && dist <= playerRange) {
                disconnect("net.minecraft.world.entity.player.Player " + other.getGameProfile().name() + " is too close (" + String.format("%.1f", dist) + "m)");
                return;
            }
        }
    }
    private void disconnect(String reason) {
        var mc = MinecraftWrapper.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().getConnection().disconnect(net.minecraft.network.chat.Component.literal("§c[RaveX AutoLog] §f" + reason));
        }
        Modules.setEnabled(AutoLog.class, false);
    }




}