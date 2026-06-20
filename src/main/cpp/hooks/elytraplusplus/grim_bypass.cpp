#include "grim_bypass.h"
#include <algorithm>

GrimBypassData getGrimBypassData() {
    GrimBypassData data{};
    data.friction = 0.98;
    data.verticalDrag = 0.99;
    data.maxVerticalSpeed = -0.1;
    data.limitVertical = true;
    data.useAirFrictionPatch = true;
    data.horizontalFriction = 0.99;
    return data;
}

VelocityOutput applyGrimAdvancedBypass(
    const MotionState& state,
    const GrimBypassData& data,
    int elapsedTicks
) {
    VelocityOutput out;

    double hDrag = data.useAirFrictionPatch ? data.horizontalFriction : 1.0;
    double tickFriction = std::pow(data.friction, std::min(elapsedTicks, 3));

    out.x = state.x * data.friction * hDrag;
    out.z = state.z * data.friction * hDrag;

    if (data.limitVertical && state.y < data.maxVerticalSpeed) {
        out.y = data.maxVerticalSpeed * data.verticalDrag;
    } else {
        out.y = state.y * data.verticalDrag;
    }

    if (state.onGround) {
        out.y = std::max(out.y, 0.0);
    }

    return out;
}

bool shouldSkipGrimVerticalCheck(double motionY, double minY) {
    return motionY >= minY;
}

double applyGrimHorizontalFriction(double current, double friction, double drag) {
    return current * friction * drag;
}
