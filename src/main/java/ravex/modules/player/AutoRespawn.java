package ravex.modules.player;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
public class AutoRespawn extends Module {
    public static final AutoRespawn INSTANCE = new AutoRespawn();
    public final BooleanParameter instant = new BooleanParameter("Instant", true);
    public final NumberParameter delay = new NumberParameter("Delay (ms)", 0, 0, 5000, 100);
    public final BooleanParameter showDeathScreen = new BooleanParameter("Show Death Screen", false);
    private long deathTime = 0;

    @Override
    protected void onDisable() {
        deathTime = 0;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
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
    }
}
