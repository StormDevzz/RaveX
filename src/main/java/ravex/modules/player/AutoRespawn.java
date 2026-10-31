package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.event.Subscribe;
import ravex.event.player.DeathEvent;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.network.NetworkUtility;

@Module(name = "AutoRespawn", category = "Player")
public class AutoRespawn {
    private boolean dead = false;

    @Subscribe
    public void onDeath(DeathEvent event) {
        if (!event.isSelf()) return;
        dead = true;
    }
    public void onDisable() {
        dead = false;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;
        // respawn the tick the death screen appears
        if (mc.getCurrentScreen() instanceof net.minecraft.client.gui.screens.DeathScreen) {
            mc.getPlayer().respawn();
            mc.setScreen(null);
            dead = false;
            return;
        }
        if (!dead) return;
        NetworkUtility.sendRespawn();
        dead = false;
    }
}
