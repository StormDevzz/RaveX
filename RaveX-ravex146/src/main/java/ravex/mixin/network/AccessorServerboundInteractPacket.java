package ravex.mixin.network;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundInteractPacket.class)
public interface AccessorServerboundInteractPacket {
    @Accessor("entityId")
    int getEntityId();
}
