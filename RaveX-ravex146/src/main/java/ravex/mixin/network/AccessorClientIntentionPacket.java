package ravex.mixin.network;

import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.handshake.ClientIntent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.Mutable;

@Mixin(ClientIntentionPacket.class)
public interface AccessorClientIntentionPacket {
    @Accessor("hostName")
    @Mutable
    void setHostName(String hostName);

    @Accessor("protocolVersion")
    @Mutable
    void setProtocolVersion(int protocolVersion);
}
