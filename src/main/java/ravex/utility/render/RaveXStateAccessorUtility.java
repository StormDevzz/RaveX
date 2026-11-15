package ravex.utility.render;

public interface RaveXStateAccessorUtility {
    boolean isRavexOnGround();
    void setRavexOnGround(boolean onGround);
    double getRavexMotionY();
    void setRavexMotionY(double motionY);
    boolean isRavexBlock();
    void setRavexBlock(boolean isBlock);
    float getRavexGroundBlend();
    void setRavexGroundBlend(float groundBlend);
    float getRavexRestRotation();
    void setRavexRestRotation(float restRotation);
    float getRavexRestRotationX();
    void setRavexRestRotationX(float restRotationX);
    double getRavexRestY();
    void setRavexRestY(double restY);
    double getRavexEntityX();
    void setRavexEntityX(double entityX);
    double getRavexEntityZ();
    void setRavexEntityZ(double entityZ);
}
