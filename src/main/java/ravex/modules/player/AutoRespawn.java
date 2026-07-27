package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import ravex.event.Subscribe;
import ravex.event.player.DeathEvent;
import ravex.mcwrapper.MinecraftWrapper;

@ModuleInfo(name = "AutoRespawn", category = "net.minecraft.world.entity.player.Player")
public class AutoRespawn implements ModuleAccess {
    @Parameter(name = "ShowDeathScreen")
    public boolean showDeathScreen = false;
    private long deathTime = 0;
    private boolean dead = false;

    @Subscribe
    public void onDeath(DeathEvent event) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("AutoRespawn").getEnabled() || !event.isSelf()) return;
        dead = true;
        deathTime = System.currentTimeMillis();
    }
    public void onDisable() {
        deathTime = 0;
        dead = false;
    }
    public void onTick() {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("AutoRespawn").getEnabled() || !dead) return;
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;
        if (showDeathScreen) return;
        mc.getConnection().send(new ServerboundClientCommandPacket(
            ServerboundClientCommandPacket.Action.PERFORM_RESPAWN
        ));
        dead = false;
        deathTime = 0;
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AutoRespawn").getEnabled();
    }
    public static AutoRespawn itz() {
        return ravex.manager.ModuleManager.delegate(AutoRespawn.class);
    }


}