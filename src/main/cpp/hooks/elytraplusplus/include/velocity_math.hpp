#pragma once

#include "types.hpp"

struct LookVector {
    double x, y, z;
};

LookVector calculateLookVector(double yaw, double pitch);

VelocityOutput calculateControlVelocity(const VelocityInput& in);
VelocityOutput calculateVanillaVelocity(const VelocityInput& in);
VelocityOutput calculateGrimVelocity(const VelocityInput& in);
VelocityOutput calculateNCPVelocity(const VelocityInput& in);
VelocityOutput calculateMinemenVelocity(const VelocityInput& in);
VelocityOutput calculatePacketVelocity(const VelocityInput& in);
VelocityOutput calculateBoostVelocity(const VelocityInput& in);
VelocityOutput calculateTickShiftVelocity(const VelocityInput& in);

VelocityOutput dispatchVelocityCalculation(ElytraMode mode, const VelocityInput& in);
