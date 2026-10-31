package ravex.mixin.client;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class MixinClientPacketListener implements ravex.utility.interfaces.IClientPacketListener {
    @Shadow private String serverBrand;

    @Override
    public String ravex$getServerBrand() {
        return this.serverBrand;
    }

    // pingSpoof: delay the keep alive reply the server times
    @Inject(method = "handleKeepAlive", at = @At("HEAD"), cancellable = true)
    private void ravexKeepAlive(ClientboundKeepAlivePacket packet, CallbackInfo ci) {
        if (ravex.modules.misc.PingSpoof.delayKeepAlive(packet.getId())) ci.cancel();
    }
}
