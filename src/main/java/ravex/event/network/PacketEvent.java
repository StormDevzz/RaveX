package ravex.event.network;

import ravex.event.CancellableEvent;
import net.minecraft.network.protocol.Packet;

public class PacketEvent extends CancellableEvent {
    public enum PacketAction { SEND, RECEIVE }
    private final PacketAction action;
    private Packet<?> packet;

    public PacketEvent(PacketAction action, Packet<?> packet) {
        this.action = action;
        this.packet = packet;
    }

    public PacketAction getAction() { return action; }
    public Packet<?> getPacket() { return packet; }
    public void setPacket(Packet<?> packet) { this.packet = packet; }

    public boolean isSend() { return action == PacketAction.SEND; }
    public boolean isReceive() { return action == PacketAction.RECEIVE; }
}
