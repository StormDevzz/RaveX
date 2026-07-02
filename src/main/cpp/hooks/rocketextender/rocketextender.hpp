#pragma once

void calculateRocketBoost(
    double yaw,
    double pitch,
    double currentVx,
    double currentVy,
    double currentVz,
    double boostFactor,
    double* outVelocity
);
