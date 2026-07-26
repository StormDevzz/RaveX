package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.mixin.network.AccessorClientIntentionPacket;

import ravex.parameter.StringParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.ModeParameter;
import ravex.manager.LuaManager;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.LuaFunction;
import java.util.List;
@ModuleInfo(name = "Handshake", category = "Player")
public class Handshake extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Basic",
            List.of("Basic", "Forge", "Lunar", "Custom"));
    public final StringParameter hostSuffix = new StringParameter("Suffix", "\u0000LUNAR\u0000");
    public final NumberParameter protocol = new NumberParameter("Protocol", 767.0, 47.0, 1000.0, 1.0);

    public Handshake() {
        hostSuffix.setVisible(() -> mode.getValue().equals("Custom"));
        protocol.setVisible(() -> mode.getValue().equals("Custom"));
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!getEnabled() || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientIntentionPacket handshakePacket) {
            AccessorClientIntentionPacket accessor = (AccessorClientIntentionPacket) (Object) handshakePacket;
            accessor.setHostName(getSpoofedHost(handshakePacket.hostName()));
            accessor.setProtocolVersion(getSpoofedProtocol(handshakePacket.protocolVersion()));
        }
    }

    public String getSpoofedHost(String originalHost) {
        if (!getEnabled()) return originalHost;
        LuaValue fn = LuaManager.INSTANCE.getGlobals().get("onHandshake");
        if (fn.isfunction()) {
            try {
                LuaValue[] args = { LuaValue.valueOf(originalHost), LuaValue.valueOf((int) protocol.getValue().doubleValue()) };
                org.luaj.vm2.Varargs res = ((LuaFunction) fn).invoke(LuaValue.varargsOf(args));
                if (res.narg() >= 1 && !res.arg(1).isnil()) {
                    return res.arg(1).tojstring();
                }
            } catch (Exception e) {
                System.err.println("[Lua Handshake Error] " + e.getMessage());
            }
        }
        String m = mode.getValue();
        switch (m) {
            case "Forge":
                return originalHost + "\u0000FML\u0000";
            case "Lunar":
                return originalHost + "\u0000LUNAR\u0000";
            case "Basic":
                return originalHost;
            default:
                return originalHost + hostSuffix.getValue();
        }
    }
    public int getSpoofedProtocol(int originalProtocol) {
        if (!getEnabled()) return originalProtocol;
        LuaValue fn = LuaManager.INSTANCE.getGlobals().get("onHandshake");
        if (fn.isfunction()) {
            try {
                LuaValue[] args = { LuaValue.valueOf(""), LuaValue.valueOf(originalProtocol) };
                org.luaj.vm2.Varargs res = ((LuaFunction) fn).invoke(LuaValue.varargsOf(args));
                if (res.narg() >= 2 && !res.arg(2).isnil()) {
                    return res.arg(2).toint();
                }
            } catch (Exception e) {
                System.err.println("[Lua Handshake Error] " + e.getMessage());
            }
        }
        String m = mode.getValue();
        switch (m) {
            case "Forge":
            case "Lunar":
                return 999;
            case "Basic":
                return originalProtocol;
            default:
                return (int) protocol.getValue().doubleValue();
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Handshake").getEnabled();
    }
    public static Handshake itz() {
        return ravex.manager.ModuleManager.delegate(Handshake.class);
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