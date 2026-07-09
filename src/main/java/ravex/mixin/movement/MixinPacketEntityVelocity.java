package ravex.mixin.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.event.EventBusHolder;
import ravex.event.movement.VelocityEvent;
import ravex.mixin.network.AccessorSetEntityMotionPacket;

@Mixin(ClientPacketListener.class)
public class MixinPacketEntityVelocity {

    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true)
    private void onVelocityEvent(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || packet.getId() != player.getId()) return;

        Vec3 packetVel = ((AccessorSetEntityMotionPacket) packet).getMovement();
        VelocityEvent event = new VelocityEvent(VelocityEvent.Type.KNOCKBACK, packetVel);
        EventBusHolder.get().post(event);

        if (event.isCancelled()) { ci.cancel(); return; }

        Vec3 modified = event.getVelocity();
        if (!modified.equals(packetVel)) {
            player.setDeltaMovement(modified);
            ci.cancel();
        }
    }
}
