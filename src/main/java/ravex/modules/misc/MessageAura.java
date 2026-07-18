package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.StringParameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MessageAura extends Module {
    public final NumberParameter range = new NumberParameter("Range", 10.0, 1.0, 100.0, 0.5);
    public final StringParameter message = new StringParameter("Message", "hello");
    public final NumberParameter delay = new NumberParameter("Delay", 5.0, 0.0, 60.0, 1.0);
    public final ModeParameter mode = new ModeParameter("Mode", "Once", List.of("Once", "Repeat"));

    private final Map<UUID, Long> sentPlayers = new HashMap<>();
    private long lastSentTime = 0;

    private MessageAura() {
        super("MessageAura");
    }

    @Override
    protected void onEnable() {
        sentPlayers.clear();
        lastSentTime = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        long now = System.currentTimeMillis();
        long delayMs = (long) (delay.getValue() * 1000);
        boolean isOnce = "Once".equals(mode.getValue());

        for (Player target : mc.level.players()) {
            if (target == mc.player) continue;
            if (mc.player.distanceTo(target) > range.getValue()) continue;

            if (isOnce) {
                if (sentPlayers.containsKey(target.getUUID())) continue;
            } else {
                Long lastSent = sentPlayers.get(target.getUUID());
                if (lastSent != null && (now - lastSent) < delayMs) continue;
            }

            if ((now - lastSentTime) < delayMs) continue;

            String msg = message.getValue();
            if (mc.player.connection != null) {
                mc.player.connection.sendChat(msg);
            }

            sentPlayers.put(target.getUUID(), now);
            lastSentTime = now;
        }
    }

    public static MessageAura itz() {
        return ModuleManager.get(MessageAura.class);
    }
}
