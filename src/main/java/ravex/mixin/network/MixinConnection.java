package ravex.mixin.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.event.EventBusHolder;
import ravex.event.network.PacketEvent;
import io.netty.channel.ChannelHandlerContext;
import ravex.modules.Modules;

@Mixin(Connection.class)
public class MixinConnection {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ServerboundInteractPacket interactPacket) {
            int entityId = ((AccessorServerboundInteractPacket) interactPacket).getEntityId();
            if (entityId == -9999) {
                ci.cancel();
                return;
            }
        }

        if (packet instanceof net.minecraft.network.protocol.game.ServerboundContainerClickPacket click) {
            // clicks are too frequent to log
        }

        // packetMine strict rotations: angles ride in movement packets
        if (packet instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket movePacket
                && (movePacket instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot
                    || movePacket instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot)) {
            if (Modules.enabled(ravex.modules.player.PacketMine.class)) {
                ravex.modules.player.PacketMine pm = Modules.get(ravex.modules.player.PacketMine.class);
                if (pm != null && "Strict".equals(pm.rotate) && ravex.modules.player.PacketMine.silentRotation.hasRotation) {
                    var accessor = (ravex.mixin.network.AccessorServerboundMovePlayerPacket) (Object) movePacket;
                    accessor.setyRot(ravex.modules.player.PacketMine.silentRotation.yaw);
                    accessor.setxRot(ravex.modules.player.PacketMine.silentRotation.pitch);
                }
            }
        }

        PacketEvent event = new PacketEvent(PacketEvent.PacketAction.SEND, packet);
        EventBusHolder.get().post(event);
        if (event.isCancelled()) { ci.cancel(); return; }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onChannelRead(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        PacketEvent event = new PacketEvent(PacketEvent.PacketAction.RECEIVE, packet);
        EventBusHolder.get().post(event);
        if (event.isCancelled()) { ci.cancel(); return; }
    }
}
