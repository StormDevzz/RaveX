package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.parameter.NumberParameter;
public class AutoReconnect extends Module {
<<<<<<< HEAD
    public final NumberParameter delay = new NumberParameter("Delay", 3.0, 0.0, 30.0, 1.0);
=======
    public static final AutoReconnect INSTANCE = new AutoReconnect();
    public final NumberParameter delay = new NumberParameter("Delay(s)", 3.0, 0.0, 30.0, 1.0);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private static ServerData lastServer = null;
    private static boolean pendingAutoReconnect = false;
    private static long reconnectAt = 0;

    public static void recordServer(ServerData server) {
        if (server != null) lastServer = server;
    }
    public static ServerData getLastServer() {
        return lastServer;
    }
    public static boolean hasLastServer() {
        return lastServer != null;
    }
    public void scheduleAutoReconnect() {
        if (!getEnabled() || !hasLastServer()) return;
        pendingAutoReconnect = true;
        reconnectAt = System.currentTimeMillis() + (long)(delay.getValue() * 1000);
    }
    @Override
    public void onTick() {
        if (!pendingAutoReconnect) return;
        if (System.currentTimeMillis() < reconnectAt) return;
        pendingAutoReconnect = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) return;
        reconnect(mc);
    }
    public static void reconnect(Minecraft mc) {
        if (!hasLastServer()) return;
        ServerAddress addr = ServerAddress.parseString(lastServer.ip);
        ConnectScreen.startConnecting(new TitleScreen(), mc, addr, lastServer, false, null);
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(AutoReconnect.class);
    }

    public static AutoReconnect itz() {
        return ModuleManager.get(AutoReconnect.class);
    }
}
