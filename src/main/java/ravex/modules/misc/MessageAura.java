package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.network.NetworkUtility;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "MessageAura", category = "Misc")
public class MessageAura {
    @Parameter(name = "Message")
    public String message = "sup retard :D";
    @Parameter(name = "Interval", min = 1.0, max = 60.0, step = 0.5)
    public double interval = 5.0;

    private long lastMessageTime;

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getConnection() == null) return;
        if (message == null || message.isEmpty()) return;

        if (System.currentTimeMillis() - lastMessageTime >= interval * 1000) {
            // a real packet, other players see it too
            if (message.startsWith("/")) {
                NetworkUtility.sendCommand(message.substring(1));
            } else {
                NetworkUtility.sendChat(message);
            }
            lastMessageTime = System.currentTimeMillis();
        }
    }

    public void onEnable() {
        lastMessageTime = System.currentTimeMillis();
    }
}
