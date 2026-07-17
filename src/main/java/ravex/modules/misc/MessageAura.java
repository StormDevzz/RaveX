package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.StringParameter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MessageAura extends Module {
    public final NumberParameter range = new NumberParameter("Range", 5.0, 2.0, 20.0, 0.5);
    public final NumberParameter delay = new NumberParameter("Delay", 5.0, 1.0, 60.0, 1.0);
    public final StringParameter message = new StringParameter("Message", "Sup %s");
    public final BooleanParameter oncePerPlayer = new BooleanParameter("OncePerPlayer", false);
    public final BooleanParameter ignoreFriends = new BooleanParameter("IgnoreFriends", true);

    private final Set<UUID> messagedPlayers = new HashSet<>();
    private final Set<UUID> recentlyMessaged = new HashSet<>();
    private int cooldownTimer = 0;

    @Override
    protected void onEnable() {
        messagedPlayers.clear();
        recentlyMessaged.clear();
        cooldownTimer = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        if (me == null || mc.level == null || me.connection == null) return;

        if (cooldownTimer > 0) {
            cooldownTimer--;
            return;
        }

        double r = range.getValue();
        double rSq = r * r;

        for (Player player : mc.level.players()) {
            if (player == me) continue;
            if (player.isRemoved() || !player.isAlive()) continue;

            UUID uuid = player.getUUID();

            if (oncePerPlayer.getValue() && messagedPlayers.contains(uuid)) continue;
            if (recentlyMessaged.contains(uuid)) continue;
            if (ignoreFriends.getValue() && isFriend(uuid)) continue;

            double distSq = me.distanceToSqr(player);
            if (distSq > rSq) continue;

            String name = player.getGameProfile().name();
            String msg = message.getValue().replace("%s", name);
            me.connection.sendChat(msg);

            messagedPlayers.add(uuid);
            recentlyMessaged.add(uuid);
            cooldownTimer = (int) (delay.getValue() * 20.0);
            return;
        }
    }

    private boolean isFriend(UUID uuid) {
        var friendManager = ravex.manager.FriendManager.INSTANCE;
        if (friendManager == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        Player player = mc.level.getPlayerByUUID(uuid);
        if (player == null) return false;
        return friendManager.isFriend(player.getGameProfile().name());
    }

    public static MessageAura itz() {
        return ModuleManager.get(MessageAura.class);
    }
}
