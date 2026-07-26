package ravex.modules.misc;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

import ravex.parameter.NumberParameter;
@ModuleInfo(name = "AutoReconnect", category = "Misc")
public class AutoReconnect extends ravex.modules.Module {
public final NumberParameter delay = new NumberParameter("Delay", 3.0, 0.0, 30.0, 1.0);
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
        return ravex.manager.ModuleManager.INSTANCE.getByName("AutoReconnect").getEnabled();
    }

    public static AutoReconnect itz() {
        return ravex.manager.ModuleManager.delegate(AutoReconnect.class);
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