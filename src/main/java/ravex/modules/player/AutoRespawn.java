package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import ravex.event.Subscribe;
import ravex.event.player.DeathEvent;

@ModuleInfo(name = "AutoRespawn", category = "Player")
public class AutoRespawn extends ravex.modules.Module {
public final ravex.parameter.BooleanParameter showDeathScreen = new ravex.parameter.BooleanParameter("ShowDeathScreen", false);
    private long deathTime = 0;
    private boolean dead = false;

    @Subscribe
    public void onDeath(DeathEvent event) {
        if (!getEnabled() || !event.isSelf()) return;
        dead = true;
        deathTime = System.currentTimeMillis();
    }
    protected void onDisable() {
        deathTime = 0;
        dead = false;
    }
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
        return ravex.manager.ModuleManager.INSTANCE.getByName("AutoRespawn").getEnabled();
    }
    public static AutoRespawn itz() {
        return ravex.manager.ModuleManager.delegate(AutoRespawn.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}