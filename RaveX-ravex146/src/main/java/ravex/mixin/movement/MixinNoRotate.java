package ravex.mixin.movement;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.NoRotate;

@Mixin(ClientPacketListener.class)
public class MixinNoRotate {
    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void onHandleMovePlayerHead(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        if (NoRotate.INSTANCE.getEnabled()) {
            NoRotate.INSTANCE.saveRotation();
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void onHandleMovePlayerTail(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        NoRotate.INSTANCE.restoreRotation();
    }
}
