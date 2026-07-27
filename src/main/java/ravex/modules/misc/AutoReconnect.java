package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import ravex.mcwrapper.MinecraftWrapper;

@ModuleInfo(name = "AutoReconnect", category = "Misc")
public class AutoReconnect implements ModuleAccess {
    @Parameter(name = "Delay", min = 0.0, max = 30.0, step = 1.0)
    public double delay = 3.0;
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
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("AutoReconnect").getEnabled() || !hasLastServer()) return;
        pendingAutoReconnect = true;
        reconnectAt = System.currentTimeMillis() + (long)(delay * 1000);
    }
    public void onTick() {
        if (!pendingAutoReconnect) return;
        if (System.currentTimeMillis() < reconnectAt) return;
        pendingAutoReconnect = false;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() != null) return;
        reconnect(mc);
    }
    public static void reconnect(MinecraftWrapper mc) {
        if (!hasLastServer()) return;
        ServerAddress addr = ServerAddress.parseString(lastServer.ip);
        ConnectScreen.startConnecting(new TitleScreen(), mc.getRaw(), addr, lastServer, false, null);
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AutoReconnect").getEnabled();
    }

    public static AutoReconnect itz() {
        return ravex.manager.ModuleManager.delegate(AutoReconnect.class);
    }


}