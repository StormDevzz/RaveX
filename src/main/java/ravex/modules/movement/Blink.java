package ravex.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.world.phys.Vec3;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
import ravex.event.network.PacketEvent;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.List;

public class Blink extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Normal",
            List.of("Normal", "Packet", "Grim"));
    public final NumberParameter limit = new NumberParameter("Limit", 30.0, 5.0, 200.0, 5.0);
    public final NumberParameter maxTicks = new NumberParameter("MaxTicks", 4.0, 1.0, 20.0, 1.0);
    public final NumberParameter autoDisableTicks = new NumberParameter("AutoDisable", 60.0, 10.0, 400.0, 5.0);
    public final BooleanParameter cancelOnShift = new BooleanParameter("CancelOnShift", true);
    public final BooleanParameter onSpot = new BooleanParameter("OnSpot", false);

    private final List<Packet<?>> packetBuffer = new ArrayList<>();
    private int tickCounter = 0;
    private int bufferTicks = 0;
    private Vec3 startPos = null;
    private boolean flushing = false;

    private Blink() {
        super("Blink");
        maxTicks.setVisible(() -> "Grim".equals(mode.getValue()));
        autoDisableTicks.setVisible(() -> "Grim".equals(mode.getValue()));
        cancelOnShift.setVisible(() -> "Grim".equals(mode.getValue()));
        onSpot.setVisible(() -> "Grim".equals(mode.getValue()));
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!event.isSend()) return;
        if (shouldCancel(event.getPacket())) {
            event.setCancelled(true);
        }
    }

    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        if ("Grim".equals(mode.getValue())) {
            if (cancelOnShift.getValue() && mc.options.keyShift.isDown()) {
                setEnabled(false);
                return;
            }

            tickCounter++;

            if (tickCounter >= autoDisableTicks.getValue()) {
                setEnabled(false);
                return;
            }

            if (flushing) return;

            bufferTicks++;

            if (bufferTicks >= maxTicks.getValue() || packetBuffer.size() >= limit.getValue().intValue()) {
                flush();
            }
        }
    }

    public boolean shouldCancel(Packet<?> packet) {
        if (!getEnabled()) return false;

        String modeVal = mode.getValue();

        if ("Grim".equals(modeVal)) {
            if (flushing) return false;
            if (packet instanceof ServerboundPongPacket) return false;
            if (packet instanceof ServerboundAcceptTeleportationPacket) return false;
            if (packetBuffer.size() >= limit.getValue().intValue()) return true;

            if (startPos == null && packet instanceof ServerboundMovePlayerPacket move && move.hasPosition()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    startPos = mc.player.position();
                }
            }

            packetBuffer.add(packet);
            return true;
        }

        if ("Packet".equals(modeVal) && !(packet instanceof ServerboundMovePlayerPacket)) return false;
        if (packetBuffer.size() >= limit.getValue().intValue()) return true;
        if (packet instanceof ServerboundMovePlayerPacket) {
            packetBuffer.add(packet);
            return true;
        }
        return false;
    }

    private void flush() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;

        flushing = true;

        if (startPos != null && onSpot.getValue()) {
            for (int i = 0; i < packetBuffer.size(); i++) {
                Packet<?> p = packetBuffer.get(i);
                if (p instanceof ServerboundMovePlayerPacket move && move.hasPosition()) {
                    packetBuffer.set(i, new ServerboundMovePlayerPacket.Pos(
                            startPos.x, startPos.y, startPos.z,
                            move.isOnGround(), move.horizontalCollision()
                    ));
                }
            }
        }

        for (Packet<?> p : packetBuffer) {
            mc.player.connection.send(p);
        }
        packetBuffer.clear();
        startPos = null;
        bufferTicks = 0;
        flushing = false;
    }

    public int getBufferedCount() {
        return packetBuffer.size();
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
        tickCounter = 0;
        bufferTicks = 0;
        startPos = null;
        flushing = false;
    }

    @Override
    protected void onDisable() {
        if (!packetBuffer.isEmpty()) {
            flush();
        }
    }
}
