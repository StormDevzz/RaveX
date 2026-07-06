#pragma once

#include "types.hpp"

BypassConfig getBypassConfig(ElytraMode mode);

VelocityOutput applyControlBypass(const MotionState& state, const BypassConfig& config);
VelocityOutput applyVanillaBypass(const MotionState& state, const BypassConfig& config);
VelocityOutput applyGrimBypass(const MotionState& state, const BypassConfig& config);
VelocityOutput applyNCPBypass(const MotionState& state, const BypassConfig& config);
VelocityOutput applyMinemenBypass(const MotionState& state, const BypassConfig& config);
VelocityOutput applyPacketBypass(const MotionState& state, const BypassConfig& config);
VelocityOutput applyBoostBypass(const MotionState& state, const BypassConfig& config);
VelocityOutput applyTickShiftBypass(const MotionState& state, const BypassConfig& config);

VelocityOutput dispatchBypass(ElytraMode mode, const MotionState& state);
