package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.network.NetworkUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "IllegalDisconnect", category = "Misc")
public class IllegalDisconnect {
    @Parameter(name = "Mode", modes = {"InvalidPacket", "Chat", "Quit"})
    public String mode = "InvalidPacket";

    private long sentAt = 0;

    public void onEnable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getConnection() == null) {
            Modules.setEnabled(IllegalDisconnect.class, false);
            return;
        }
        switch (mode) {
            case "Quit" -> {
                NetworkUtility.disconnect("RaveX");
                Modules.setEnabled(IllegalDisconnect.class, false);
                return;
            }
            // overlong chat, the server kicks on its length check
            case "Chat" -> NetworkUtility.sendChat("x".repeat(300));
            default -> writeBrokenFrame(mc);
        }
        sentAt = System.currentTimeMillis();
    }

    public void onTick() {
        if (sentAt == 0) return;
        if (System.currentTimeMillis() - sentAt < 1000) return;
        sentAt = 0;
        if (MinecraftWrapper.getWrapper().getConnection() != null) {
            NetworkUtility.disconnect("RaveX");
        }
        Modules.setEnabled(IllegalDisconnect.class, false);
    }

    // raw netty write, decoder throws before plugins see it
    private void writeBrokenFrame(MinecraftWrapper mc) {
        var connection = mc.getConnection().getConnection();
        var channel = ((ravex.mixin.network.AccessorConnection) connection).getChannel();
        if (channel == null || !channel.isActive()) return;
        channel.write(io.netty.buffer.Unpooled.buffer(0));
        channel.writeAndFlush(io.netty.buffer.Unpooled.wrappedBuffer(new byte[]{0x7f}));
    }
}
