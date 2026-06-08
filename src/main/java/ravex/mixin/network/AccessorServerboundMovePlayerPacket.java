package ravex.mixin.network;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface AccessorServerboundMovePlayerPacket {
    @Accessor("onGround")
    void setOnGround(boolean onGround);

    @Accessor("xRot")
    float getxRot();

    @Accessor("xRot")
    void setxRot(float xRot);

    @Accessor("yRot")
    float getyRot();

    @Accessor("yRot")
    void setyRot(float yRot);
}
