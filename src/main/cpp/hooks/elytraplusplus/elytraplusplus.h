#pragma once

#include "types.h"
#include "velocity_math.h"
#include "bypass_handler.h"
#include "grim_bypass.h"

void calculateElytraVelocity(
    const std::string& modeStr,
    double hSpeed, double vSpeed, double glide,
    double yaw, double pitch,
    bool jump, bool sneak,
    double* outVelocity
);

void applyElytraBypass(
    const std::string& modeStr,
    double motionX, double motionY, double motionZ,
    double yaw, double pitch,
    bool jump, bool sneak, bool onGround,
    double* outMotion
);
