#pragma once

#include "types.hpp"

struct GrimBypassData {
    double friction;
    double verticalDrag;
    double maxVerticalSpeed;
    bool limitVertical;
    bool useAirFrictionPatch;
    double horizontalFriction;
};

GrimBypassData getGrimBypassData();

VelocityOutput applyGrimAdvancedBypass(
    const MotionState& state,
    const GrimBypassData& data,
    int elapsedTicks
);

bool shouldSkipGrimVerticalCheck(double motionY, double minY);
double applyGrimHorizontalFriction(double current, double friction, double drag);
