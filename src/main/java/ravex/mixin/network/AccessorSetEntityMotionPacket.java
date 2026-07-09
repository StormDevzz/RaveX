package ravex.mixin.network;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSetEntityMotionPacket.class)
public interface AccessorSetEntityMotionPacket {
<<<<<<< HEAD
    @Accessor("movement")
    Vec3 getMovement();

=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    @Mutable
    @Accessor("movement")
    void setMovement(Vec3 movement);
}
