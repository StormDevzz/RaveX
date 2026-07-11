package ravex.mixin.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.event.EventBusHolder;
import ravex.event.network.PacketEvent;
import ravex.modules.movement.GuiMove;
import io.netty.channel.ChannelHandlerContext;

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

        if (packet instanceof ServerboundContainerClickPacket) {
            GuiMove gw = GuiMove.itz();
            if (gw.getEnabled() && "Grim".equals(gw.mode.getValue())) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen instanceof AbstractContainerScreen) {
                    ci.cancel();
                    return;
                }
            }
        }

        PacketEvent event = new PacketEvent(PacketEvent.PacketAction.SEND, packet);
        EventBusHolder.get().post(event);
        if (event.isCancelled()) { ci.cancel(); return; }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void onChannelRead(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        PacketEvent event = new PacketEvent(PacketEvent.PacketAction.RECEIVE, packet);
        EventBusHolder.get().post(event);
        if (event.isCancelled()) { ci.cancel(); return; }
    }
}
