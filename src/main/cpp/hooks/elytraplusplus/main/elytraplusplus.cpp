#include "elytraplusplus.hpp"

void calculateElytraVelocity(
    const std::string& modeStr,
    double hSpeed, double vSpeed, double glide,
    double yaw, double pitch,
    bool jump, bool sneak,
    double* outVelocity
) {
    ElytraMode mode = modeFromString(modeStr);
    VelocityInput in;
    in.hSpeed = hSpeed;
    in.vSpeed = vSpeed;
    in.glide = glide;
    in.yaw = yaw;
    in.pitch = pitch;
    in.jump = jump;
    in.sneak = sneak;

    VelocityOutput out = dispatchVelocityCalculation(mode, in);
    outVelocity[0] = out.x;
    outVelocity[1] = out.y;
    outVelocity[2] = out.z;
}

void applyElytraBypass(
    const std::string& modeStr,
    double motionX, double motionY, double motionZ,
    double yaw, double pitch,
    bool jump, bool sneak, bool onGround,
    double* outMotion
) {
    ElytraMode mode = modeFromString(modeStr);
    MotionState state;
    state.x = motionX;
    state.y = motionY;
    state.z = motionZ;
    state.yaw = yaw;
    state.pitch = pitch;
    state.jump = jump;
    state.sneak = sneak;
    state.onGround = onGround;

    VelocityOutput out = dispatchBypass(mode, state);
    outMotion[0] = out.x;
    outMotion[1] = out.y;
    outMotion[2] = out.z;
}
