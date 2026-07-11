package ravex.modules.movement;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.List;
public class Blink extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Normal", List.of("Normal", "Packet"));
    public final NumberParameter limit = new NumberParameter("Limit", 30.0, 5.0, 100.0, 5.0);
    private final List<Packet<?>> packetBuffer = new ArrayList<>();

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!event.isSend()) return;
        if (shouldCancel(event.getPacket())) {
            event.setCancelled(true);
        }
    }

    public boolean shouldCancel(Packet<?> packet) {
        if (!getEnabled()) return false;
        if ("Packet".equals(mode.getValue()) && !(packet instanceof ServerboundMovePlayerPacket)) return false;
        if (packetBuffer.size() >= limit.getValue().intValue()) return true;
        if (packet instanceof ServerboundMovePlayerPacket) {
            packetBuffer.add(packet);
            return true;
        }
        return false;
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(Blink.class);
    }
    public static Blink itz() {
        return ModuleManager.get(Blink.class);
    }
    @Override
    protected void onEnable() {
        packetBuffer.clear();
    }
    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.connection != null) {
            for (Packet<?> p : packetBuffer) {
                mc.player.connection.send(p);
            }
        }
        packetBuffer.clear();
    }
}
