package ravex.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.phys.Vec3;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
import ravex.event.network.PacketEvent;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.mixin.network.AccessorServerboundMovePlayerPacket;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.List;

public class Blink extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Normal",
            List.of("Normal", "Packet", "Grim", "NCP"));
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
    private int flushIndex = 0;
    private Vec3 flushStartPos = null;
    private double flushTotalHPos = 0.0;
    private int idleTicker = 0;
    private long flushStartTime = 0L;

    private static final int MAX_PACKETS_PER_TICK = 4;
    private static final int IDLE_INTERVAL = 3;
    private static final double MAX_MOVE_PER_PACKET = 0.35;

    private Blink() {
        super("Blink");
        maxTicks.setVisible(() -> "Grim".equals(mode.getValue()));
        autoDisableTicks.setVisible(() -> "Grim".equals(mode.getValue()) || "NCP".equals(mode.getValue()));
        cancelOnShift.setVisible(() -> "Grim".equals(mode.getValue()) || "NCP".equals(mode.getValue()));
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

        boolean isGrim = "Grim".equals(mode.getValue());
        boolean isNcp = "NCP".equals(mode.getValue());

        if (isGrim || isNcp) {
            if (cancelOnShift.getValue() && mc.options.keyShift.isDown()) {
                setEnabled(false);
                return;
            }

            tickCounter++;

            if (tickCounter >= autoDisableTicks.getValue()) {
                setEnabled(false);
                return;
            }

            if (flushing) {
                continueFlush();
                return;
            }

            bufferTicks++;

            if (isNcp) {
                idleTicker++;
                if (idleTicker >= IDLE_INTERVAL) {
                    sendIdleMove();
                    idleTicker = 0;
                }
            }

            if (bufferTicks >= maxTicks.getValue() || packetBuffer.size() >= limit.getValue().intValue()) {
                startFlush();
            }
        }
    }

    public boolean shouldCancel(Packet<?> packet) {
        if (!getEnabled()) return false;

        String modeVal = mode.getValue();

        if ("Grim".equals(modeVal) || "NCP".equals(modeVal)) {
            if (flushing) return false;
            if (packet instanceof ServerboundPongPacket) return false;
            if (packet instanceof ServerboundAcceptTeleportationPacket) return false;

            if ("NCP".equals(modeVal) && packet instanceof ServerboundPlayerInputPacket) {
                return false;
            }

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

    private void sendIdleMove() {
        if (packetBuffer.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(mc.player.onGround(), mc.player.horizontalCollision));
    }

    private void startFlush() {
        flushing = true;
        flushIndex = 0;
        flushStartTime = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            flushStartPos = mc.player.position();
        } else {
            flushStartPos = startPos != null ? startPos : Vec3.ZERO;
        }

        if ("NCP".equals(mode.getValue())) {
            flushTotalHPos = 0.0;
            for (Packet<?> p : packetBuffer) {
                if (p instanceof ServerboundMovePlayerPacket move && move.hasPosition()) {
                    AccessorServerboundMovePlayerPacket accessor = (AccessorServerboundMovePlayerPacket) move;
                    Vec3 pktPos = new Vec3(accessor.getX(), accessor.getY(), accessor.getZ());
                    if (flushTotalHPos == 0.0) {
                        flushTotalHPos = flushStartPos.distanceTo(pktPos);
                    }
                }
            }
        }

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
    }

    private void continueFlush() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) {
            finishFlush();
            return;
        }

        boolean isNcp = "NCP".equals(mode.getValue());

        int sentThisTick = 0;

        while (flushIndex < packetBuffer.size() && sentThisTick < MAX_PACKETS_PER_TICK) {
            Packet<?> p = packetBuffer.get(flushIndex);

            if (isNcp && p instanceof ServerboundMovePlayerPacket move && move.hasPosition() && flushStartPos != null) {
                double t = (double) flushIndex / Math.max(1, packetBuffer.size());
                Vec3 currentPlayerPos = mc.player.position();
                double dx = currentPlayerPos.x - flushStartPos.x;
                double dy = currentPlayerPos.y - flushStartPos.y;
                double dz = currentPlayerPos.z - flushStartPos.z;

                double px = flushStartPos.x + dx * t;
                double py = flushStartPos.y + dy * t;
                double pz = flushStartPos.z + dz * t;

                if (flushIndex > 0) {
                    Packet<?> prev = packetBuffer.get(flushIndex - 1);
                    if (prev instanceof ServerboundMovePlayerPacket prevMove) {
                        AccessorServerboundMovePlayerPacket prevAccessor = (AccessorServerboundMovePlayerPacket) prevMove;
                        double prevX = prevAccessor.getX();
                        double prevY = prevAccessor.getY();
                        double prevZ = prevAccessor.getZ();
                        double stepDist = Math.sqrt(
                                Math.pow(px - prevX, 2) +
                                Math.pow(py - prevY, 2) +
                                Math.pow(pz - prevZ, 2)
                        );
                        if (stepDist > MAX_MOVE_PER_PACKET) {
                            int extraSteps = (int) Math.ceil(stepDist / MAX_MOVE_PER_PACKET);
                            for (int s = 1; s < extraSteps; s++) {
                                double st = (double) s / extraSteps;
                                double sx = prevX + (px - prevX) * st;
                                double sy = prevY + (py - prevY) * st;
                                double sz = prevZ + (pz - prevZ) * st;
                                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                                        sx, sy, sz, move.isOnGround(), move.horizontalCollision()
                                ));
                            }
                        }
                    }
                }

                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                        px, py, pz, move.isOnGround(), move.horizontalCollision()
                ));
            } else {
                mc.player.connection.send(p);
            }

            flushIndex++;
            sentThisTick++;
        }

        if (flushIndex >= packetBuffer.size()) {
            finishFlush();
        }
    }

    private void finishFlush() {
        packetBuffer.clear();
        startPos = null;
        bufferTicks = 0;
        flushing = false;
        flushIndex = 0;
        flushStartPos = null;
        flushTotalHPos = 0.0;
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
        flushIndex = 0;
        flushStartPos = null;
        flushTotalHPos = 0.0;
        idleTicker = 0;
        flushStartTime = 0L;
    }

    @Override
    protected void onDisable() {
        if (!packetBuffer.isEmpty()) {
            startFlush();
            flushing = true;
            flushIndex = 0;
            flushStartTime = System.currentTimeMillis();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                flushStartPos = mc.player.position();
            }
            for (int i = 0; i < packetBuffer.size(); i++) {
                continueFlush();
            }
        }
    }
}
