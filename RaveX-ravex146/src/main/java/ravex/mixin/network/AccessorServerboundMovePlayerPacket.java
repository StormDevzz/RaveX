package ravex.mixin.network;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface AccessorServerboundMovePlayerPacket {
    @Mutable
    @Accessor("onGround")
    void setOnGround(boolean onGround);

    @Accessor("xRot")
    float getxRot();

    @Mutable
    @Accessor("xRot")
    void setxRot(float xRot);

    @Accessor("yRot")
    float getyRot();

    @Mutable
    @Accessor("yRot")
    void setyRot(float yRot);

    @Accessor("x")
    double getX();

    @Mutable
    @Accessor("x")
    void setX(double x);

    @Accessor("y")
    double getY();

    @Mutable
    @Accessor("y")
    void setY(double y);

    @Accessor("z")
    double getZ();

    @Mutable
    @Accessor("z")
    void setZ(double z);
}

