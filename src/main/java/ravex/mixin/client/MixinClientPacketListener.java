package ravex.mixin.client;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class MixinClientPacketListener {
}
