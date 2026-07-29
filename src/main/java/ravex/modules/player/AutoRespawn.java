package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.event.Subscribe;
import ravex.event.player.DeathEvent;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.network.NetworkUtility;
import ravex.modules.Modules;

@Module(name = "AutoRespawn", category = "net.minecraft.world.entity.player.Player")
public class AutoRespawn {
    @Parameter(name = "ShowDeathScreen")
    public boolean showDeathScreen = false;
    private long deathTime = 0;
    private boolean dead = false;

    @Subscribe
    public void onDeath(DeathEvent event) {
        if (!Modules.enabled(AutoRespawn.class) || !event.isSelf()) return;
        dead = true;
        deathTime = System.currentTimeMillis();
    }
    public void onDisable() {
        deathTime = 0;
        dead = false;
    }
    public void onTick() {
        if (!Modules.enabled(AutoRespawn.class) || !dead) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;
        if (showDeathScreen) return;
        NetworkUtility.sendRespawn();
        dead = false;
        deathTime = 0;
    }
}
