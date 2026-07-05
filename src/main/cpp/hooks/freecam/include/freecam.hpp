#ifndef RAVEX_FREECAM_H
#define RAVEX_FREECAM_H

namespace ravex::hooks::freecam {

    struct FreeCamState {
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        float yaw = 0.0f;
        float pitch = 0.0f;

        double prevX = 0.0;
        double prevY = 0.0;
        double prevZ = 0.0;
        float prevYaw = 0.0f;
        float prevPitch = 0.0f;

        double targetX = 0.0;
        double targetY = 0.0;
        double targetZ = 0.0;
    };

    
    extern FreeCamState g_state;

    void reset(double startX, double startY, double startZ, float startYaw, float startPitch);
    void turn(double yRot, double xRot);
    void updatePosition(bool keyUp, bool keyDown, bool keyLeft, bool keyRight, bool keyJump, bool keyShift, double speed, double smoothness);

} 

#endif 
