package ravex.mixin.client;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class MixinClientPacketListener implements ravex.utility.interfaces.IClientPacketListener {
    @Shadow private String serverBrand;

    @Override
    public String ravex$getServerBrand() {
        return this.serverBrand;
    }
}
