package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.StringParameter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MessageAura extends Module {

    public final NumberParameter range = new NumberParameter("Range", 10.0, 1.0, 100.0, 1.0);
    public final StringParameter message = new StringParameter("Message", "Hello!");
    public final BooleanParameter onlyOnce = new BooleanParameter("OnlyOnce", true);
    public final BooleanParameter clientMessage = new BooleanParameter("ClientMessage", false);

    private final Set<UUID> messaged = new HashSet<>();

    public static boolean maybeEnabled() {
        return maybeEnabled(MessageAura.class);
    }

    public static MessageAura itz() {
        return ModuleManager.get(MessageAura.class);
    }

    @Override
    protected void onDisable() {
        messaged.clear();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        double rangeSq = range.getValue() * range.getValue();
        String msg = message.getValue();

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof Player target)) continue;
            if (target == mc.player) continue;
            if (target.isSpectator()) continue;

            UUID targetId = target.getUUID();
            if (onlyOnce.getValue() && messaged.contains(targetId)) continue;

            if (mc.player.distanceToSqr(target) <= rangeSq) {
                if (clientMessage.getValue()) {
                    mc.player.displayClientMessage(Component.literal(msg.replace("%player%", target.getName().getString())), false);
                } else {
                    mc.player.connection.sendChat(msg.replace("%player%", target.getName().getString()));
                }
                messaged.add(targetId);
            }
        }
    }
}
