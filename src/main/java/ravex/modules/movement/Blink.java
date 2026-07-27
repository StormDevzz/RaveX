package ravex.modules.movement;
import ravex.utility.network.NetworkUtility;

import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.PhysicUtility;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
import ravex.event.network.PacketEvent;

import ravex.mixin.network.AccessorServerboundMovePlayerPacket;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "Blink", category = "Movement")
public class Blink {
    @Parameter(name = "Mode", modes = {"Normal", "Packet", "Grim", "NCP"})
    public String mode = "Normal";
    @Parameter(name = "Limit", min = 5.0, max = 200.0, step = 5.0)
    public double limit = 30.0;
    @Parameter(name = "MaxTicks", min = 1.0, max = 20.0, step = 1.0)
    public double maxTicks = 4.0;
    @Parameter(name = "AutoDisable", min = 10.0, max = 400.0, step = 5.0)
    public double autoDisableTicks = 60.0;
    @Parameter(name = "CancelOnShift")
    public boolean cancelOnShift = true;
    @Parameter(name = "OnSpot")
    public boolean onSpot = false;

    private final List<net.minecraft.network.protocol.Packet<?>> packetBuffer = new ArrayList<>();
    private int tickCounter = 0;
    private int bufferTicks = 0;
    private net.minecraft.world.phys.Vec3 startPos = null;
    private boolean flushing = false;
    private int flushIndex = 0;
    private net.minecraft.world.phys.Vec3 flushStartPos = null;
    private double flushTotalHPos = 0.0;
    private int idleTicker = 0;
    private long flushStartTime = 0L;

    private static final int MAX_PACKETS_PER_TICK = 4;
    private static final int IDLE_INTERVAL = 3;
    private static final double MAX_MOVE_PER_PACKET = 0.35;

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!event.isSend()) return;
        if (shouldCancel(event.getPacket())) {
            event.setCancelled(true);
        }
    }

    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!Modules.enabled(Blink.class)) return;
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        boolean isGrim = "Grim".equals(mode);
        boolean isNcp = "NCP".equals(mode);

        if (isGrim || isNcp) {
            if (cancelOnShift && mc.options.keyShift.isDown()) {
                Modules.setEnabled(Blink.class, false);
                return;
            }

            tickCounter++;

            if (tickCounter >= autoDisableTicks) {
                Modules.setEnabled(Blink.class, false);
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

            if (bufferTicks >= maxTicks || packetBuffer.size() >= (int) limit) {
                startFlush();
            }
        }
    }

    public boolean shouldCancel(net.minecraft.network.protocol.Packet<?> packet) {
        if (!Modules.enabled(Blink.class)) return false;

        String modeVal = mode;

        if ("Grim".equals(modeVal) || "NCP".equals(modeVal)) {
            if (flushing) return false;
            if (packet instanceof net.minecraft.network.protocol.common.ServerboundPongPacket) return false;
            if (packet instanceof net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket) return false;

            if ("NCP".equals(modeVal) && NetworkUtility.isInputPacket(packet)) {
                return false;
            }

            if (packetBuffer.size() >= (int) limit) return true;

            if (startPos == null && packet instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket move && move.hasPosition()) {
                var mc = MinecraftWrapper.getInstance();
                if (mc.player != null) {
                    startPos = mc.player.position();
                }
            }

            packetBuffer.add(packet);
            return true;
        }

        if ("Packet".equals(modeVal) && !(packet instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket)) return false;
        if (packetBuffer.size() >= (int) limit) return true;
        if (packet instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket) {
            packetBuffer.add(packet);
            return true;
        }
        return false;
    }

    private void sendIdleMove() {
        if (packetBuffer.isEmpty()) return;
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly(mc.player.onGround(), mc.player.horizontalCollision));
    }

    private void startFlush() {
        flushing = true;
        flushIndex = 0;
        flushStartTime = System.currentTimeMillis();
        var mc = MinecraftWrapper.getInstance();
        if (mc.player != null) {
            flushStartPos = mc.player.position();
        } else {
            flushStartPos = startPos != null ? startPos : net.minecraft.world.phys.Vec3.ZERO;
        }

        if ("NCP".equals(mode)) {
            flushTotalHPos = 0.0;
            for (net.minecraft.network.protocol.Packet<?> p : packetBuffer) {
                if (p instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket move && move.hasPosition()) {
                    AccessorServerboundMovePlayerPacket accessor = (AccessorServerboundMovePlayerPacket) move;
                    net.minecraft.world.phys.Vec3 pktPos = new net.minecraft.world.phys.Vec3(accessor.getX(), accessor.getY(), accessor.getZ());
                    if (flushTotalHPos == 0.0) {
                        flushTotalHPos = flushStartPos.distanceTo(pktPos);
                    }
                }
            }
        }

        if (startPos != null && onSpot) {
            for (int i = 0; i < packetBuffer.size(); i++) {
                net.minecraft.network.protocol.Packet<?> p = packetBuffer.get(i);
                if (p instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket move && move.hasPosition()) {
                    packetBuffer.set(i, new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                            startPos.x, startPos.y, startPos.z,
                            move.isOnGround(), move.horizontalCollision()
                    ));
                }
            }
        }
    }

    private void continueFlush() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.player.connection == null) {
            finishFlush();
            return;
        }

        boolean isNcp = "NCP".equals(mode);

        int sentThisTick = 0;

        while (flushIndex < packetBuffer.size() && sentThisTick < MAX_PACKETS_PER_TICK) {
            net.minecraft.network.protocol.Packet<?> p = packetBuffer.get(flushIndex);

            if (isNcp && p instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket move && move.hasPosition() && flushStartPos != null) {
                double t = (double) flushIndex / Math.max(1, packetBuffer.size());
                net.minecraft.world.phys.Vec3 currentPlayerPos = mc.player.position();
                double dx = currentPlayerPos.x - flushStartPos.x;
                double dy = currentPlayerPos.y - flushStartPos.y;
                double dz = currentPlayerPos.z - flushStartPos.z;

                double px = flushStartPos.x + dx * t;
                double py = flushStartPos.y + dy * t;
                double pz = flushStartPos.z + dz * t;

                if (flushIndex > 0) {
                    net.minecraft.network.protocol.Packet<?> prev = packetBuffer.get(flushIndex - 1);
                    if (prev instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket prevMove) {
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
                                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                                        sx, sy, sz, move.isOnGround(), move.horizontalCollision()
                                ));
                            }
                        }
                    }
                }

                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
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




    public void onEnable() {
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
    public void onDisable() {
        if (!packetBuffer.isEmpty()) {
            startFlush();
            flushing = true;
            flushIndex = 0;
            flushStartTime = System.currentTimeMillis();
            var mc = MinecraftWrapper.getInstance();
            if (mc.player != null) {
                flushStartPos = mc.player.position();
            }
            for (int i = 0; i < packetBuffer.size(); i++) {
                continueFlush();
            }
        }
    }


}