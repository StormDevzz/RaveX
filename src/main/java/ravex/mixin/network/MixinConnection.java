package ravex.mixin.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.exploit.AntiHunger;
import ravex.modules.exploit.PacketCanceller;
import ravex.modules.misc.PacketLogger;
import ravex.modules.exploit.HandshakeSpoof;
import io.netty.channel.ChannelHandlerContext;

@Mixin(Connection.class)
public class MixinConnection {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        // Attack/Interact shield for FakePlayer to prevent server-side kicks/desyncs
        if (packet instanceof ServerboundInteractPacket interactPacket) {
            int entityId = ((AccessorServerboundInteractPacket) interactPacket).getEntityId();
            if (entityId == -9999) {
                ci.cancel();
                return;
            }
        }

        // HandshakeSpoof injection
        if (packet instanceof ClientIntentionPacket handshakePacket) {
            if (HandshakeSpoof.INSTANCE.getEnabled()) {
                AccessorClientIntentionPacket accessor = (AccessorClientIntentionPacket) (Object) handshakePacket;
                String originalHost = handshakePacket.hostName();
                int originalProtocol = handshakePacket.protocolVersion();
                
                String spoofedHost = HandshakeSpoof.INSTANCE.getSpoofedHost(originalHost);
                int spoofedProtocol = HandshakeSpoof.INSTANCE.getSpoofedProtocol(originalProtocol);

                accessor.setHostName(spoofedHost);
                accessor.setProtocolVersion(spoofedProtocol);
            }
        }

        // Packet Logger (Outgoing)
        if (PacketLogger.INSTANCE.getEnabled() && PacketLogger.INSTANCE.outgoing.getValue()) {
            PacketLogger.INSTANCE.logPacket("C2S ->", packet);
        }

        // Packet Canceller
        if (PacketCanceller.INSTANCE.getEnabled()) {
            boolean cancel = false;
            if (packet instanceof ServerboundMovePlayerPacket && PacketCanceller.INSTANCE.move.getValue()) cancel = true;
            if (packet instanceof ServerboundPlayerInputPacket && PacketCanceller.INSTANCE.input.getValue()) cancel = true;
            if (packet instanceof ServerboundInteractPacket && PacketCanceller.INSTANCE.interact.getValue()) cancel = true;
            if (packet instanceof ServerboundSwingPacket && PacketCanceller.INSTANCE.swing.getValue()) cancel = true;
            if (packet instanceof ServerboundUseItemPacket && PacketCanceller.INSTANCE.use.getValue()) cancel = true;
            if (packet instanceof ServerboundUseItemOnPacket && PacketCanceller.INSTANCE.use.getValue()) cancel = true;
            
            if (cancel) {
                ci.cancel();
                return;
            }
        }

        if (AntiHunger.INSTANCE.getEnabled()) {
            String currentMode = AntiHunger.INSTANCE.mode.getValue();
            
            // Handle movement packages (setting ground to false)
            if (packet instanceof ServerboundMovePlayerPacket) {
                if ("Full".equals(currentMode) || "OnGround".equals(currentMode)) {
                    ((AccessorServerboundMovePlayerPacket) packet).setOnGround(false);
                }
            }
            
            // Handle player commands (cancelling sprint states to avoid server-side sprint exhaustion ticks)
            if (packet instanceof ServerboundPlayerCommandPacket commandPacket) {
                var action = commandPacket.getAction();
                if (action == ServerboundPlayerCommandPacket.Action.START_SPRINTING || 
                    action == ServerboundPlayerCommandPacket.Action.STOP_SPRINTING) {
                    if ("Full".equals(currentMode) || "Sprint".equals(currentMode)) {
                        ci.cancel();
                    }
                }
            }
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void onChannelRead(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        // Packet Logger (Incoming)
        if (PacketLogger.INSTANCE.getEnabled() && PacketLogger.INSTANCE.incoming.getValue()) {
            PacketLogger.INSTANCE.logPacket("S2C <-", packet);
        }
    }
}
