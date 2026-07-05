#pragma once

#include "types.hpp"
#include "velocity_math.hpp"
#include "bypass_handler.hpp"
#include "grim_bypass.hpp"

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
