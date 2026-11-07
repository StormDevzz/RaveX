package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.utility.network.NetworkUtility;

import ravex.modules.Modules;
@Module(name = "Handshake", category = "Player")
public class Handshake {
    @Parameter(name = "Mode", modes = {"Basic", "Forge", "Lunar", "Custom"})
    public String mode = "Basic";
    @Parameter(name = "Suffix", visible = "mode=Custom")
    public String hostSuffix = "ravex149";
    @Parameter(name = "Protocol", min = 47.0, max = 1000.0, step = 1.0)
    public double protocol = 767.0;

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(Handshake.class) || !event.isSend()) return;
        var packet = event.getPacket();
        if (NetworkUtility.isHandshakePacket(packet)) {
            String host = NetworkUtility.handshakeHost(packet);
            if (host == null) return;
            NetworkUtility.setHandshakeHost(packet, getSpoofedHost(host));
            NetworkUtility.setHandshakeProtocol(packet, getSpoofedProtocol(NetworkUtility.handshakeProtocol(packet)));
        }
    }

    public String getSpoofedHost(String originalHost) {
        if (!Modules.enabled(Handshake.class)) return originalHost;
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