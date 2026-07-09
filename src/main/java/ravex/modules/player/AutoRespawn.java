package ravex.modules.player;
<<<<<<< HEAD
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
=======
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
public class AutoRespawn extends Module {
    public static final AutoRespawn INSTANCE = new AutoRespawn();
    public final BooleanParameter instant = new BooleanParameter("Instant", true);
    public final NumberParameter delay = new NumberParameter("Delay(ms)", 0, 0, 5000, 100);
    public final BooleanParameter showDeathScreen = new BooleanParameter("ShowDeathScreen", false);
    private long deathTime = 0;

    @Override
    protected void onDisable() {
        deathTime = 0;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }
    @Override
    public void onTick() {
        if (!getEnabled() || !dead) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
<<<<<<< HEAD
        if (showDeathScreen.getValue()) return;
        mc.getConnection().send(new ServerboundClientCommandPacket(
            ServerboundClientCommandPacket.Action.PERFORM_RESPAWN
        ));
        dead = false;
        deathTime = 0;
=======
        if (!mc.player.isAlive()) {
            if (deathTime == 0) {
                deathTime = System.currentTimeMillis();
            }
            if (showDeathScreen.getValue()) return;
            long elapsed = System.currentTimeMillis() - deathTime;
            long requiredDelay = instant.getValue() ? 0 : delay.getValue().longValue();
            if (elapsed >= requiredDelay) {
                mc.getConnection().send(new ServerboundClientCommandPacket(
                    ServerboundClientCommandPacket.Action.PERFORM_RESPAWN
                ));
                deathTime = 0;
            }
        } else {
            deathTime = 0;
        }
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(AutoRespawn.class);
    }
    public static AutoRespawn itz() {
        return ModuleManager.get(AutoRespawn.class);
    }

}