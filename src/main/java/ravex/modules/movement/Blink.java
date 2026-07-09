package ravex.modules.movement;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
<<<<<<< HEAD
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
=======
import ravex.modules.Category;
import ravex.modules.Module;
import java.util.ArrayList;
import java.util.List;
public class Blink extends Module {
    public static final Blink INSTANCE = new Blink();
    private final List<Packet<?>> packetBuffer = new ArrayList<>();

    public boolean shouldCancel(Packet<?> packet) {
        if (!getEnabled()) return false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (packet instanceof ServerboundMovePlayerPacket) {
            packetBuffer.add(packet);
            return true;
        }
        return false;
    }
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(Blink.class);
    }
    public static Blink itz() {
        return ModuleManager.get(Blink.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
