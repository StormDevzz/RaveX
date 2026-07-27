package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.mixin.network.AccessorClientIntentionPacket;

import ravex.parameter.StringParameter;
import ravex.manager.LuaManager;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.LuaFunction;
import java.util.List;
import ravex.modules.Modules;
@Module(name = "Handshake", category = "net.minecraft.world.entity.player.Player")
public class Handshake {
    @Parameter(name = "Mode", modes = {"Basic", "Forge", "Lunar", "Custom"})
    public String mode = "Basic";
    @Parameter(name = "Suffix")
    public String hostSuffix = "\u0000LUNAR\u0000";
    @Parameter(name = "Protocol", min = 47.0, max = 1000.0, step = 1.0)
    public double protocol = 767.0;

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(Handshake.class) || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientIntentionPacket handshakePacket) {
            AccessorClientIntentionPacket accessor = (AccessorClientIntentionPacket) (Object) handshakePacket;
            accessor.setHostName(getSpoofedHost(handshakePacket.hostName()));
            accessor.setProtocolVersion(getSpoofedProtocol(handshakePacket.protocolVersion()));
        }
    }

    public String getSpoofedHost(String originalHost) {
        if (!Modules.enabled(Handshake.class)) return originalHost;
        LuaValue fn = LuaManager.INSTANCE.getGlobals().get("onHandshake");
        if (fn.isfunction()) {
            try {
                LuaValue[] args = { LuaValue.valueOf(originalHost), LuaValue.valueOf((int) (double) protocol) };
                org.luaj.vm2.Varargs res = ((LuaFunction) fn).invoke(LuaValue.varargsOf(args));
                if (res.narg() >= 1 && !res.arg(1).isnil()) {
                    return res.arg(1).tojstring();
                }
            } catch (Exception e) {
                System.err.println("[Lua Handshake Error] " + e.getMessage());
            }
        }
        String m = mode;
        switch (m) {
            case "Forge":
                return originalHost + "\u0000FML\u0000";
            case "Lunar":
                return originalHost + "\u0000LUNAR\u0000";
            case "Basic":
                return originalHost;
            default:
                return originalHost + hostSuffix;
        }
    }
    public int getSpoofedProtocol(int originalProtocol) {
        if (!Modules.enabled(Handshake.class)) return originalProtocol;
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
        String m = mode;
        switch (m) {
            case "Forge":
            case "Lunar":
                return 999;
            case "Basic":
                return originalProtocol;
            default:
                return (int) (double) protocol;
        }
    }




}