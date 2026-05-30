package ravex.mixin.render;

import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ravex.utility.render.RaveXStateAccessor;

@Mixin(ItemEntityRenderState.class)
public class MixinItemEntityRenderState implements RaveXStateAccessor {
    @Unique
    private boolean ravexOnGround;
    @Unique
    private double ravexMotionY;
    @Unique
    private boolean ravexIsBlock;

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
}
