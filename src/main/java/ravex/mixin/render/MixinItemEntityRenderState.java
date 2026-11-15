package ravex.mixin.render;

import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ravex.utility.render.RaveXStateAccessorUtility;

@Mixin(ItemEntityRenderState.class)
public class MixinItemEntityRenderState implements RaveXStateAccessorUtility {
    @Unique
    private boolean ravexOnGround;
    @Unique
    private double ravexMotionY;
    @Unique
    private boolean ravexIsBlock;
    @Unique
    private float ravexGroundBlend;
    @Unique
    private float ravexRestRotation;
    @Unique
    private float ravexRestRotationX;
    @Unique
    private double ravexRestY;
    @Unique
    private double ravexEntityX;
    @Unique
    private double ravexEntityZ;

    @Override
    public boolean isRavexOnGround() {
        return ravexOnGround;
    }

    @Override
    public void setRavexOnGround(boolean onGround) {
        this.ravexOnGround = onGround;
    }

    @Override
    public double getRavexMotionY() {
        return ravexMotionY;
    }

    @Override
    public void setRavexMotionY(double motionY) {
        this.ravexMotionY = motionY;
    }

    @Override
    public boolean isRavexBlock() {
        return ravexIsBlock;
    }

    @Override
    public void setRavexBlock(boolean isBlock) {
        this.ravexIsBlock = isBlock;
    }

    @Override
    public float getRavexGroundBlend() {
        return ravexGroundBlend;
    }

    @Override
    public void setRavexGroundBlend(float groundBlend) {
        this.ravexGroundBlend = groundBlend;
    }

    @Override
    public float getRavexRestRotation() {
        return ravexRestRotation;
    }

    @Override
    public void setRavexRestRotation(float restRotation) {
        this.ravexRestRotation = restRotation;
    }

    @Override
    public float getRavexRestRotationX() {
        return ravexRestRotationX;
    }

    @Override
    public void setRavexRestRotationX(float restRotationX) {
        this.ravexRestRotationX = restRotationX;
    }

    @Override
    public double getRavexRestY() {
        return ravexRestY;
    }

    @Override
    public void setRavexRestY(double restY) {
        this.ravexRestY = restY;
    }

    @Override
    public double getRavexEntityX() {
        return ravexEntityX;
    }

    @Override
    public void setRavexEntityX(double entityX) {
        this.ravexEntityX = entityX;
    }

    @Override
    public double getRavexEntityZ() {
        return ravexEntityZ;
    }

    @Override
    public void setRavexEntityZ(double entityZ) {
        this.ravexEntityZ = entityZ;
    }
}
