#include "bypass_handler.hpp"

BypassConfig getBypassConfig(ElytraMode mode) {
    BypassConfig config{};
    switch (mode) {
        case ElytraMode::Control:
            config.friction = 0.5;
            config.maxYDrop = -0.05;
            config.minYLimit = -0.2;
            config.clampVertical = true;
            config.enableHorizontalFriction = true;
            break;
        case ElytraMode::Vanilla:
            config.friction = 0.99;
            config.maxYDrop = -100.0;
            config.minYLimit = -100.0;
            config.clampVertical = false;
            config.enableHorizontalFriction = true;
            break;
        case ElytraMode::Grim:
            config.friction = 0.98;
            config.maxYDrop = -0.1;
            config.minYLimit = -0.5;
            config.clampVertical = true;
            config.enableHorizontalFriction = true;
            break;
        case ElytraMode::NCP:
            config.friction = 0.95;
            config.maxYDrop = -0.5;
            config.minYLimit = -1.0;
            config.clampVertical = true;
            config.enableHorizontalFriction = true;
            break;
        case ElytraMode::Minemen:
            config.friction = 0.97;
            config.maxYDrop = 0.0;
            config.minYLimit = -0.3;
            config.clampVertical = true;
            config.enableHorizontalFriction = true;
            break;
        case ElytraMode::Packet:
            config.friction = 1.0;
            config.maxYDrop = -100.0;
            config.minYLimit = -100.0;
            config.clampVertical = false;
            config.enableHorizontalFriction = false;
            break;
        case ElytraMode::Boost:
            config.friction = 0.95;
            config.maxYDrop = -0.2;
            config.minYLimit = -0.5;
            config.clampVertical = true;
            config.enableHorizontalFriction = true;
            break;
        case ElytraMode::TickShift:
            config.friction = 0.92;
            config.maxYDrop = -0.3;
            config.minYLimit = -0.8;
            config.clampVertical = true;
            config.enableHorizontalFriction = true;
            break;
    }
    return config;
}

VelocityOutput applyControlBypass(const MotionState& state, const BypassConfig& config) {
    VelocityOutput out;
    out.x = state.x * 0.5;
    out.y = state.jump ? state.y : (state.sneak ? state.y : state.y * 0.8);
    out.z = state.z * 0.5;
    if (config.clampVertical && out.y < config.maxYDrop) {
        out.y = config.maxYDrop;
    }
    return out;
}

VelocityOutput applyVanillaBypass(const MotionState& state, const BypassConfig& config) {
    VelocityOutput out;
    out.x = state.x * config.friction;
    out.y = state.y * config.friction;
    out.z = state.z * config.friction;
    return out;
}

VelocityOutput applyGrimBypass(const MotionState& state, const BypassConfig& config) {
    VelocityOutput out;
    out.x = state.x * config.friction;
    out.y = state.y * config.friction;
    out.z = state.z * config.friction;

    if (config.clampVertical && out.y < config.maxYDrop) {
        out.y = config.maxYDrop;
    }
    if (out.y < config.minYLimit) {
        out.y = config.minYLimit;
    }
    return out;
}

VelocityOutput applyNCPBypass(const MotionState& state, const BypassConfig& config) {
    VelocityOutput out;
    double hFriction = config.enableHorizontalFriction ? config.friction : 1.0;
    out.x = state.x * hFriction;
    out.y = state.y * config.friction;
    out.z = state.z * hFriction;

    if (config.clampVertical && out.y < config.maxYDrop) {
        out.y = config.maxYDrop;
    }
    if (out.y < config.minYLimit) {
        out.y = config.minYLimit;
    }
    return out;
}

VelocityOutput applyMinemenBypass(const MotionState& state, const BypassConfig& config) {
    VelocityOutput out;
    out.x = state.x * config.friction;
    out.y = state.y * config.friction;
    out.z = state.z * config.friction;

    if (out.y > config.maxYDrop) {
        out.y = config.maxYDrop;
    }
    if (out.y < config.minYLimit) {
        out.y = config.minYLimit;
    }
    return out;
}

VelocityOutput applyPacketBypass(const MotionState& state, const BypassConfig& config) {
    VelocityOutput out;
    out.x = state.x;
    out.y = state.y;
    out.z = state.z;
    return out;
}

VelocityOutput applyBoostBypass(const MotionState& state, const BypassConfig& config) {
    VelocityOutput out;
    out.x = state.x * config.friction;
    out.y = state.y;
    out.z = state.z * config.friction;

    if (out.y < config.maxYDrop) {
        out.y = config.maxYDrop;
    }
    return out;
}

VelocityOutput applyTickShiftBypass(const MotionState& state, const BypassConfig& config) {
    VelocityOutput out;
    out.x = state.x * config.friction;
    out.y = state.y * 0.96;
    out.z = state.z * config.friction;

    if (out.y < config.minYLimit) {
        out.y = config.minYLimit;
    }
    return out;
}

VelocityOutput dispatchBypass(ElytraMode mode, const MotionState& state) {
    BypassConfig config = getBypassConfig(mode);
    switch (mode) {
        case ElytraMode::Control:   return applyControlBypass(state, config);
        case ElytraMode::Vanilla:   return applyVanillaBypass(state, config);
        case ElytraMode::Grim:      return applyGrimBypass(state, config);
        case ElytraMode::NCP:       return applyNCPBypass(state, config);
        case ElytraMode::Minemen:   return applyMinemenBypass(state, config);
        case ElytraMode::Packet:    return applyPacketBypass(state, config);
        case ElytraMode::Boost:     return applyBoostBypass(state, config);
        case ElytraMode::TickShift: return applyTickShiftBypass(state, config);
    }
    return applyVanillaBypass(state, config);
}
