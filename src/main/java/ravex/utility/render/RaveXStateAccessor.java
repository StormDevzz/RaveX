package ravex.utility.render;

public interface RaveXStateAccessor {
    boolean isRavexOnGround();
    void setRavexOnGround(boolean onGround);
    double getRavexMotionY();
    void setRavexMotionY(double motionY);
    boolean isRavexBlock();
    void setRavexBlock(boolean isBlock);
}
