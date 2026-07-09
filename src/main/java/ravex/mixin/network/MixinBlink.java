package ravex.mixin.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.Blink;

@Mixin(Connection.class)
public class MixinBlink {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onBlinkSend(Packet<?> packet, CallbackInfo ci) {
        if (Blink.itz().shouldCancel(packet)) {
            ci.cancel();
        }
    }
}
