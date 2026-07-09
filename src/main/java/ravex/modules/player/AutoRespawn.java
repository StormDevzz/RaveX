package ravex.modules.player;
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import ravex.event.Subscribe;
import ravex.event.player.DeathEvent;
import ravex.modules.Module;
public class AutoRespawn extends Module {
    public final ravex.parameter.BooleanParameter showDeathScreen = new ravex.parameter.BooleanParameter("ShowDeathScreen", false);
    private long deathTime = 0;
    private boolean dead = false;

    @Subscribe
    public void onDeath(DeathEvent event) {
        if (!getEnabled() || !event.isSelf()) return;
        dead = true;
        deathTime = System.currentTimeMillis();
    }

    @Override
    protected void onDisable() {
        deathTime = 0;
        dead = false;
    }

    @Override
    public void onTick() {
        if (!getEnabled() || !dead) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (showDeathScreen.getValue()) return;
        mc.getConnection().send(new ServerboundClientCommandPacket(
            ServerboundClientCommandPacket.Action.PERFORM_RESPAWN
        ));
        dead = false;
        deathTime = 0;
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(AutoRespawn.class);
    }
    public static AutoRespawn itz() {
        return ModuleManager.get(AutoRespawn.class);
    }

}