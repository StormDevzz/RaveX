package ravex.mixin.network;

import io.netty.channel.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.network.Connection.class)
public interface AccessorConnection {
    @Accessor("channel")
    Channel getChannel();
}
